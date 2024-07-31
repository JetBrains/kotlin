/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import java.util.concurrent.locks.ReentrantLock

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
     * Creates a cache which computes and caches a value on demand.
     *
     * Each value is computed in two phases:
     *
     *  - [createValue] -- Creates a value of type [V] given the key [K] and context [CONTEXT], stores it in the cache, and passes [V] and
     *  [DATA] to [postCompute].
     *  - [postCompute] -- Performs a post-computation on the created and already cached value.
     *
     * [FirCache.getValue] should not be called inside [createValue]. However, it may be safely called in [postCompute] from the same thread
     * and the correct value created by [createValue] will be returned, although it may be in an incomplete state of post-computation.
     *
     * The motivation for post-computation is to be able to access other values in the cache during computation, and even values which are
     * currently being computed in case of cycles.
     *
     * By default, value computation is synchronized on each individual computed value, meaning that two different values may be calculated
     * and post-computed concurrently. This strategy works as long as there are no mutual dependencies between post-computations (see
     * [KT-70327](https://youtrack.jetbrains.com/issue/KT-70327)). For example, a class C1 may access a class C2 during its
     * post-computation, and vice versa. This carries the risk of a deadlock if C1 and C2 are being processed concurrently in different
     * threads. To circumvent this issue, a [sharedComputationLock] may be specified to confine value computation (both [createValue] and
     * [postCompute]) to a single thread.
     *
     * @param CONTEXT The type of the context providing additional information to [createValue].
     * @param DATA The type of additional data that will be passed from [createValue] to [postCompute].
     */
    abstract fun <K : Any, V, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (K, CONTEXT) -> Pair<V, DATA>,
        postCompute: (K, V, DATA) -> Unit,
        sharedComputationLock: ReentrantLock? = null,
    ): FirCache<K, V, CONTEXT>

    abstract fun <V> createLazyValue(createValue: () -> V): FirLazyValue<V>
}

val FirSession.firCachesFactory: FirCachesFactory by FirSession.sessionComponentAccessor()

inline fun <K : Any, V> FirCachesFactory.createCache(
    crossinline createValue: (K) -> V,
): FirCache<K, V, Nothing?> = createCache(
    createValue = { key, _ -> createValue(key) },
)
