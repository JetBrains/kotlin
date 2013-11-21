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
        @NotNull
        @Override
        protected <T> RecursionDetectedResult<T> recursionDetectedDefault() {
            return RecursionDetectedResult.fallThrough();
        }

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
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(@NotNull Function1<? super K, ? extends V> compute) {
        return createMemoizedFunction(compute, new ConcurrentHashMap<K, Object>());
    }

    @NotNull
    protected  <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(
            @NotNull Function1<? super K, ? extends V> compute,
            @NotNull ConcurrentMap<K, Object> map
    ) {
        return new MapBasedMemoizedFunctionToNotNull<K, V>(map, compute);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(@NotNull Function1<? super K, ? extends V> compute) {
        return createMemoizedFunctionWithNullableValues(compute, new ConcurrentHashMap<K, Object>());
    }

    @NotNull
    protected <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull Function1<? super K, ? extends V> compute,
            @NotNull ConcurrentMap<K, Object> map
    ) {
        return new MapBasedMemoizedFunction<K, V>(map, compute);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValue(@NotNull Function0<? extends T> computable) {
        return new LockBasedNotNullLazyValue<T>(computable);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createRecursionTolerantLazyValue(
            @NotNull Function0<? extends T> computable, @NotNull final T onRecursiveCall
    ) {
        return new LockBasedNotNullLazyValue<T>(computable) {
            @NotNull
            @Override
            protected RecursionDetectedResult<T> recursionDetected(boolean firstTime) {
                return RecursionDetectedResult.value(onRecursiveCall);
            }
        };
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValueWithPostCompute(
            @NotNull Function0<? extends T> computable,
            final Function1<? super Boolean, ? extends T> onRecursiveCall,
            @NotNull final Function1<? super T, ? extends Unit> postCompute
    ) {
        return new LockBasedNotNullLazyValue<T>(computable) {
            @NotNull
            @Override
            protected RecursionDetectedResult<T> recursionDetected(boolean firstTime) {
                if (onRecursiveCall == null) {
                    return super.recursionDetected(firstTime);
                }
                return RecursionDetectedResult.value(onRecursiveCall.invoke(firstTime));
            }

            @Override
            protected void postCompute(@NotNull T value) {
                postCompute.invoke(value);
            }
        };
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValue(@NotNull Function0<? extends T> computable) {
        return new LockBasedLazyValue<T>(computable);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createRecursionTolerantNullableLazyValue(@NotNull Function0<? extends T> computable, final T onRecursiveCall) {
        return new LockBasedLazyValue<T>(computable) {
            @NotNull
            @Override
            protected RecursionDetectedResult<T> recursionDetected(boolean firstTime) {
                return RecursionDetectedResult.value(onRecursiveCall);
            }
        };
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(
            @NotNull Function0<? extends T> computable, @NotNull final Function1<? super T, ? extends Unit> postCompute
    ) {
        return new LockBasedLazyValue<T>(computable) {
            @Override
            protected void postCompute(@Nullable T value) {
                postCompute.invoke(value);
            }
        };
    }

    @Override
    public <T> T compute(@NotNull Function0<? extends T> computable) {
        lock.lock();
        try {
            return computable.invoke();
        }
        finally {
            lock.unlock();
        }
    }

    @NotNull
    protected <T> RecursionDetectedResult<T> recursionDetectedDefault() {
        throw new IllegalStateException("Recursive call in a lazy value");
    }

    private static class RecursionDetectedResult<T> {

        @NotNull
        public static <T> RecursionDetectedResult<T> value(T value) {
            return new RecursionDetectedResult<T>(value, false);
        }

        @NotNull
        public static <T> RecursionDetectedResult<T> fallThrough() {
            return new RecursionDetectedResult<T>(null, true);
        }

        private final T value;
        private final boolean fallThrough;

        private RecursionDetectedResult(T value, boolean fallThrough) {
            this.value = value;
            this.fallThrough = fallThrough;
        }

        public T getValue() {
            assert !fallThrough : "A value requested from FALL_THROUGH ";
            return value;
        }

        public boolean isFallThrough() {
            return fallThrough;
        }

        @Override
        public String toString() {
            return isFallThrough() ? "FALL_THROUGH" : String.valueOf(value);
        }
    }

    private enum NotValue {
        NOT_COMPUTED,
        COMPUTING,
        RECURSION_WAS_DETECTED
    }

    private class LockBasedLazyValue<T> implements NullableLazyValue<T> {

        private final Function0<? extends T> computable;

        @Nullable
        private volatile Object value = NotValue.NOT_COMPUTED;

        public LockBasedLazyValue(@NotNull Function0<? extends T> computable) {
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
                    RecursionDetectedResult<T> result = recursionDetected(/*firstTime = */ true);
                    if (!result.isFallThrough()) {
                        return result.getValue();
                    }
                }

                if (_value == NotValue.RECURSION_WAS_DETECTED) {
                    RecursionDetectedResult<T> result = recursionDetected(/*firstTime = */ false);
                    if (!result.isFallThrough()) {
                        return result.getValue();
                    }
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
        @NotNull
        protected RecursionDetectedResult<T> recursionDetected(boolean firstTime) {
            return recursionDetectedDefault();
        }

        protected void postCompute(T value) {
            // Doing something in post-compute helps prevent infinite recursion
        }
    }

    private class LockBasedNotNullLazyValue<T> extends LockBasedLazyValue<T> implements NotNullLazyValue<T> {

        public LockBasedNotNullLazyValue(@NotNull Function0<? extends T> computable) {
            super(computable);
        }

        @Override
        @NotNull
        public T invoke() {
            T result = super.invoke();
            assert result != null : "compute() returned null";
            return result;
        }
    }

    private class MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNullable<K, V> {
        private final ConcurrentMap<K, Object> cache;
        private final Function1<? super K, ? extends V> compute;

        public MapBasedMemoizedFunction(@NotNull ConcurrentMap<K, Object> map, @NotNull Function1<? super K, ? extends V> compute) {
            this.cache = map;
            this.compute = compute;
        }

        @Override
        @Nullable
        public V invoke(K input) {
            Object value = cache.get(input);
            if (value != null && value != NotValue.COMPUTING) return WrappedValues.unescapeExceptionOrNull(value);

            lock.lock();
            try {
                value = cache.get(input);
                assert value != NotValue.COMPUTING : "Recursion detected on input: " + input;
                if (value != null) return WrappedValues.unescapeExceptionOrNull(value);

                AssertionError error = null;
                try {
                    cache.put(input, NotValue.COMPUTING);
                    V typedValue = compute.invoke(input);
                    Object oldValue = cache.put(input, WrappedValues.escapeNull(typedValue));

                    // This code effectively asserts that oldValue is null
                    // The trickery is here because below we catch all exceptions thrown here, and this is the only exception that shouldn't be stored
                    // A seemingly obvious way to come about this case would be to declare a special exception class, but the problem is that
                    // one memoized function is likely to (indirectly) call another, and if this second one throws this exception, we are screwed
                    if (oldValue != NotValue.COMPUTING) {
                        error = new AssertionError("Race condition detected on input " + input + ". Old value is " + oldValue);
                        throw error;
                    }

                    return typedValue;
                }
                catch (Throwable throwable) {
                    if (throwable == error) throw error;

                    Object oldValue = cache.put(input, WrappedValues.escapeThrowable(throwable));
                    assert oldValue == NotValue.COMPUTING : "Race condition detected on input " + input + ". Old value is " + oldValue;

                    throw ExceptionUtils.rethrow(throwable);
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

    private class MapBasedMemoizedFunctionToNotNull<K, V> extends MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNotNull<K, V> {

        public MapBasedMemoizedFunctionToNotNull(
                @NotNull ConcurrentMap<K, Object> map,
                @NotNull Function1<? super K, ? extends V> compute
        ) {
            super(map, compute);
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
