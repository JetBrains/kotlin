/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains

import io.opentelemetry.api.metrics.LongCounter
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsScopes
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsService
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.getMeter

/**
 * Statistics for analysis sessions and `analyze` calls.
 */
class LLAnalysisSessionStatistics(statisticsService: LLStatisticsService) : LLStatisticsDomain {
    val meter = statisticsService.openTelemetry.getMeter(LLStatisticsScopes.AnalysisSessions)

    val analyzeCallCounter: LongCounter = meter.counterBuilder(LLStatisticsScopes.AnalysisSessions.Analyze.Invocations.name).build()

    val lowMemoryCacheCleanupInvocationCounter: LongCounter =
        meter.counterBuilder(LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations.name).build()
}
