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
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.WrappedValues;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LockBasedStorageManager implements StorageManager {

    protected final Object lock = new Object() {
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
    public <T> NotNullLazyValue<T> createRecursionTolerantLazyValue(
            @NotNull Computable<T> computable, @NotNull final T onRecursiveCall
    ) {
        return new LockBasedNotNullLazyValue<T>(lock, computable) {
            @Override
            protected Object recursionDetected() {
                return onRecursiveCall;
            }
        };
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValueWithPostCompute(
            @NotNull Computable<T> computable,
            final Computable<Object> onRecursiveCall,
            @NotNull final Consumer<T> postCompute
    ) {
        return new LockBasedNotNullLazyValue<T>(lock, computable) {
            @Nullable
            @Override
            protected Object recursionDetected() {
                if (onRecursiveCall == null) {
                    return super.recursionDetected();
                }
                return onRecursiveCall.compute();
            }

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
    public <T> NullableLazyValue<T> createRecursionTolerantNullableLazyValue(@NotNull Computable<T> computable, final T onRecursiveCall) {
        return new LockBasedLazyValue<T>(lock, computable) {
            @Override
            protected Object recursionDetected() {
                return WrappedValues.escapeNull(onRecursiveCall);
            }
        };
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

    @Override
    public <T> T compute(@NotNull Computable<T> computable) {
        synchronized (lock) {
            return computable.compute();
        }
    }

    private static class LockBasedLazyValue<T> implements NullableLazyValue<T> {

        private static final Object COMPUTING = new Object();

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
            if (_value != null && _value != COMPUTING) return WrappedValues.unescapeExceptionOrNull(_value);

            synchronized (lock) {
                _value = value;
                if (_value == COMPUTING) {
                    Object result = recursionDetected();
                    if (result != null) {
                        return WrappedValues.unescapeExceptionOrNull(result);
                    }
                }

                if (_value != null) return WrappedValues.unescapeExceptionOrNull(_value);

                value = COMPUTING;
                try {
                    T typedValue = computable.compute();
                    value = WrappedValues.escapeNull(typedValue);
                    postCompute(typedValue);
                    return typedValue;
                }
                catch (Throwable throwable) {
                    value = WrappedValues.escapeThrowable(throwable);
                    throw ExceptionUtils.rethrow(throwable);
                }
            }
        }

        /**
         * @return {@code null} to proceed, a wrapped value otherwise, see WrappedValues
         * @throws DO NOT throw exceptions from implementations of this method, instead return WrappedValues.escapeThrowable(exception)
         */
        @Nullable
        protected Object recursionDetected() {
            return WrappedValues.escapeThrowable(new IllegalStateException("Recursive call in a lazy value"));
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
                    Object oldValue = cache.put(input, WrappedValues.escapeThrowable(throwable));
                    assert oldValue == null : "Race condition detected";

                    throw ExceptionUtils.rethrow(throwable);
                }
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
}
