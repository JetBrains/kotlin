/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.NullValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.nullValueToNull

/**
 * A wrapper around a Caffeine [Cache] which stores `null` values returned by the computation in the form of explicit [NullValue]s. On a
 * conceptual level, this allows the cache to store failures so that future accesses to the same key don't recompute the same failure.
 */
internal class NullableCaffeineCache<K : Any, V : Any>(configure: (Caffeine<Any, Any>) -> Caffeine<Any, Any>) {
    private val cache: Cache<K, Any> = configure(Caffeine.newBuilder()).build()

    /**
     * Returns the value for the given [key] if it's contained in the cache, or computes the value with [compute] and adds it to the cache.
     */
    fun get(key: K, compute: (K) -> V?): V? = cache.get(key) { compute(it) ?: NullValue }?.nullValueToNull()
}
