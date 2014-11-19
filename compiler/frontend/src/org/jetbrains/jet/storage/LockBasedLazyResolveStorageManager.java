/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.storage;

import com.google.common.collect.ImmutableMap;
import com.intellij.util.containers.ConcurrentWeakValueHashMap;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

// This class is kept under the same package as LockBasedStorageManager to get access to its protected members
// Otherwise wed have to expose the lock which is worse than have such a hackish class placement
public class LockBasedLazyResolveStorageManager implements LazyResolveStorageManager {

    private final StorageManager storageManager;

    public LockBasedLazyResolveStorageManager(@NotNull StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    @NotNull
    public <K, V> MemoizedFunctionToNotNull<K, V> createWeaklyRetainedMemoizedFunction(
            @NotNull Function1<K, V> compute
    ) {
        return storageManager.createMemoizedFunction(compute, new ConcurrentWeakValueHashMap<K, Object>());
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createWeaklyRetainedMemoizedFunctionWithNullableValues(
            @NotNull Function1<K, V> compute
    ) {
        return storageManager.createMemoizedFunctionWithNullableValues(compute, new ConcurrentWeakValueHashMap<K, Object>());
    }

    @NotNull
    @Override
    public BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace) {
        // It seems safe to have a separate lock for traces:
        // no other locks will be acquired inside the trace operations

        return new LockProtectedTrace(storageManager, originalTrace);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull Function1<? super K, ? extends V> compute, @NotNull ConcurrentMap<K, Object> map
    ) {
        return storageManager.createMemoizedFunctionWithNullableValues(compute, map);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(
            @NotNull Function1<? super K, ? extends V> compute, @NotNull ConcurrentMap<K, Object> map
    ) {
        return storageManager.createMemoizedFunction(compute, map);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(@NotNull Function1<? super K, ? extends V> compute) {
        return storageManager.createMemoizedFunction(compute);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(@NotNull Function1<? super K, ? extends V> compute) {
        return storageManager.createMemoizedFunctionWithNullableValues(compute);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValue(@NotNull Function0<? extends T> computable) {
        return storageManager.createLazyValue(computable);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createRecursionTolerantLazyValue(
            @NotNull Function0<? extends T> computable,
            @NotNull T onRecursiveCall
    ) {
        return storageManager.createRecursionTolerantLazyValue(computable, onRecursiveCall);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValueWithPostCompute(
            @NotNull Function0<? extends T> computable,
            @Nullable Function1<? super Boolean, ? extends T> onRecursiveCall,
            @NotNull Function1<? super T, ? extends Unit> postCompute
    ) {
        return storageManager.createLazyValueWithPostCompute(computable, onRecursiveCall, postCompute);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValue(@NotNull Function0<? extends T> computable) {
        return storageManager.createNullableLazyValue(computable);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createRecursionTolerantNullableLazyValue(
            @NotNull Function0<? extends T> computable,
            T onRecursiveCall
    ) {
        return storageManager.createRecursionTolerantNullableLazyValue(computable, onRecursiveCall);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(
            @NotNull Function0<? extends T> computable,
            @NotNull Function1<? super T, ? extends Unit> postCompute
    ) {
        return storageManager.createNullableLazyValueWithPostCompute(computable, postCompute);
    }

    @Override
    public <T> T compute(@NotNull Function0<? extends T> computable) {
        return storageManager.compute(computable);
    }

    private static class LockProtectedContext implements BindingContext {
        private final StorageManager storageManager;
        private final BindingContext context;

        private LockProtectedContext(StorageManager storageManager, BindingContext context) {
            this.storageManager = storageManager;
            this.context = context;
        }

        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            return storageManager.compute(new Function0<Diagnostics>() {
                @Override
                public Diagnostics invoke() {
                    return context.getDiagnostics();
                }
            });
        }

        @Nullable
        @Override
        public <K, V> V get(final ReadOnlySlice<K, V> slice, final K key) {
            return storageManager.compute(new Function0<V>() {
                @Override
                public V invoke() {
                    return context.get(slice, key);
                }
            });
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(final WritableSlice<K, V> slice) {
            return storageManager.compute(new Function0<Collection<K>>() {
                @Override
                public Collection<K> invoke() {
                    return context.getKeys(slice);
                }
            });
        }

        @NotNull
        @Override
        @TestOnly
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull final ReadOnlySlice<K, V> slice) {
            return storageManager.compute(new Function0<ImmutableMap<K, V>>() {
                @Override
                public ImmutableMap<K, V> invoke() {
                    return context.getSliceContents(slice);
                }
            });
        }
    }

    private static class LockProtectedTrace implements BindingTrace {
        private final BindingTrace trace;
        private final BindingContext context;
        private final StorageManager storageManager;

        public LockProtectedTrace(@NotNull StorageManager storageManager, @NotNull BindingTrace trace) {
            this.storageManager = storageManager;
            this.trace = trace;
            this.context = new LockProtectedContext(storageManager, trace.getBindingContext());
        }

        @NotNull
        @Override
        public BindingContext getBindingContext() {
            return context;
        }

        @Override
        public <K, V> void record(final WritableSlice<K, V> slice, final K key, final V value) {
            storageManager.compute(new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    trace.record(slice, key, value);
                    return Unit.INSTANCE$;
                }
            });
        }

        @Override
        public <K> void record(final WritableSlice<K, Boolean> slice, final K key) {
            storageManager.compute(new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    trace.record(slice, key);
                    return Unit.INSTANCE$;
                }
            });
        }

        @Override
        @Nullable
        public <K, V> V get(final ReadOnlySlice<K, V> slice, final K key) {
            return storageManager.compute(new Function0<V>() {
                @Override
                public V invoke() {
                    return trace.get(slice, key);
                }
            });
        }

        @Override
        @NotNull
        public <K, V> Collection<K> getKeys(final WritableSlice<K, V> slice) {
            return storageManager.compute(new Function0<Collection<K>>() {
                @Override
                public Collection<K> invoke() {
                    return trace.getKeys(slice);
                }
            });
        }

        @Override
        public void report(@NotNull final Diagnostic diagnostic) {
            storageManager.compute(new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    trace.report(diagnostic);
                    return Unit.INSTANCE$;
                }
            });
        }
    }
}