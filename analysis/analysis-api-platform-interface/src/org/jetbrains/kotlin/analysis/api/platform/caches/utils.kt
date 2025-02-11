/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.StatsCounter
import kotlin.collections.getOrPut

/**
 * Returns the value for the given [key] if it's contained in the cache, or computes the value with [compute] *outside the cache's
 * computation lock* and adds it to the cache.
 */
public fun <K : Any, V : Any> Cache<K, V>.getOrPut(key: K, compute: (K) -> V): V =
    asMap().getOrPut(key) { compute(key) }

/**
 * Applies the [StatsCounter] to the [Caffeine] cache builder if it's non-null, or otherwise doesn't register it.
 *
 * [withStatsCounter] exists because [Caffeine.recordStats] itself doesn't handle `null` stats counters.
 */
public fun <K, V> Caffeine<K, V>.withStatsCounter(statsCounter: StatsCounter?): Caffeine<K, V> {
    return if (statsCounter != null) {
        recordStats { statsCounter }
    } else {
        this
    }
}
