/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Cache
import java.util.concurrent.ConcurrentMap

/**
 * An object used as a representative for `null` in collections which prohibit `null` values.
 */
public object NullValue

/**
 * Converts [NullValue] to `null`, and all other instances of [this] to [V].
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
public inline fun <V> Any.nullValueToNull(): V = when (this) {
    NullValue -> null
    else -> this
} as V

/**
 * Implements [ConcurrentMap.getOrPut] with [NullValue] conversion.
 */
public inline fun <K : Any, R> ConcurrentMap<K, Any>.getOrPutWithNullableValue(
    key: K,
    crossinline compute: (K) -> Any?,
): R {
    val value = getOrPut(key) { compute(key) ?: NullValue }
    return value.nullValueToNull()
}

/**
 * Implements [Cache.getOrPut] with [NullValue] conversion.
 */
public inline fun <K : Any, R> Cache<K, Any>.getOrPutWithNullableValue(key: K, crossinline compute: (K) -> Any?): R =
    asMap().getOrPutWithNullableValue(key) { compute(key) }
