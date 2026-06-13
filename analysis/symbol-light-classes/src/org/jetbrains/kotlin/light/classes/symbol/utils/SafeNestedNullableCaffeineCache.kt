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
 * Regular Caffeine cache prohibits nested updates:
 * if some cache is currently computing a value, this cache cannot be updated in this value computation.
 * If [SafeNestedNullableCaffeineCache] gets such a query, it will try to read the already cached value
 * or perform a non-cached computation using the given provider otherwise.
 *
 * @property outerCache the constructed outer cache.
 * @property innerCacheFactory factory for creating nested caches. Is a fallback factory for [getOrPut] on the [outerCache].
 */
internal class SafeNestedNullableCaffeineCache<K1 : Any, K2 : Any, V : Any>(
    private val outerCache: Cache<K1, Cache<Any, Any>>,
    private val innerCacheFactory: () -> Cache<Any, Any>
) {
    private val firstKeysInProgress = ThreadLocal.withInitial { mutableSetOf<K1>() }

    fun getOrPut(firstKey: K1, secondKey: K2?, compute: (K1, K2?) -> V?): V? {
        val innerCache = outerCache.getOrPut(firstKey) { innerCacheFactory() }
        val secondKeyNonNull = secondKey ?: NullValue

        if (!firstKeysInProgress.get().add(firstKey)) {
            val cachedValue = innerCache.getIfPresent(secondKeyNonNull)
            if (cachedValue != null) {
                return cachedValue.nullValueToNull()
            }

            return compute(firstKey, secondKey)
        }

        return try {
            val computedValue = innerCache.get(secondKeyNonNull) { secondKeyValue ->
                compute(firstKey, secondKeyValue.nullValueToNull()) ?: NullValue
            }

            computedValue?.nullValueToNull()
        } finally {
            firstKeysInProgress.get().remove(firstKey)
        }
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
