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

package org.jetbrains.jet.lang.resolve.lazy.storage;

import com.google.common.collect.ImmutableMap;
import com.intellij.util.containers.ConcurrentWeakValueHashMap;
import jet.Function0;
import jet.Function1;
import jet.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.storage.*;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

public class LockBasedLazyResolveStorageManager implements LazyResolveStorageManager {

    private final LockBasedStorageManager storageManager;

    public LockBasedLazyResolveStorageManager(@NotNull LockBasedStorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    @NotNull
    public <K, V> MemoizedFunctionToNotNull<K, V> createWeaklyRetainedMemoizedFunction(
            @NotNull Function1<K, V> compute
    ) {
        return storageManager.new Internals().createMemoizedFunction(compute, new ConcurrentWeakValueHashMap<K, Object>());
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createWeaklyRetainedMemoizedFunctionWithNullableValues(
            @NotNull Function1<K, V> compute
    ) {
        return storageManager.new Internals().createMemoizedFunctionWithNullableValues(compute, new ConcurrentWeakValueHashMap<K, Object>());
    }

    @NotNull
    @Override
    public BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace) {
        // It seems safe to have a separate lock for traces:
        // no other locks will be acquired inside the trace operations
        return new LockProtectedTrace(storageManager.new Internals().getLock(), originalTrace);
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
        private final Lock lock;
        private final BindingContext context;

        private LockProtectedContext(Lock lock, BindingContext context) {
            this.lock = lock;
            this.context = context;
        }

        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            lock.lock();
            try {
                return context.getDiagnostics();
            }
            finally {
                lock.unlock();
            }
        }

        @Nullable
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            lock.lock();
            try {
                return context.get(slice, key);
            }
            finally {
                lock.unlock();
            }
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            lock.lock();
            try {
                return context.getKeys(slice);
            }
            finally {
                lock.unlock();
            }
        }

        @NotNull
        @Override
        @TestOnly
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            lock.lock();
            try {
                return context.getSliceContents(slice);
            }
            finally {
                lock.unlock();
            }
        }
    }

    private static class LockProtectedTrace implements BindingTrace {
        private final Lock lock;
        private final BindingTrace trace;
        private final BindingContext context;

        public LockProtectedTrace(@NotNull Lock lock, @NotNull BindingTrace trace) {
            this.lock = lock;
            this.trace = trace;
            this.context = new LockProtectedContext(lock, trace.getBindingContext());
        }

        @NotNull
        @Override
        public BindingContext getBindingContext() {
            return context;
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
            lock.lock();
            try {
                trace.record(slice, key, value);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
            lock.lock();
            try {
                trace.record(slice, key);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        @Nullable
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            lock.lock();
            try {
                return trace.get(slice, key);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        @NotNull
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            lock.lock();
            try {
                return trace.getKeys(slice);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            lock.lock();
            try {
                trace.report(diagnostic);
            }
            finally {
                lock.unlock();
            }
        }
    }
}
