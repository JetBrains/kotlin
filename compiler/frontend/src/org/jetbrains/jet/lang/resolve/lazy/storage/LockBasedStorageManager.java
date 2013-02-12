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
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(@NotNull final Function<K, V> compute, @NotNull final ReferenceKind valuesReferenceKind) {
        return new MemoizedFunctionToNotNull<K, V>() {
            private final ConcurrentMap<K, V> cache = createConcurrentMap(valuesReferenceKind);

            @NotNull
            @Override
            public V fun(@NotNull final K input) {
                V value = cache.get(input);
                if (value != null) return value;

                synchronized (lock) {
                    value = cache.get(input);
                    if (value != null) return value;

                    value = compute.fun(input);

                    V oldValue = cache.put(input, value);
                    assert oldValue == null : "Race condition detected";
                }

                return value;
            }
        };
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull final Function<K, V> compute, @NotNull final ReferenceKind valuesReferenceKind
    ) {
        return new MemoizedFunctionToNullable<K, V>() {
            private final ConcurrentMap<K, NullableLazyValue<V>> cache = createConcurrentMap(valuesReferenceKind);

            @Override
            @Nullable
            public V fun(@NotNull final K input) {
                NullableLazyValue<V> lazyValue = cache.get(input);
                if (lazyValue != null) return lazyValue.compute();

                lazyValue = createNullableLazyValue(new Computable<V>() {
                    @Override
                    public V compute() {
                        return compute.fun(input);
                    }
                });

                NullableLazyValue<V> oldValue = cache.putIfAbsent(input, lazyValue);
                if (oldValue != null) return oldValue.compute();

                return lazyValue.compute();
            }
        };
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
        return new LockBasedNullableLazyValue<T>(lock, computable);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(
            @NotNull Computable<T> computable,
            @NotNull final Consumer<T> postCompute
    ) {
        return new LockBasedNullableLazyValue<T>(lock, computable) {
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

    private static class LockBasedNotNullLazyValue<T> implements NotNullLazyValue<T> {
        private final Object lock;
        private final Computable<T> computable;

        @Nullable
        private volatile T value;

        public LockBasedNotNullLazyValue(@NotNull Object lock, @NotNull Computable<T> computable) {
            this.lock = lock;
            this.computable = computable;
        }

        @NotNull
        @Override
        public T compute() {
            T _value = value;
            if (_value != null) {
                return _value;
            }

            synchronized (lock) {
                _value = value;
                if (_value == null) {
                    _value = computable.compute();
                    value = _value;
                    postCompute(_value);
                }
                return _value;
            }
        }

        protected void postCompute(@NotNull T value) {
            // Doing something in post-compute helps prevent infinite recursion
        }
    }

    private static class LockBasedNullableLazyValue<T> implements NullableLazyValue<T> {
        private final Object lock;
        private final Computable<T> computable;

        private volatile boolean computed = false;
        @Nullable
        private volatile T value = null;

        public LockBasedNullableLazyValue(@NotNull Object lock, @NotNull Computable<T> computable) {
            this.lock = lock;
            this.computable = computable;
        }

        @Override
        @Nullable
        public T compute() {
            // NOTE: no local variables used here, because they would not reduce the number of volatile reads/writes

            // We want to guarantee that whenever computed = true, value is not null
            // First, read computed, then read value
            if (computed) {
                return value;
            }

            synchronized (lock) {
                if (!computed) {
                    T _value = computable.compute();

                    // First write value, then write computed
                    value = _value;
                    computed = true;

                    postCompute(_value);

                    return _value;
                }
                return value;
            }
        }

        protected void postCompute(@Nullable T value) {
            // Doing something in post-compute helps prevent infinite recursion
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
