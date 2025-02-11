/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.caches.getOrPutWithNullableValue
import org.jetbrains.kotlin.analysis.api.platform.caches.nullValueToNull
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirLazyValue
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal class FirThreadSafeCachesFactory(private val project: Project) : FirCachesFactory() {
    override fun <KEY : Any, VALUE, CONTEXT> createCache(createValue: (KEY, CONTEXT) -> VALUE): FirCache<KEY, VALUE, CONTEXT> =
        FirThreadSafeCache(createValue = createValue)

    override fun <K : Any, V, CONTEXT> createCache(
        initialCapacity: Int,
        loadFactor: Float,
        createValue: (K, CONTEXT) -> V
    ): FirCache<K, V, CONTEXT> =
        FirThreadSafeCache(
            ConcurrentHashMap<K, Any>(initialCapacity, loadFactor),
            createValue
        )


    override fun <KEY : Any, VALUE, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
        postCompute: (KEY, VALUE, DATA) -> Unit
    ): FirCache<KEY, VALUE, CONTEXT> =
        FirThreadSafeCacheWithPostCompute(createValue, postCompute)

    override fun <K : Any, V, CONTEXT> createCacheWithSuggestedLimits(
        expirationAfterAccess: Duration?,
        maximumSize: Long?,
        keyStrength: KeyReferenceStrength,
        valueStrength: ValueReferenceStrength,
        createValue: (K, CONTEXT) -> V
    ): FirCache<K, V, CONTEXT> {
        if (
            expirationAfterAccess == null &&
            maximumSize == null &&
            keyStrength == KeyReferenceStrength.STRONG &&
            valueStrength == ValueReferenceStrength.STRONG
        ) {
            return createCache(createValue)
        }

        val builder = Caffeine<K, V>.newBuilder()

        if (expirationAfterAccess != null) {
            builder.expireAfterAccess(expirationAfterAccess.toJavaDuration())
        }

        if (maximumSize != null) {
            builder.maximumSize(maximumSize.toLong())
        }

        if (keyStrength == KeyReferenceStrength.WEAK) {
            builder.weakKeys()
        }

        when (valueStrength) {
            ValueReferenceStrength.STRONG -> {}
            ValueReferenceStrength.SOFT -> builder.softValues()
            ValueReferenceStrength.WEAK -> builder.weakValues()
        }

        return FirCaffeineCache(builder.build(), createValue)
    }

    override fun <V> createLazyValue(createValue: () -> V): FirLazyValue<V> =
        FirThreadSafeValue(createValue)

    override fun <V> createPossiblySoftLazyValue(createValue: () -> V): FirLazyValue<V> =
        LLFirSoftLazyValue(project, createValue)

    @PerformanceWise
    override val isThreadSafe: Boolean
        get() = true
}

private class FirCaffeineCache<K : Any, V, CONTEXT>(
    private val cache: Cache<K, Any>,
    private val createValue: (K, CONTEXT) -> V,
) : FirCache<K, V, CONTEXT>() {

    /**
     * [Cache.get] cannot be used here as [createValue] may access the map recursively.
     */
    override fun getValue(key: K, context: CONTEXT): V = cache.asMap().getOrPutWithNullableValue(key) {
        createValue(it, context)
    }

    override fun getValueIfComputed(key: K): V? = cache.getIfPresent(key)?.nullValueToNull()
}
