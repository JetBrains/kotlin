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

package org.jetbrains.kotlin.storage

import java.util.concurrent.ConcurrentMap

abstract class ObservableStorageManager(private val delegate: StorageManager) : StorageManager {
    protected abstract val <T> (() -> T).observable: () -> T
    protected abstract val <K, V> ((K) -> V).observable: (K) -> V

    override fun <K, V: Any> createMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V> {
        return delegate.createMemoizedFunction(compute.observable)
    }

    override fun <K, V: Any> createMemoizedFunctionWithNullableValues(compute: (K) -> V?): MemoizedFunctionToNullable<K, V> {
        return delegate.createMemoizedFunctionWithNullableValues(compute.observable)
    }

    override fun <K, V: Any> createMemoizedFunction(compute: (K) -> V, map: ConcurrentMap<K, Any>): MemoizedFunctionToNotNull<K, V> {
        return delegate.createMemoizedFunction(compute.observable, map)
    }

    override fun <K, V: Any> createMemoizedFunctionWithNullableValues(compute: (K) -> V, map: ConcurrentMap<K, Any>): MemoizedFunctionToNullable<K, V> {
        return delegate.createMemoizedFunctionWithNullableValues(compute.observable, map)
    }

    override fun <K, V : Any> createCacheWithNullableValues(): CacheWithNullableValues<K, V> {
        return delegate.createCacheWithNullableValues()
    }

    override fun <K, V : Any> createCacheWithNotNullValues(): CacheWithNotNullValues<K, V> {
        return delegate.createCacheWithNotNullValues()
    }

    override fun <T: Any> createLazyValue(computable: () -> T): NotNullLazyValue<T> {
        return delegate.createLazyValue(computable.observable)
    }

    override fun <T: Any> createRecursionTolerantLazyValue(computable: () -> T, onRecursiveCall: T): NotNullLazyValue<T> {
        return delegate.createRecursionTolerantLazyValue(computable.observable, onRecursiveCall)
    }

    override fun <T: Any> createLazyValueWithPostCompute(computable: () -> T, onRecursiveCall: ((Boolean) -> T)?, postCompute: (T) -> Unit): NotNullLazyValue<T> {
        return delegate.createLazyValueWithPostCompute(computable.observable, onRecursiveCall, postCompute)
    }

    override fun <T: Any> createNullableLazyValue(computable: () -> T?): NullableLazyValue<T> {
        return delegate.createNullableLazyValue(computable.observable)
    }

    override fun <T: Any> createRecursionTolerantNullableLazyValue(computable: () -> T?, onRecursiveCall: T?): NullableLazyValue<T> {
        return delegate.createRecursionTolerantNullableLazyValue(computable.observable, onRecursiveCall)
    }

    override fun <T: Any> createNullableLazyValueWithPostCompute(computable: () -> T?, postCompute: (T?) -> Unit): NullableLazyValue<T> {
        return delegate.createNullableLazyValueWithPostCompute(computable.observable, postCompute)
    }

    override fun <T> compute(computable: () -> T): T {
        return delegate.compute(computable.observable)
    }
}
