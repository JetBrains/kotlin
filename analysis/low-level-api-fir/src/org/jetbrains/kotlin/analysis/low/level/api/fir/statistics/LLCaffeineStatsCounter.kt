/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics

import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.github.benmanes.caffeine.cache.stats.StatsCounter
import io.opentelemetry.api.metrics.Meter
import org.checkerframework.checker.index.qual.NonNegative

/**
 * A Caffeine [StatsCounter] which delegates to OpenTelemetry counters.
 */
internal class LLCaffeineStatsCounter(meter: Meter, scope: LLCaffeineStatisticsScope) : StatsCounter {
    private val hitCounter = meter.counterBuilder(scope.hits.name).build()

    private val missCounter = meter.counterBuilder(scope.misses.name).build()

    private val evictionCounter = meter.counterBuilder(scope.evictions.name).build()

    override fun recordHits(count: @NonNegative Int) {
        hitCounter.add(count.toLong())
    }

    override fun recordMisses(count: @NonNegative Int) {
        missCounter.add(count.toLong())
    }

    override fun recordLoadSuccess(loadTime: @NonNegative Long) {
    }

    override fun recordLoadFailure(loadTime: @NonNegative Long) {
    }

//    @Deprecated("Deprecated in Caffeine")
//    override fun recordEviction() {
//        evictionCounter.add(1)
//    }

    override fun recordEviction(weight: @NonNegative Int, cause: RemovalCause?) {
        evictionCounter.add(1)
    }

    /**
     * We cannot retrieve any stats from OpenTelemetry, so the snapshot will be empty.
     */
    override fun snapshot(): CacheStats = CacheStats.empty()
}
