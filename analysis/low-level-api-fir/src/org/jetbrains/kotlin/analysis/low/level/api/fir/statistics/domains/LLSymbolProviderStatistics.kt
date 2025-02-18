/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains

import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLCaffeineStatsCounter
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsScopes
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsService
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.getMeter

class LLSymbolProviderStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain {
    val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.SymbolProviders)

    /**
     * A global [Caffeine stats counter][com.github.benmanes.caffeine.cache.stats.StatsCounter] for combined symbol provider caches.
     */
    val combinedSymbolProviderCacheStatsCounter = LLCaffeineStatsCounter(meter, LLStatisticsScopes.SymbolProviders.Combined)
}
