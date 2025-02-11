/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.caches

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * A wrapper around a [ConcurrentMap] which stores `null` values returned by the computation in the form of explicit [NullValue]s.
 */
@JvmInline
public value class NullableConcurrentCache<K : Any, V>(
    public val map: ConcurrentMap<K, Any> = ConcurrentHashMap(),
) {
    /**
     * Returns the value for the given [key] if it's contained in the cache, or computes the value with [compute] *outside the cache's
     * computation lock* and adds it to the cache.
     */
    public inline fun getOrPut(
        key: K,
        crossinline compute: (K) -> V?,
    ): V {
        return map.getOrPutWithNullableValue(key) { compute(key) }
    }
}
