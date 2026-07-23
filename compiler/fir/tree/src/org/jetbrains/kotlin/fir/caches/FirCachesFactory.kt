/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import kotlin.time.Duration

abstract class FirCachesFactory : FirSessionComponent {
    /**
     * Creates a cache which returns a value by key on demand if it is computed.
     * Otherwise, computes the value in [createValue] and caches it for future invocations.
     *
     * [FirCache.getValue] should not be called inside [createValue].
     *
     * Note that [createValue] might be called multiple times for the same value,
     * but all threads will always get the same value.
     *
     * Where:
     * [CONTEXT] -- type of value which be used to create value by [createValue]
     *
     * Consider using [org.jetbrains.kotlin.fir.caches.createCache] shortcut if your cache does not need any kind of [CONTEXT] parameter.
     */
    abstract fun <K : Any, V, CONTEXT> createCache(createValue: (K, CONTEXT) -> V): FirCache<K, V, CONTEXT>

    /**
     * Creates a cache which returns a value by key on demand if it is computed.
     * Otherwise, computes the value in [createValue] and caches it for future invocations.
     *
     * [FirCache.getValue] should not be called inside [createValue].
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
     * Creates a cache which returns a caches value on demand if it is computed.
     * Otherwise, computes the value in two phases:
     *  - [createValue] -- creates values and stores value of type [V] to cache and passes [V] & [DATA] to [postCompute]
     *  - [postCompute] -- performs some operations on computed value after it placed into map
     *
     * [FirCache.getValue] can be safely called in [postCompute] from the same thread and the correct value computed by [createValue] will
     * be returned.
     *
     * [FirCache.getValue] should not be called inside [createValue].
     *
     * Where:
     *  [CONTEXT] -- type of value which be used to create value by [createValue]
     *  [DATA] -- type of additional data which will be passed from [createValue] to [postCompute]
     */
    abstract fun <K : Any, V, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (K, CONTEXT) -> Pair<V, DATA>,
        postCompute: (K, V, DATA) -> Unit
    ): FirCache<K, V, CONTEXT>

    /**
     * Creates a cache which returns a value by key on demand if it is computed.
     * Otherwise, computes the value in [createValue] and caches it for future invocations.
     *
     * [FirCache.getValue] should not be called inside [createValue].
     *
     * The cache may be limited in various dimensions, such as time, size, and the choice of references, as specified by [limits]. Limits
     * should be understood as *suggestions*. Whether a suggested limit is applied is up to the cache factory implementation. Hence, it is
     * legal for a cache factory to construct an entirely unlimited cache.
     *
     * Where:
     * [CONTEXT] -- type of value which be used to create value by [createValue]
     *
     * @param limits The suggested limits to apply to the cache. See [FirCacheLimits] for a description of the individual limit options.
     */
    abstract fun <K : Any, V, CONTEXT> createCacheWithSuggestedLimits(
        limits: FirCacheLimits,
        createValue: (K, CONTEXT) -> V,
    ): FirCache<K, V, CONTEXT>

    /**
     * Creates a cache which returns a value by key on demand if it is computed.
     * Otherwise, computes the value in [createValue] and caches it for future invocations.
     *
     * This is a convenience overload of [createCacheWithSuggestedLimits] which accepts the individual limit options directly instead of a
     * [FirCacheLimits] object. See [FirCacheLimits] for a description of each limit option.
     *
     * Where:
     * [CONTEXT] -- type of value which be used to create value by [createValue]
     *
     * @see FirCacheLimits
     */
    fun <K : Any, V, CONTEXT> createCacheWithSuggestedLimits(
        expirationAfterAccess: Duration? = null,
        maximumSize: Long? = null,
        keyStrength: FirCacheLimits.KeyReferenceStrength = FirCacheLimits.KeyReferenceStrength.STRONG,
        valueStrength: FirCacheLimits.ValueReferenceStrength = FirCacheLimits.ValueReferenceStrength.STRONG,
        createValue: (K, CONTEXT) -> V,
    ): FirCache<K, V, CONTEXT> =
        createCacheWithSuggestedLimits(
            FirCacheLimits(expirationAfterAccess, maximumSize, keyStrength, valueStrength),
            createValue,
        )

    abstract fun <V> createLazyValue(createValue: () -> V): FirLazyValue<V>

    /**
     * Creates a [FirLazyValueWithContext] which computes its value on demand, using a [CONTEXT] passed to
     * [FirLazyValueWithContext.getValue]. The [CONTEXT] is only used for the first computation.
     *
     * [createValue] might be called multiple times for the same value, but all threads will always get the same value.
     */
    abstract fun <V, CONTEXT> createLazyValueWithContext(createValue: (CONTEXT) -> V): FirLazyValueWithContext<V, CONTEXT>

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
}

val FirSession.firCachesFactory: FirCachesFactory by FirSession.sessionComponentAccessor()

inline fun <K : Any, V> FirCachesFactory.createCache(
    crossinline createValue: (K) -> V,
): FirCache<K, V, Nothing?> = createCache(
    createValue = { key, _ -> createValue(key) },
)

/**
 * A collection of limit options for a cache created via [FirCachesFactory.createCacheWithSuggestedLimits]. Each option limits the cache in
 * a particular dimension, such as time, size, or the choice of references. Every option is optional and defaults to an unlimited or strong
 * setting, so an empty [FirCacheLimits] suggests no limits at all.
 *
 * @param expirationAfterAccess The cache evicts entries after they haven't been accessed for a set amount of time. The cache is not
 *  required to register scheduled maintenance, so expiration of cache entries may require active cache access.
 * @param maximumSize If the cache exceeds the maximum size, it evicts entries based on a least-usage strategy.
 * @param keyStrength The strength of the key reference.
 * @param valueStrength The strength of the value reference.
 *
 * @see FirCachesFactory.createCacheWithSuggestedLimits
 */
data class FirCacheLimits(
    val expirationAfterAccess: Duration? = null,
    val maximumSize: Long? = null,
    val keyStrength: KeyReferenceStrength = KeyReferenceStrength.STRONG,
    val valueStrength: ValueReferenceStrength = ValueReferenceStrength.STRONG,
) {
    enum class KeyReferenceStrength {
        /**
         * An ordinary strong reference.
         */
        STRONG,

        /**
         * @see java.lang.ref.WeakReference
         */
        WEAK,
    }

    enum class ValueReferenceStrength {
        /**
         * An ordinary strong reference.
         */
        STRONG,

        /**
         * @see java.lang.ref.SoftReference
         */
        SOFT,

        /**
         * @see java.lang.ref.WeakReference
         */
        WEAK,
    }
}

/**
 * @see FirCachesFactory.createCacheWithSuggestedLimits
 */
inline fun <K : Any, V> FirCachesFactory.createCacheWithSuggestedLimits(
    expirationAfterAccess: Duration? = null,
    maximumSize: Long? = null,
    keyStrength: FirCacheLimits.KeyReferenceStrength = FirCacheLimits.KeyReferenceStrength.STRONG,
    valueStrength: FirCacheLimits.ValueReferenceStrength = FirCacheLimits.ValueReferenceStrength.STRONG,
    crossinline createValue: (K) -> V,
): FirCache<K, V, Nothing?> =
    createCacheWithSuggestedLimits(expirationAfterAccess, maximumSize, keyStrength, valueStrength) { key, _ -> createValue(key) }
