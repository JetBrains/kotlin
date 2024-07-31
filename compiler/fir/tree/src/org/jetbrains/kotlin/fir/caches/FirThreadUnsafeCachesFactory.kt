/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import java.util.concurrent.locks.ReentrantLock

object FirThreadUnsafeCachesFactory : FirCachesFactory() {
    override fun <K : Any, V, CONTEXT> createCache(createValue: (K, CONTEXT) -> V): FirCache<K, V, CONTEXT> =
        FirThreadUnsafeCache(createValue = createValue)

    override fun <K : Any, V, CONTEXT> createCache(
        initialCapacity: Int,
        loadFactor: Float,
        createValue: (K, CONTEXT) -> V
    ): FirCache<K, V, CONTEXT> =
        FirThreadUnsafeCache(
            NullableMap(HashMap(initialCapacity, loadFactor)),
            createValue
        )

    override fun <K : Any, V, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (K, CONTEXT) -> Pair<V, DATA>,
        postCompute: (K, V, DATA) -> Unit,
        sharedComputationLock: ReentrantLock?,
    ): FirCache<K, V, CONTEXT> =
        FirThreadUnsafeCacheWithPostCompute(createValue, postCompute)

    override fun <V> createLazyValue(createValue: () -> V): FirLazyValue<V> =
        FirThreadUnsafeValue(createValue)
}

@Suppress("UNCHECKED_CAST")
private class FirThreadUnsafeCache<K : Any, V, CONTEXT>(
    private val map: NullableMap<K, V> = NullableMap<K, V>(),
    private val createValue: (K, CONTEXT) -> V
) : FirCache<K, V, CONTEXT>() {

    override fun getValue(key: K, context: CONTEXT): V =
        map.getOrElse(key) {
            createValue(key, context).also { createdValue ->
                map[key] = createdValue
            }
        }

    override fun getValueIfComputed(key: K): V? =
        map.getOrElse(key) { null as V }
}


private class FirThreadUnsafeCacheWithPostCompute<K : Any, V, CONTEXT, DATA>(
    private val createValue: (K, CONTEXT) -> Pair<V, DATA>,
    private val postCompute: (K, V, DATA) -> Unit
) : FirCache<K, V, CONTEXT>() {
    private val map = NullableMap<K, V>()

    override fun getValue(key: K, context: CONTEXT): V =
        map.getOrElse(key) {
            val (createdValue, data) = createValue(key, context)
            map[key] = createdValue
            postCompute(key, createdValue, data)
            createdValue
        }


    @Suppress("UNCHECKED_CAST")
    override fun getValueIfComputed(key: K): V? =
        map.getOrElse(key) { null as V }
}

private class FirThreadUnsafeValue<V>(createValue: () -> V) : FirLazyValue<V>() {
    private val lazyValue by lazy(LazyThreadSafetyMode.NONE, createValue)
    override fun getValue(): V = lazyValue
}