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

import jet.Function0;
import jet.Function1;
import jet.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.WrappedValues;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockBasedStorageManager implements StorageManager {

    public static final StorageManager NO_LOCKS = new LockBasedStorageManager(NoLock.INSTANCE) {
        @Override
        public String toString() {
            return "NO_LOCKS";
        }
    };

    protected final Lock lock;

    public LockBasedStorageManager() {
        this(new ReentrantLock());
    }

    private LockBasedStorageManager(@NotNull Lock lock) {
        this.lock = lock;
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(@NotNull Function1<K, V> compute) {
        return createMemoizedFunction(compute, new ConcurrentHashMap<K, Object>());
    }

    @NotNull
    protected  <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(
            @NotNull Function1<K, V> compute,
            @NotNull ConcurrentMap<K, Object> map
    ) {
        return new MapBasedMemoizedFunctionToNotNull<K, V>(lock, map, compute);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(@NotNull Function1<K, V> compute) {
        return createMemoizedFunctionWithNullableValues(compute, new ConcurrentHashMap<K, Object>());
    }

    @NotNull
    protected <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull Function1<K, V> compute,
            @NotNull ConcurrentMap<K, Object> map
    ) {
        return new MapBasedMemoizedFunction<K, V>(lock, map, compute);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValue(@NotNull Function0<T> computable) {
        return new LockBasedNotNullLazyValue<T>(lock, computable);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createRecursionTolerantLazyValue(
            @NotNull Function0<T> computable, @NotNull final T onRecursiveCall
    ) {
        return new LockBasedNotNullLazyValue<T>(lock, computable) {
            @Override
            protected T recursionDetected(boolean firstTime) {
                return onRecursiveCall;
            }
        };
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValueWithPostCompute(
            @NotNull Function0<T> computable,
            final Function1<Boolean, T> onRecursiveCall,
            @NotNull final Function1<T, Unit> postCompute
    ) {
        return new LockBasedNotNullLazyValue<T>(lock, computable) {
            @Nullable
            @Override
            protected T recursionDetected(boolean firstTime) {
                if (onRecursiveCall == null) {
                    return super.recursionDetected(firstTime);
                }
                return onRecursiveCall.invoke(firstTime);
            }

            @Override
            protected void postCompute(@NotNull T value) {
                postCompute.invoke(value);
            }
        };
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValue(@NotNull Function0<T> computable) {
        return new LockBasedLazyValue<T>(lock, computable);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createRecursionTolerantNullableLazyValue(@NotNull Function0<T> computable, final T onRecursiveCall) {
        return new LockBasedLazyValue<T>(lock, computable) {
            @Override
            protected T recursionDetected(boolean firstTime) {
                return onRecursiveCall;
            }
        };
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(
            @NotNull Function0<T> computable, @NotNull final Function1<T, Unit> postCompute
    ) {
        return new LockBasedLazyValue<T>(lock, computable) {
            @Override
            protected void postCompute(@Nullable T value) {
                postCompute.invoke(value);
            }
        };
    }

    @Override
    public <T> T compute(@NotNull Function0<T> computable) {
        lock.lock();
        try {
            return computable.invoke();
        }
        finally {
            lock.unlock();
        }
    }

    private static class LockBasedLazyValue<T> implements NullableLazyValue<T> {

        private enum NotValue {
            NOT_COMPUTED,
            COMPUTING,
            RECURSION_WAS_DETECTED
        }

        private final Lock lock;
        private final Function0<T> computable;

        @Nullable
        private volatile Object value = NotValue.NOT_COMPUTED;

        public LockBasedLazyValue(@NotNull Lock lock, @NotNull Function0<T> computable) {
            this.lock = lock;
            this.computable = computable;
        }

        @Override
        public boolean isComputed() {
            return value != NotValue.NOT_COMPUTED && value != NotValue.COMPUTING;
        }

        @Override
        public T invoke() {
            Object _value = value;
            if (!(value instanceof NotValue)) return WrappedValues.unescapeThrowable(_value);

            lock.lock();
            try {
                _value = value;
                if (!(_value instanceof NotValue)) return WrappedValues.unescapeThrowable(_value);

                if (_value == NotValue.COMPUTING) {
                    value = NotValue.RECURSION_WAS_DETECTED;
                    return recursionDetected(/*firstTime = */ true);
                }

                if (_value == NotValue.RECURSION_WAS_DETECTED) {
                    return recursionDetected(/*firstTime = */ false);
                }

                value = NotValue.COMPUTING;
                try {
                    T typedValue = computable.invoke();
                    value = typedValue;
                    postCompute(typedValue);
                    return typedValue;
                }
                catch (Throwable throwable) {
                    if (value == NotValue.COMPUTING) {
                        // Store only if it's a genuine result, not something thrown through recursionDetected()
                        value = WrappedValues.escapeThrowable(throwable);
                    }
                    throw ExceptionUtils.rethrow(throwable);
                }
            }
            finally {
                lock.unlock();
            }
        }

        /**
         * @param firstTime {@code true} when recursion has been just detected, {@code false} otherwise
         * @return a value to be returned on a recursive call or subsequent calls
         */
        @Nullable
        protected T recursionDetected(boolean firstTime) {
            throw new IllegalStateException("Recursive call in a lazy value");
        }

        protected void postCompute(T value) {
            // Doing something in post-compute helps prevent infinite recursion
        }
    }

    private static class LockBasedNotNullLazyValue<T> extends LockBasedLazyValue<T> implements NotNullLazyValue<T> {

        public LockBasedNotNullLazyValue(@NotNull Lock lock, @NotNull Function0<T> computable) {
            super(lock, computable);
        }

        @Override
        @NotNull
        public T invoke() {
            T result = super.invoke();
            assert result != null : "compute() returned null";
            return result;
        }
    }

    private static class MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNullable<K, V> {
        private final Lock lock;
        private final ConcurrentMap<K, Object> cache;
        private final Function1<K, V> compute;

        public MapBasedMemoizedFunction(@NotNull Lock lock, @NotNull ConcurrentMap<K, Object> map, @NotNull Function1<K, V> compute) {
            this.lock = lock;
            this.cache = map;
            this.compute = compute;
        }

        @Override
        @Nullable
        public V invoke(K input) {
            Object value = cache.get(input);
            if (value != null) return WrappedValues.unescapeExceptionOrNull(value);

            lock.lock();
            try {
                value = cache.get(input);
                if (value != null) return WrappedValues.unescapeExceptionOrNull(value);

                try {
                    V typedValue = compute.invoke(input);
                    Object oldValue = cache.put(input, WrappedValues.escapeNull(typedValue));
                    assert oldValue == null : "Race condition or recursion detected. Old value is " + oldValue;

                    return typedValue;
                }
                catch (Throwable throwable) {
                    Object oldValue = cache.put(input, WrappedValues.escapeThrowable(throwable));
                    assert oldValue == null : "Race condition or recursion detected. Old value is " + oldValue;

                    throw ExceptionUtils.rethrow(throwable);
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

    private static class MapBasedMemoizedFunctionToNotNull<K, V> extends MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNotNull<K, V> {

        public MapBasedMemoizedFunctionToNotNull(
                @NotNull Lock lock,
                @NotNull ConcurrentMap<K, Object> map,
                @NotNull Function1<K, V> compute
        ) {
            super(lock, map, compute);
        }

        @NotNull
        @Override
        public V invoke(K input) {
            V result = super.invoke(input);
            assert result != null : "compute() returned null";
            return result;
        }
    }
}
