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

package org.jetbrains.jet.lang.resolve.lazy;

import com.intellij.openapi.util.Computable;
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

import static org.jetbrains.jet.lang.resolve.lazy.StorageManager.MemoizationMode.WEAK;

public class LockBasedStorageManager implements StorageManager {

    private final Object lock = new Object() {
        @Override
        public String toString() {
            return "LockBasedStorageManager centralized lock";
        }
    };

    @NotNull
    @Override
    public <K, V> Function<K, V> createMemoizedFunction(@NotNull final Function<K, V> compute, @NotNull final MemoizationMode mode) {
        return createMemoizedFunction(compute, mode, false);
    }

    @NotNull
    @Override
    public <K, V> Function<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull Function<K, V> compute, @NotNull MemoizationMode modeForValues
    ) {
        return createMemoizedFunction(compute, modeForValues, true);
    }

    @NotNull
    private <K, V> Function<K, V> createMemoizedFunction(final Function<K, V> compute, final MemoizationMode mode, final boolean nullable) {
        return new Function<K, V>() {
            private final ConcurrentMap<K, LazyValue<V>> cache;
            {
                cache = (mode == WEAK) ? new ConcurrentWeakValueHashMap<K, LazyValue<V>>() : new ConcurrentHashMap<K, LazyValue<V>>();
            }

            @Override
            public V fun(@NotNull final K input) {
                LazyValue<V> lazyValue = cache.get(input);
                if (lazyValue != null) return lazyValue.get();

                Computable<V> computable = new Computable<V>() {
                    @Override
                    public V compute() {
                        return compute.fun(input);
                    }
                };
                lazyValue = nullable ? createNullableLazyValue(computable) : createLazyValue(computable);

                LazyValue<V> oldValue = cache.putIfAbsent(input, lazyValue);
                if (oldValue != null) return oldValue.get();

                return lazyValue.get();
            }
        };
    }

    @NotNull
    @Override
    public <T> LazyValue<T> createLazyValue(@NotNull Computable<T> computable) {
        return new LockBasedLazyValue<T>(lock, computable);
    }

    @NotNull
    @Override
    public <T> LazyValue<T> createNullableLazyValue(@NotNull Computable<T> computable) {
        return new LockBasedNullableLazyValue<T>(lock, computable);
    }

    @NotNull
    @Override
    public BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace) {
        // It seems safe to have a separate lock for traces:
        // no other locks will be acquired inside the trace operations
        return new LockProtectedTrace(lock, originalTrace);
    }

    private static class LockBasedLazyValue<T> implements LazyValue<T> {
        private final Object lock;
        private final Computable<T> computable;

        @Nullable
        private volatile T value;

        public LockBasedLazyValue(@NotNull Object lock, @NotNull Computable<T> computable) {
            this.lock = lock;
            this.computable = computable;
        }

        @NotNull
        @Override
        public T get() {
            T _value = value;
            if (_value != null) {
                return _value;
            }

            synchronized (lock) {
                _value = value;
                if (_value == null) {
                    _value = computable.compute();
                    value = _value;
                }
                return _value;
            }
        }
    }

    private static class LockBasedNullableLazyValue<T> implements LazyValue<T> {
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
        public T get() {
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

                    return _value;
                }
                return value;
            }
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
