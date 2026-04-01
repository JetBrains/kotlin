/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.StatsCounter
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface

/**
 * Returns the value for the given [key] if it's contained in the cache, or computes the value with [compute] *outside the cache's
 * computation lock* and adds it to the cache.
 */
@KaPlatformInterface
public fun <K : Any, V : Any> Cache<K, V>.getOrPut(key: K, compute: (K) -> V): V {
    // We should call `getIfPresent` on the `Cache` to record hits and misses if stat counters are configured. Calling `get` on `asMap()`
    // does not record stats.
    getIfPresent(key)?.let { return it }

    // We have to use `asMap()` here. `Cache` has `get`, but it needs a `Function` passed to it, which means there's at least one additional
    // allocation compared to `putIfAbsent`.
    //
    // Both hits and misses are recorded by `getIfPresent`, so using `asMap()` is fine when recording stats. In case of competition (two
    // threads calling `putIfAbsent` for the same key), one of the threads will have a "hit" into the cache with `putIfAbsent` that isn't
    // recorded. But we can consider all threads to have collectively missed, so not recording this "hit" is fine.
    val newValue = compute(key)
    return asMap().putIfAbsent(key, newValue) ?: newValue
}

/**
 * Applies the [StatsCounter] to the [Caffeine] cache builder if it's non-null, or otherwise doesn't register it.
 *
 * [withStatsCounter] exists because [Caffeine.recordStats] itself doesn't handle `null` stats counters.
 *
 * Beware: Some operations of [Cache.asMap] do not record stats, so it's recommended to use the Caffeine cache directly.
 */
@KaPlatformInterface
public fun <K, V> Caffeine<K, V>.withStatsCounter(statsCounter: StatsCounter?): Caffeine<K, V> {
    return if (statsCounter != null) {
        recordStats { statsCounter }
    } else {
        this
    }
}
