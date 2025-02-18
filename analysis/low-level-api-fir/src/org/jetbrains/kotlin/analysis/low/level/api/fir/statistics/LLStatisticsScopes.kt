/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter

abstract class LLStatisticsScope(val name: String) {
    override fun toString(): String = name
}

fun OpenTelemetry.getMeter(scope: LLStatisticsScope): Meter = getMeter(scope.name)

interface LLCaffeineStatisticsScope {
    val hits: LLStatisticsScope
    val misses: LLStatisticsScope
    val evictions: LLStatisticsScope
}

object LLStatisticsScopes : LLStatisticsScope("kotlin.analysis") {
    object AnalysisSessions : LLStatisticsScope("$name.analysisSessions") {
        object Analyze : LLStatisticsScope("$name.analyze") {
            object Invocations : LLStatisticsScope("$name.invocations")
        }

        object LowMemoryCacheCleanup : LLStatisticsScope("$name.lowMemoryCacheCleanup") {
            object Invocations : LLStatisticsScope("$name.invocations")
        }
    }

    object SymbolProviders : LLStatisticsScope("$name.symbolProviders") {
        object Combined : LLStatisticsScope("$name.combined"), LLCaffeineStatisticsScope {
            object Hits : LLStatisticsScope("$name.hits")
            object Misses : LLStatisticsScope("$name.misses")
            object Evictions : LLStatisticsScope("$name.evictions")

            override val hits: LLStatisticsScope get() = Hits
            override val misses: LLStatisticsScope get() = Misses
            override val evictions: LLStatisticsScope get() = Evictions
        }
    }
}
