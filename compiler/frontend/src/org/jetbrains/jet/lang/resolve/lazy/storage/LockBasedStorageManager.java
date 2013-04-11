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

import com.intellij.openapi.util.Computable;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.containers.ConcurrentWeakValueHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;
import org.jetbrains.jet.utils.Nulls;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LockBasedStorageManager implements StorageManager {

    private final Object lock = new Object() {
        @Override
        public String toString() {
            return "LockBasedStorageManager centralized lock";
        }
    };

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(
            @NotNull Function<K, V> compute, @NotNull ReferenceKind valuesReferenceKind
    ) {
        ConcurrentMap<K, Object> map = createConcurrentMap(valuesReferenceKind);
        return new MapBasedMemoizedFunctionToNotNull<K, V>(lock, map, compute);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull Function<K, V> compute, @NotNull ReferenceKind valuesReferenceKind
    ) {
        ConcurrentMap<K, Object> map = createConcurrentMap(valuesReferenceKind);
        return new MapBasedMemoizedFunction<K, V>(lock, map, compute);
    }

    private static <K, V> ConcurrentMap<K, V> createConcurrentMap(ReferenceKind referenceKind) {
        return (referenceKind == ReferenceKind.WEAK) ? new ConcurrentWeakValueHashMap<K, V>() : new ConcurrentHashMap<K, V>();
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValue(@NotNull Computable<T> computable) {
        return new LockBasedNotNullLazyValue<T>(lock, computable);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValueWithPostCompute(@NotNull Computable<T> computable, @NotNull final Consumer<T> postCompute) {
        return new LockBasedNotNullLazyValue<T>(lock, computable) {
            @Override
            protected void postCompute(@NotNull T value) {
                postCompute.consume(value);
            }
        };
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValue(@NotNull Computable<T> computable) {
        return new LockBasedLazyValue<T>(lock, computable);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(
            @NotNull Computable<T> computable, @NotNull final Consumer<T> postCompute
    ) {
        return new LockBasedLazyValue<T>(lock, computable) {
            @Override
            protected void postCompute(@Nullable T value) {
                postCompute.consume(value);
            }
        };
    }

    @NotNull
    @Override
    public BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace) {
        // It seems safe to have a separate lock for traces:
        // no other locks will be acquired inside the trace operations
        return new LockProtectedTrace(lock, originalTrace);
    }

    private static class LockBasedLazyValue<T> implements NullableLazyValue<T> {
        private final Object lock;
        private final Computable<T> computable;

        @Nullable
        private volatile Object value = null;

        public LockBasedLazyValue(@NotNull Object lock, @NotNull Computable<T> computable) {
            this.lock = lock;
            this.computable = computable;
        }

        @Override
        public T compute() {
            Object _value = value;
            if (_value != null) return Nulls.unescape(_value);

            synchronized (lock) {
                _value = value;
                if (_value != null) return Nulls.unescape(_value);

                T typedValue = computable.compute();
                value = Nulls.escape(typedValue);

                postCompute(typedValue);

                return typedValue;
            }
        }

        protected void postCompute(T value) {
            // Doing something in post-compute helps prevent infinite recursion
        }
    }

    private static class LockBasedNotNullLazyValue<T> extends LockBasedLazyValue<T> implements NotNullLazyValue<T> {

        public LockBasedNotNullLazyValue(@NotNull Object lock, @NotNull Computable<T> computable) {
            super(lock, computable);
        }

        @Override
        @NotNull
        public T compute() {
            T result = super.compute();
            assert result != null : "compute() returned null";
            return result;
        }
    }

    private static class MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNullable<K, V> {
        private final Object lock;
        private final ConcurrentMap<K, Object> cache;
        private final Function<K, V> compute;

        public MapBasedMemoizedFunction(@NotNull Object lock, @NotNull ConcurrentMap<K, Object> map, @NotNull Function<K, V> compute) {
            this.lock = lock;
            this.cache = map;
            this.compute = compute;
        }

        @Override
        @Nullable
        public V fun(@NotNull K input) {
            Object value = cache.get(input);
            if (value != null) return Nulls.unescape(value);

            synchronized (lock) {
                value = cache.get(input);
                if (value != null) return Nulls.unescape(value);

                V typedValue = compute.fun(input);

                Object oldValue = cache.put(input, Nulls.escape(typedValue));
                assert oldValue == null : "Race condition detected";

                return typedValue;
            }
        }
    }

    private static class MapBasedMemoizedFunctionToNotNull<K, V> extends MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNotNull<K, V> {

        public MapBasedMemoizedFunctionToNotNull(
                @NotNull Object lock,
                @NotNull ConcurrentMap<K, Object> map,
                @NotNull Function<K, V> compute
        ) {
            super(lock, map, compute);
        }

        @NotNull
        @Override
        public V fun(@NotNull K input) {
            V result = super.fun(input);
            assert result != null : "compute() returned null";
            return result;
        }
    }

    private static class LockProtectedTrace implements BindingTrace {
        private final Object lock;
        private final BindingTrace trace;

        public LockProtectedTrace(@NotNull Object lock, @NotNull BindingTrace trace) {
            this.lock = lock;
            this.trace = trace;
        }

        @Override
        public BindingContext getBindingContext() {
            synchronized (lock) {
                return trace.getBindingContext();
            }
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
            synchronized (lock) {
                trace.record(slice, key, value);
            }
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
            synchronized (lock) {
                trace.record(slice, key);
            }
        }

        @Override
        @Nullable
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            synchronized (lock) {
                return trace.get(slice, key);
            }
        }

        @Override
        @NotNull
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            synchronized (lock) {
                return trace.getKeys(slice);
            }
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            synchronized (lock) {
                trace.report(diagnostic);
            }
        }
    }

}
