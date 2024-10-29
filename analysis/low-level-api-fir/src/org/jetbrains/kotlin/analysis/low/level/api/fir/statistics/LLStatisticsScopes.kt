/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter

internal abstract class LLStatisticsScope(val name: String) {
    override fun toString(): String = name
}

internal fun OpenTelemetry.getMeter(scope: LLStatisticsScope): Meter = getMeter(scope.name)

internal interface LLCaffeineStatisticsScope {
    val hits: LLStatisticsScope
    val misses: LLStatisticsScope
    val evictions: LLStatisticsScope
}

internal object LLStatisticsScopes : LLStatisticsScope("kotlin.analysis") {
    object AnalysisSessions : LLStatisticsScope("$name.analysisSessions") {
        object Analyze : LLStatisticsScope("$name.analyze") {
            object Invocations : LLStatisticsScope("$name.invocations")
        }

        object LowMemoryCacheCleanup : LLStatisticsScope("$name.lowMemoryCacheCleanup") {
            object Invocations : LLStatisticsScope("$name.invocations")
        }
    }

    object SymbolProviders : LLStatisticsScope("$name.symbolProviders") {
        // The number of uniquely computed "classifier names in package" sets per `KaModule`.
        // How to calculate: Keep a global "`KaModule` -> package name" map and record a hit when a classifier name set is computed (in any
        // symbol provider). Every new module/package name combination leads to one count increment.
        object UniqueClassifierNameSets : LLStatisticsScope("$name.uniqueClassifierNameSets")

        object Module : LLStatisticsScope("$name.module") {
            // NOTE: These are symbol provider hits themselves, i.e. "does the symbol provider return non-null here?"
            // Not to be confused with cache hits/misses, which need much clearer naming.
            object ClassHits : LLStatisticsScope("$name.classHits")
            object ClassMisses : LLStatisticsScope("$name.classMisses")
            object NestedClassMisses : LLStatisticsScope("$name.nestedClassMisses")
        }

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
