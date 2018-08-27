package org.dataloader;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests related to context.  DataLoaderTest is getting to big and needs refactoring
 */
public class DataLoaderBatchLoaderEnvironmentTest {

    private BatchLoaderWithContext<String, String> contextBatchLoader() {
        return (keys, environment) -> {
            List<String> list = keys.stream().map(k -> {
                Object context = environment.getContext();
                Object keyContext = environment.getKeyContexts().get(k);
                return k + "-" + context + "-" + keyContext;
            }).collect(Collectors.toList());
            return CompletableFuture.completedFuture(list);
        };
    }


    @Test
    public void context_is_passed_to_batch_loader_function() throws Exception {
        BatchLoaderWithContext<String, String> batchLoader = (keys, environment) -> {
            List<String> list = keys.stream().map(k -> k + "-" + environment.getContext()).collect(Collectors.toList());
            return CompletableFuture.completedFuture(list);
        };
        DataLoaderOptions options = DataLoaderOptions.newOptions()
                .setBatchLoaderEnvironmentProvider(() -> "ctx");
        DataLoader<String, String> loader = DataLoader.newDataLoader(batchLoader, options);

        loader.load("A");
        loader.load("B");
        loader.loadMany(asList("C", "D"));

        List<String> results = loader.dispatchAndJoin();

        assertThat(results, equalTo(asList("A-ctx", "B-ctx", "C-ctx", "D-ctx")));
    }

    @Test
    public void key_contexts_are_passed_to_batch_loader_function() throws Exception {
        BatchLoaderWithContext<String, String> batchLoader = contextBatchLoader();
        DataLoaderOptions options = DataLoaderOptions.newOptions()
                .setBatchLoaderEnvironmentProvider(() -> "ctx");
        DataLoader<String, String> loader = DataLoader.newDataLoader(batchLoader, options);

        loader.load("A", "aCtx");
        loader.load("B", "bCtx");
        loader.loadMany(asList("C", "D"), asList("cCtx", "dCtx"));

        List<String> results = loader.dispatchAndJoin();

        assertThat(results, equalTo(asList("A-ctx-aCtx", "B-ctx-bCtx", "C-ctx-cCtx", "D-ctx-dCtx")));
    }

    @Test
    public void key_contexts_are_passed_to_batch_loader_function_when_batching_disabled() throws Exception {
        BatchLoaderWithContext<String, String> batchLoader = contextBatchLoader();
        DataLoaderOptions options = DataLoaderOptions.newOptions()
                .setBatchingEnabled(false)
                .setBatchLoaderEnvironmentProvider(() -> "ctx");
        DataLoader<String, String> loader = DataLoader.newDataLoader(batchLoader, options);

        CompletableFuture<String> aLoad = loader.load("A", "aCtx");
        CompletableFuture<String> bLoad = loader.load("B", "bCtx");
        CompletableFuture<List<String>> canDLoad = loader.loadMany(asList("C", "D"), asList("cCtx", "dCtx"));

        List<String> results = new ArrayList<>(asList(aLoad.join(), bLoad.join()));
        results.addAll(canDLoad.join());

        assertThat(results, equalTo(asList("A-ctx-aCtx", "B-ctx-bCtx", "C-ctx-cCtx", "D-ctx-dCtx")));
    }

    @Test
    public void missing_key_contexts_are_passed_to_batch_loader_function() throws Exception {
        BatchLoaderWithContext<String, String> batchLoader = contextBatchLoader();
        DataLoaderOptions options = DataLoaderOptions.newOptions()
                .setBatchLoaderEnvironmentProvider(() -> "ctx");
        DataLoader<String, String> loader = DataLoader.newDataLoader(batchLoader, options);

        loader.load("A", "aCtx");
        loader.load("B");
        loader.loadMany(asList("C", "D"), singletonList("cCtx"));

        List<String> results = loader.dispatchAndJoin();

        assertThat(results, equalTo(asList("A-ctx-aCtx", "B-ctx-null", "C-ctx-cCtx", "D-ctx-null")));
    }

    @Test
    public void context_is_passed_to_map_batch_loader_function() throws Exception {
        MappedBatchLoaderWithContext<String, String> mapBatchLoader = (keys, environment) -> {
            Map<String, String> map = new HashMap<>();
            keys.forEach(k -> {
                Object context = environment.getContext();
                Object keyContext = environment.getKeyContexts().get(k);
                map.put(k, k + "-" + context + "-" + keyContext);
            });
            return CompletableFuture.completedFuture(map);
        };
        DataLoaderOptions options = DataLoaderOptions.newOptions()
                .setBatchLoaderEnvironmentProvider(() -> "ctx");
        DataLoader<String, String> loader = DataLoader.newMappedDataLoader(mapBatchLoader, options);

        loader.load("A", "aCtx");
        loader.load("B");
        loader.loadMany(asList("C", "D"), singletonList("cCtx"));

        List<String> results = loader.dispatchAndJoin();

        assertThat(results, equalTo(asList("A-ctx-aCtx", "B-ctx-null", "C-ctx-cCtx", "D-ctx-null")));
    }

    @Test
    public void null_is_passed_as_context_if_you_do_nothing() throws Exception {
        BatchLoaderWithContext<String, String> batchLoader = (keys, environment) -> {
            List<String> list = keys.stream().map(k -> k + "-" + environment.getContext()).collect(Collectors.toList());
            return CompletableFuture.completedFuture(list);
        };
        DataLoader<String, String> loader = DataLoader.newDataLoader(batchLoader);

        loader.load("A");
        loader.load("B");
        loader.loadMany(asList("C", "D"));

        List<String> results = loader.dispatchAndJoin();

        assertThat(results, equalTo(asList("A-null", "B-null", "C-null", "D-null")));
    }

    @Test
    public void null_is_passed_as_context_to_map_loader_if_you_do_nothing() throws Exception {
        MappedBatchLoaderWithContext<String, String> mapBatchLoader = (keys, environment) -> {
            Map<String, String> map = new HashMap<>();
            keys.forEach(k -> map.put(k, k + "-" + environment.getContext()));
            return CompletableFuture.completedFuture(map);
        };
        DataLoader<String, String> loader = DataLoader.newMappedDataLoader(mapBatchLoader);

        loader.load("A");
        loader.load("B");
        loader.loadMany(asList("C", "D"));

        List<String> results = loader.dispatchAndJoin();

        assertThat(results, equalTo(asList("A-null", "B-null", "C-null", "D-null")));
    }
}
