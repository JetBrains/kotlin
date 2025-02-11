/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.checkerframework.checker.index.qual.NonNegative

/**
 * A wrapper around a Caffeine [Cache] which stores `null` values returned by the computation in the form of explicit [NullValue]s. On a
 * conceptual level, this allows the cache to store failures so that future accesses to the same key don't recompute the same failure.
 */
@JvmInline
public value class NullableCaffeineCache<K : Any, V : Any>(
    public val cache: Cache<K, Any>,
) {
    /**
     * Creates a new [NullableCaffeineCache] by configuring a [Caffeine] builder with [configure].
     */
    public constructor(configure: (Caffeine<Any, Any>) -> Caffeine<Any, Any>) : this(configure(Caffeine.newBuilder()).build())

    /**
     * Returns the value for the given [key] if it's contained in the cache, or computes the value with [compute] and adds it to the cache.
     */
    public inline fun get(key: K, crossinline compute: (K) -> V?): V? =
        cache.get(key) { compute(it) ?: NullValue }?.nullValueToNull()

    /**
     * Returns the approximate number of entries in the cache.
     */
    public val estimatedSize: @NonNegative Long
        get() = cache.estimatedSize()
}
