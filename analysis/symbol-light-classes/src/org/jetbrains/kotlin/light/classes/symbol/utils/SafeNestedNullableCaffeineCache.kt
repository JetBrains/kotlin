/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.utils

import com.github.benmanes.caffeine.cache.Cache
import org.jetbrains.kotlin.analysis.api.platform.caches.getOrPut

/**
 * A simple wrapper for the nested [Cache] construction that allows storing `null` second keys and values as explicit objects.
 *
 * Values are computed outside the cache: [Cache.get] holds a lock of the underlying map while the value is computed,
 * which blocks other updates of the map (e.g., resizes). The computation might wait for another thread (e.g., FIR lazy resolution)
 * which updates the same cache, so computing under the lock can lead to a deadlock.
 * As a result, the same value might be computed several times concurrently, but only the first one is stored and returned.
 *
 * @property outerCache the constructed outer cache.
 * @property innerCacheFactory factory for creating nested caches. Is a fallback factory for [getOrPut] on the [outerCache].
 */
internal class SafeNestedNullableCaffeineCache<K1 : Any, K2 : Any, V : Any>(
    private val outerCache: Cache<K1, Cache<Any, Any>>,
    private val innerCacheFactory: () -> Cache<Any, Any>
) {
    fun getOrPut(firstKey: K1, secondKey: K2?, compute: (K1, K2?) -> V?): V? {
        val innerCache = outerCache.getOrPut(firstKey) { innerCacheFactory() }
        return innerCache.getOrPut(secondKey ?: NullValue) { compute(firstKey, secondKey) ?: NullValue }.nullValueToNull()
    }

    fun invalidateAll() {
        outerCache.invalidateAll()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any.nullValueToNull(): T = when (this) {
        NullValue -> null
        else -> this
    } as T

    private object NullValue
}
