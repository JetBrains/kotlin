/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.util.*

fun BuildReporter.reportPerformanceData(moduleStats: UnitStats) {
    if (moduleStats.linesCount > 0) {
        addMetric(SOURCE_LINES_NUMBER, moduleStats.linesCount.toLong())
    }

    fun reportLps(lpsMetrics: BuildPerformanceMetric, time: Time) {
        if (time != Time.ZERO) {
            addMetric(lpsMetrics, moduleStats.getLinesPerSecond(time).toLong())
        }
    }

    var codegenTime: Time = Time.ZERO

    moduleStats.forEachPhaseMeasurement { phaseType, time ->
        if (time == null) return@forEachPhaseMeasurement

        val gradleBuildTime = when (phaseType) {
            PhaseType.Initialization -> COMPILER_INITIALIZATION
            PhaseType.Analysis -> CODE_ANALYSIS
            PhaseType.TranslationToIr -> TRANSLATION_TO_IR
            PhaseType.IrPreLowering -> IR_PRE_LOWERING
            PhaseType.IrSerialization -> IR_SERIALIZATION
            PhaseType.KlibWriting -> KLIB_WRITING
            PhaseType.IrLowering -> {
                codegenTime += time
                IR_LOWERING
            }
            PhaseType.Backend -> {
                codegenTime += time
                BACKEND
            }
        }

        addTimeMetricNs(gradleBuildTime, time.nanos)

        moduleStats.dynamicStats?.filter { it.parentPhaseType == phaseType }?.forEach { (_, name, time) ->
            addDynamicTimeMetricNs(name, gradleBuildTime, time.nanos)
        }

        if (phaseType == PhaseType.Analysis) {
            reportLps(ANALYSIS_LPS, time)
        }
    }

    if (codegenTime != Time.ZERO) {
        addTimeMetricNs(CODE_GENERATION, codegenTime.nanos)
        reportLps(CODE_GENERATION_LPS, codegenTime)
    }
}