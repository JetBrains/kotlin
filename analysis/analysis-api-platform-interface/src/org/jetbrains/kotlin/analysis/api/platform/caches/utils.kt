/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.caches

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.StatsCounter

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
