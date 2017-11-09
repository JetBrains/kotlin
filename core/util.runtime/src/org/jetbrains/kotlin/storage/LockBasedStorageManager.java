/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.storage;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.WrappedValues;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockBasedStorageManager implements StorageManager {
    private static final String PACKAGE_NAME = StringsKt.substringBeforeLast(LockBasedStorageManager.class.getCanonicalName(), ".", "");

    public interface ExceptionHandlingStrategy {
        ExceptionHandlingStrategy THROW = new ExceptionHandlingStrategy() {
            @NotNull
            @Override
            public RuntimeException handleException(@NotNull Throwable throwable) {
                throw ExceptionUtilsKt.rethrow(throwable);
            }
        };

        /*
         * The signature of this method is a trick: it is used as
         *
         *     throw strategy.handleException(...)
         *
         * most implementations of this method throw exceptions themselves, so it does not matter what they return
         */
        @NotNull
        RuntimeException handleException(@NotNull Throwable throwable);
    }

    public static final StorageManager NO_LOCKS = new LockBasedStorageManager("NO_LOCKS", ExceptionHandlingStrategy.THROW, NoLock.INSTANCE) {
        @NotNull
        @Override
        protected <T> RecursionDetectedResult<T> recursionDetectedDefault() {
            return RecursionDetectedResult.fallThrough();
        }
    };

    @NotNull
    public static LockBasedStorageManager createWithExceptionHandling(@NotNull ExceptionHandlingStrategy exceptionHandlingStrategy) {
        return new LockBasedStorageManager(exceptionHandlingStrategy);
    }

    protected final Lock lock;
    private final ExceptionHandlingStrategy exceptionHandlingStrategy;
    private final String debugText;

    private LockBasedStorageManager(
            @NotNull String debugText,
            @NotNull ExceptionHandlingStrategy exceptionHandlingStrategy,
            @NotNull Lock lock
    ) {
        this.lock = lock;
        this.exceptionHandlingStrategy = exceptionHandlingStrategy;
        this.debugText = debugText;
    }

    public LockBasedStorageManager() {
        this(defaultDebugName(), ExceptionHandlingStrategy.THROW, new ReentrantLock());
    }

    protected LockBasedStorageManager(@NotNull ExceptionHandlingStrategy exceptionHandlingStrategy) {
        this(defaultDebugName(), exceptionHandlingStrategy, new ReentrantLock());
    }

    private static String defaultDebugName() {
        return "<unknown creating class>";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " (" + debugText + ")";
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(@NotNull Function1<? super K, ? extends V> compute) {
        return createMemoizedFunction(compute, LockBasedStorageManager.<K>createConcurrentHashMap());
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(
            @NotNull Function1<? super K, ? extends V> compute,
            @NotNull ConcurrentMap<K, Object> map
    ) {
        return new MapBasedMemoizedFunctionToNotNull<K, V>(this, map, compute);
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(@NotNull Function1<? super K, ? extends V> compute) {
        return createMemoizedFunctionWithNullableValues(compute, LockBasedStorageManager.<K>createConcurrentHashMap());
    }

    @Override
    @NotNull
    public  <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(
            @NotNull Function1<? super K, ? extends V> compute,
            @NotNull ConcurrentMap<K, Object> map
    ) {
        return new MapBasedMemoizedFunction<K, V>(this, map, compute);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createLazyValue(@NotNull Function0<? extends T> computable) {
        return new LockBasedNotNullLazyValue<T>(this, computable);
    }

    @NotNull
    @Override
    public <T> NotNullLazyValue<T> createRecursionTolerantLazyValue(
            @NotNull Function0<? extends T> computable, @NotNull final T onRecursiveCall
    ) {
        return new LockBasedNotNullLazyValue<T>(this, computable) {
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
            @NotNull final Function1<? super T, Unit> postCompute
    ) {
        return new LockBasedNotNullLazyValue<T>(this, computable) {
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
        return new LockBasedLazyValue<T>(this, computable);
    }

    @NotNull
    @Override
    public <T> NullableLazyValue<T> createRecursionTolerantNullableLazyValue(@NotNull Function0<? extends T> computable, final T onRecursiveCall) {
        return new LockBasedLazyValue<T>(this, computable) {
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
            @NotNull Function0<? extends T> computable, @NotNull final Function1<? super T, Unit> postCompute
    ) {
        return new LockBasedLazyValue<T>(this, computable) {
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
        catch (Throwable throwable) {
            throw exceptionHandlingStrategy.handleException(throwable);
        }
        finally {
            lock.unlock();
        }
    }

    @NotNull
    private static <K> ConcurrentMap<K, Object> createConcurrentHashMap() {
        // memory optimization: fewer segments and entries stored
        return new ConcurrentHashMap<K, Object>(3, 1, 2);
    }

    @NotNull
    protected <T> RecursionDetectedResult<T> recursionDetectedDefault() {
        throw sanitizeStackTrace(new IllegalStateException("Recursive call in a lazy value under " + this));
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
            assert !fallThrough : "A value requested from FALL_THROUGH in " + this;
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

    // Being static is memory optimization to prevent capturing outer-class reference at each level of inheritance hierarchy
    private static class LockBasedLazyValue<T> implements NullableLazyValue<T> {
        private final LockBasedStorageManager storageManager;
        private final Function0<? extends T> computable;

        @Nullable
        private volatile Object value = NotValue.NOT_COMPUTED;

        public LockBasedLazyValue(@NotNull LockBasedStorageManager storageManager, @NotNull Function0<? extends T> computable) {
            this.storageManager = storageManager;
            this.computable = computable;
        }

        @Override
        public boolean isComputed() {
            return value != NotValue.NOT_COMPUTED && value != NotValue.COMPUTING;
        }

        @Override
        public boolean isComputing() {
            return value == NotValue.COMPUTING;
        }

        @Override
        public T invoke() {
            Object _value = value;
            if (!(_value instanceof NotValue)) return WrappedValues.unescapeThrowable(_value);

            storageManager.lock.lock();
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
                    if (ExceptionUtilsKt.isProcessCanceledException(throwable)) {
                        value = NotValue.NOT_COMPUTED;
                        //noinspection ConstantConditions
                        throw (RuntimeException)throwable;
                    }

                    if (value == NotValue.COMPUTING) {
                        // Store only if it's a genuine result, not something thrown through recursionDetected()
                        value = WrappedValues.escapeThrowable(throwable);
                    }
                    throw storageManager.exceptionHandlingStrategy.handleException(throwable);
                }
            }
            finally {
                storageManager.lock.unlock();
            }
        }

        /**
         * @param firstTime {@code true} when recursion has been just detected, {@code false} otherwise
         * @return a value to be returned on a recursive call or subsequent calls
         */
        @NotNull
        protected RecursionDetectedResult<T> recursionDetected(boolean firstTime) {
            return storageManager.recursionDetectedDefault();
        }

        protected void postCompute(T value) {
            // Doing something in post-compute helps prevent infinite recursion
        }
    }

    private static class LockBasedNotNullLazyValue<T> extends LockBasedLazyValue<T> implements NotNullLazyValue<T> {

        public LockBasedNotNullLazyValue(@NotNull LockBasedStorageManager storageManager, @NotNull Function0<? extends T> computable) {
            super(storageManager, computable);
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
        private final LockBasedStorageManager storageManager;
        private final ConcurrentMap<K, Object> cache;
        private final Function1<? super K, ? extends V> compute;

        public MapBasedMemoizedFunction(
                @NotNull LockBasedStorageManager storageManager,
                @NotNull ConcurrentMap<K, Object> map,
                @NotNull Function1<? super K, ? extends V> compute
        ) {
            this.storageManager = storageManager;
            this.cache = map;
            this.compute = compute;
        }

        @Override
        @Nullable
        public V invoke(K input) {
            Object value = cache.get(input);
            if (value != null && value != NotValue.COMPUTING) return WrappedValues.unescapeExceptionOrNull(value);

            storageManager.lock.lock();
            try {
                value = cache.get(input);
                if (value == NotValue.COMPUTING) {
                    throw recursionDetected(input);
                }
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
                        error = raceCondition(input, oldValue);
                        throw error;
                    }

                    return typedValue;
                }
                catch (Throwable throwable) {
                    if (ExceptionUtilsKt.isProcessCanceledException(throwable)) {
                        cache.remove(input);
                        //noinspection ConstantConditions
                        throw (RuntimeException)throwable;
                    }
                    if (throwable == error) {
                        throw storageManager.exceptionHandlingStrategy.handleException(throwable);
                    }

                    Object oldValue = cache.put(input, WrappedValues.escapeThrowable(throwable));
                    if (oldValue != NotValue.COMPUTING) {
                        throw raceCondition(input, oldValue);
                    }

                    throw storageManager.exceptionHandlingStrategy.handleException(throwable);
                }
            }
            finally {
                storageManager.lock.unlock();
            }
        }

        @NotNull
        private AssertionError recursionDetected(K input) {
            return sanitizeStackTrace(
                    new AssertionError("Recursion detected on input: " + input + " under " + storageManager)
            );
        }

        @NotNull
        private AssertionError raceCondition(K input, Object oldValue) {
            return sanitizeStackTrace(
                    new AssertionError("Race condition detected on input " + input + ". Old value is " + oldValue +
                                       " under " + storageManager)
            );
        }

        @Override
        public boolean isComputed(K key) {
            Object value = cache.get(key);
            return value != null && value != NotValue.COMPUTING;
        }

        protected LockBasedStorageManager getStorageManager() {
            return storageManager;
        }
    }

    private static class MapBasedMemoizedFunctionToNotNull<K, V> extends MapBasedMemoizedFunction<K, V> implements MemoizedFunctionToNotNull<K, V> {

        public MapBasedMemoizedFunctionToNotNull(
                @NotNull LockBasedStorageManager storageManager, @NotNull ConcurrentMap<K, Object> map,
                @NotNull Function1<? super K, ? extends V> compute
        ) {
            super(storageManager, map, compute);
        }

        @NotNull
        @Override
        public V invoke(K input) {
            V result = super.invoke(input);
            assert result != null : "compute() returned null under " + getStorageManager();
            return result;
        }
    }

    @NotNull
    public static LockBasedStorageManager createDelegatingWithSameLock(
            @NotNull LockBasedStorageManager base,
            @NotNull ExceptionHandlingStrategy newStrategy
    ) {
        return new LockBasedStorageManager(defaultDebugName(), newStrategy, base.lock);
    }

    @NotNull
    private static <T extends Throwable> T sanitizeStackTrace(@NotNull T throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int size = stackTrace.length;

        int firstNonStorage = -1;
        for (int i = 0; i < size; i++) {
            // Skip everything (memoized functions and lazy values) from package org.jetbrains.kotlin.storage
            if (!stackTrace[i].getClassName().startsWith(PACKAGE_NAME)) {
                firstNonStorage = i;
                break;
            }
        }
        assert firstNonStorage >= 0 : "This method should only be called on exceptions created in LockBasedStorageManager";

        List<StackTraceElement> list = Arrays.asList(stackTrace).subList(firstNonStorage, size);
        throwable.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        return throwable;
    }

    @NotNull
    @Override
    public <K, V> CacheWithNullableValues<K, V> createCacheWithNullableValues() {
        return new CacheWithNullableValuesBasedOnMemoizedFunction<K, V>(
                this, LockBasedStorageManager.<KeyWithComputation<K,V>>createConcurrentHashMap());
    }

    private static class CacheWithNullableValuesBasedOnMemoizedFunction<K, V> extends MapBasedMemoizedFunction<KeyWithComputation<K, V>, V> implements CacheWithNullableValues<K, V> {

        private CacheWithNullableValuesBasedOnMemoizedFunction(
                @NotNull LockBasedStorageManager storageManager,
                @NotNull ConcurrentMap<KeyWithComputation<K, V>, Object> map
        ) {
            super(storageManager, map, new Function1<KeyWithComputation<K, V>, V>() {
                @Override
                public V invoke(KeyWithComputation<K, V> computation) {
                    return computation.computation.invoke();
                }
            });
        }

        @Nullable
        @Override
        public V computeIfAbsent(K key, @NotNull Function0<? extends V> computation) {
            return invoke(new KeyWithComputation<K, V>(key, computation));
        }
    }

    @NotNull
    @Override
    public <K, V> CacheWithNotNullValues<K, V> createCacheWithNotNullValues() {
        return new CacheWithNotNullValuesBasedOnMemoizedFunction<K, V>(this, LockBasedStorageManager.<KeyWithComputation<K,V>>createConcurrentHashMap());
    }

    private static class CacheWithNotNullValuesBasedOnMemoizedFunction<K, V> extends CacheWithNullableValuesBasedOnMemoizedFunction<K, V> implements CacheWithNotNullValues<K, V> {

        private CacheWithNotNullValuesBasedOnMemoizedFunction(
                @NotNull LockBasedStorageManager storageManager,
                @NotNull ConcurrentMap<KeyWithComputation<K, V>, Object> map
        ) {
            super(storageManager, map);
        }

        @NotNull
        @Override
        public V computeIfAbsent(K key, @NotNull Function0<? extends V> computation) {
            V result = super.computeIfAbsent(key, computation);
            assert result != null : "computeIfAbsent() returned null under " + getStorageManager();
            return result;
        }
    }

    // equals and hashCode use only key
    private static class KeyWithComputation<K, V> {
        private final K key;
        private final Function0<? extends V> computation;

        public KeyWithComputation(K key, Function0<? extends V> computation) {
            this.key = key;
            this.computation = computation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KeyWithComputation<?, ?> that = (KeyWithComputation<?, ?>) o;

            if (!key.equals(that.key)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}
