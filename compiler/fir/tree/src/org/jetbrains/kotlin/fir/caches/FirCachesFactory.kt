/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

abstract class FirCachesFactory : FirSessionComponent {
    /**
     * Creates a cache with returns a value by key on demand if it is computed
     * Otherwise computes the value in [createValue] and caches it for future invocations
     *
     * [FirCache.getValue] should not be called inside [createValue]
     *
     * Note, that [createValue] might be called multiple times for the same value,
     * but all threads will always get the same value
     *
     * Where:
     * [CONTEXT] -- type of value which be used to create value by [createValue]
     *
     * Consider using [org.jetbrains.kotlin.fir.caches.createCache] shortcut if your cache does not need any kind of [CONTEXT] parameter.
     */
    abstract fun <K : Any, V, CONTEXT> createCache(createValue: (K, CONTEXT) -> V): FirCache<K, V, CONTEXT>

    /**
     * Creates a cache with returns a value by key on demand if it is computed
     * Otherwise computes the value in [createValue] and caches it for future invocations
     *
     * [FirCache.getValue] should not be called inside [createValue]
     *
     * Where:
     * [CONTEXT] -- type of value which be used to create value by [createValue]
     *
     * @param initialCapacity initial capacity for the underlying cache map
     * @param loadFactor loadFactor for the underlying cache map
     */
    abstract fun <K : Any, V, CONTEXT> createCache(
        initialCapacity: Int,
        loadFactor: Float,
        createValue: (K, CONTEXT) -> V
    ): FirCache<K, V, CONTEXT>

    /**
     * Creates a cache with returns a caches value on demand if it is computed
     * Otherwise computes the value in two phases:
     *  - [createValue] -- creates values and stores value of type [V] to cache and passes [V] & [DATA] to [postCompute]
     *  - [postCompute] -- performs some operations on computed value after it placed into map
     *
     * [FirCache.getValue] can be safely called in postCompute from the same thread and correct value computed by [createValue] will be returned
     * [FirCache.getValue] should not be called inside [createValue]
     *
     * Where:
     *  [CONTEXT] -- type of value which be used to create value by [createValue]
     *  [DATA] -- type of additional data which will be passed from [createValue] to [postCompute]
     */
    abstract fun <K : Any, V, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (K, CONTEXT) -> Pair<V, DATA>,
        postCompute: (K, V, DATA) -> Unit
    ): FirCache<K, V, CONTEXT>

    abstract fun <V> createLazyValue(createValue: () -> V): FirLazyValue<V>

    /**
     * Creates a [FirLazyValue] which possibly references its value softly. If the referenced value is garbage-collected, it will be
     * recomputed with the [createValue] function.
     *
     * The lazy value doesn't make any guarantees regarding the number of invocations of [createValue] or the threads it is invoked in.
     *
     * Whether the lazy value actually references its value softly depends on the cache factory implementation. The cache factory may create
     * a lazy value which strongly references its value.
     */
    abstract fun <V> createPossiblySoftLazyValue(createValue: () -> V): FirLazyValue<V>

    @RequiresOptIn("This API is performance wise and should not be used in general code")
    annotation class PerformanceWise

    @PerformanceWise
    abstract val isThreadSafe: Boolean
}

val FirSession.firCachesFactory: FirCachesFactory by FirSession.sessionComponentAccessor()

inline fun <K : Any, V> FirCachesFactory.createCache(
    crossinline createValue: (K) -> V,
): FirCache<K, V, Nothing?> = createCache(
    createValue = { key, _ -> createValue(key) },
)
