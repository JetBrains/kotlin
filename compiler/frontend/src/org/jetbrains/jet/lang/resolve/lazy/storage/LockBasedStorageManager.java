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
import com.intellij.openapi.util.Computable;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.containers.ConcurrentWeakValueHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.WrappedValues;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LockBasedStorageManager implements StorageManager {
    public static LockBasedStorageManager create() {
        return new LockBasedStorageManager(true);
    }

    public static LockBasedStorageManager createWithoutExceptionMemoization() {
        return new LockBasedStorageManager(false);
    }

    private final Object lock = new Object() {
        @Override
        public String toString() {
            return "LockBasedStorageManager centralized lock";
        }
    };

    private final boolean memoizeExceptions;

    private LockBasedStorageManager(boolean memoizeExceptions) {
        this.memoizeExceptions = memoizeExceptions;
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(
            @NotNull Function<K, V> compute, @NotNull ReferenceKind valuesReferenceKind
    ) {
        ConcurrentMap<K, Object> map = createConcurrentMap(valuesReferenceKind);
        return new MapBasedMemoizedFunctionToNotNull<K, V>(lock, map, compute, memoizeExceptions);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull Function<K, V> compute, @NotNull ReferenceKind valuesReferenceKind
    ) {
        ConcurrentMap<K, Object> map = createConcurrentMap(valuesReferenceKind);
        return new MapBasedMemoizedFunction<K, V>(lock, map, compute, memoizeExceptions);
    }

    private static <K, V> ConcurrentMap<K, V> createConcurrentMap(ReferenceKind referenceKind) {
        return (referenceKind == ReferenceKind.WEAK) ? new ConcurrentWeakValueHashMap<K, V>() : new ConcurrentHashMap<K, V>();
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValue(@NotNull Computable<T> computable) {
        return new LockBasedNotNullLazyValue<T>(lock, computable, memoizeExceptions);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValueWithPostCompute(@NotNull Computable<T> computable, @NotNull final Consumer<T> postCompute) {
        return new LockBasedNotNullLazyValue<T>(lock, computable, memoizeExceptions) {
            @Override
            protected void postCompute(@NotNull T value) {
                postCompute.consume(value);
            }
        };
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValue(@NotNull Computable<T> computable) {
        return new LockBasedLazyValue<T>(lock, computable, memoizeExceptions);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(
            @NotNull Computable<T> computable, @NotNull final Consumer<T> postCompute
    ) {
        return new LockBasedLazyValue<T>(lock, computable, memoizeExceptions) {
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

    @Override
    public <T> T compute(@NotNull Computable<T> computable) {
        synchronized (lock) {
            return computable.compute();
        }
    }

    private static class LockBasedLazyValue<T> implements NullableLazyValue<T> {
        private final Object lock;
        private final Computable<T> computable;
        private final boolean memoizeExceptions;

        @Nullable
        private volatile Object value = null;

        public LockBasedLazyValue(@NotNull Object lock, @NotNull Computable<T> computable, boolean memoizeExceptions) {
            this.lock = lock;
            this.computable = computable;
            this.memoizeExceptions = memoizeExceptions;
        }

        @Override
        public T compute() {
            Object _value = value;
            if (_value != null) return WrappedValues.unescapeExceptionOrNull(_value);

            synchronized (lock) {
                _value = value;
                if (_value != null) return WrappedValues.unescapeExceptionOrNull(_value);

                try {
                    T typedValue = computable.compute();
                    value = WrappedValues.escapeNull(typedValue);
                    postCompute(typedValue);
                    return typedValue;
                }
                catch (Throwable throwable) {
                    if (memoizeExceptions) {
                        value = WrappedValues.escapeThrowable(throwable);
                    }
                    throw ExceptionUtils.rethrow(throwable);
                }
            }
        }

        protected void postCompute(T value) {
            // Doing something in post-compute helps prevent infinite recursion
        }
    }

    private static class LockBasedNotNullLazyValue<T> extends LockBasedLazyValue<T> implements NotNullLazyValue<T> {

        public LockBasedNotNullLazyValue(@NotNull Object lock, @NotNull Computable<T> computable, boolean memoizeExceptions) {
            super(lock, computable, memoizeExceptions);
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
        private final boolean memoizeExceptions;

        public MapBasedMemoizedFunction(
                @NotNull Object lock, @NotNull ConcurrentMap<K, Object> map,
                @NotNull Function<K, V> compute, boolean memoizeExceptions) {
            this.lock = lock;
            this.cache = map;
            this.compute = compute;
            this.memoizeExceptions = memoizeExceptions;
        }

        @Override
        @Nullable
        public V fun(@NotNull K input) {
            Object value = cache.get(input);
            if (value != null) return WrappedValues.unescapeExceptionOrNull(value);

            synchronized (lock) {
                value = cache.get(input);
                if (value != null) return WrappedValues.unescapeExceptionOrNull(value);

                try {
                    V typedValue = compute.fun(input);
                    Object oldValue = cache.put(input, WrappedValues.escapeNull(typedValue));
                    assert oldValue == null : "Race condition detected";

                    return typedValue;
                }
                catch (Throwable throwable) {
                    if (memoizeExceptions) {
                        Object oldValue = cache.put(input, WrappedValues.escapeThrowable(throwable));
                        assert oldValue == null : "Race condition detected";
                    }

                    throw ExceptionUtils.rethrow(throwable);
                }
            }
        }
    }

    private static class MapBasedMemoizedFunctionToNotNull<K, V> extends MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNotNull<K, V> {

        public MapBasedMemoizedFunctionToNotNull(
                @NotNull Object lock,
                @NotNull ConcurrentMap<K, Object> map,
                @NotNull Function<K, V> compute,
                boolean memoizeExceptions
        ) {
            super(lock, map, compute, memoizeExceptions);
        }

        @NotNull
        @Override
        public V fun(@NotNull K input) {
            V result = super.fun(input);
            assert result != null : "compute() returned null";
            return result;
        }
    }

    private static class LockProtectedContext implements BindingContext {
        private final Object lock;
        private final BindingContext context;

        private LockProtectedContext(Object lock, BindingContext context) {
            this.lock = lock;
            this.context = context;
        }

        @Override
        public Collection<Diagnostic> getDiagnostics() {
            synchronized (lock) {
                return context.getDiagnostics();
            }
        }

        @Nullable
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            synchronized (lock) {
                return context.get(slice, key);
            }
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            synchronized (lock) {
                return context.getKeys(slice);
            }
        }

        @NotNull
        @Override
        @TestOnly
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            synchronized (lock) {
                return context.getSliceContents(slice);
            }
        }
    }

    private static class LockProtectedTrace implements BindingTrace {
        private final Object lock;
        private final BindingTrace trace;
        private final BindingContext context;

        public LockProtectedTrace(@NotNull Object lock, @NotNull BindingTrace trace) {
            this.lock = lock;
            this.trace = trace;
            this.context = new LockProtectedContext(lock, trace.getBindingContext());
        }

        @Override
        public BindingContext getBindingContext() {
            return context;
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
