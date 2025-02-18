/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.opentelemetry.api.OpenTelemetry
import org.jetbrains.kotlin.analysis.api.platform.statistics.KaStatisticsService
import org.jetbrains.kotlin.analysis.api.platform.statistics.KotlinOpenTelemetryProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains.LLAnalysisSessionStatistics
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains.LLStatisticsDomain
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains.LLSymbolProviderStatistics

/**
 * [LLStatisticsService] is the facilitator of low-level API statistics collection and reporting. The service manages the scheduler and the
 * individual [LLStatisticsDomain]s.
 *
 * The Analysis API uses [OpenTelemetry](https://opentelemetry.io) as a telemetry backend to report metrics to the Analysis API platform,
 * such as IntelliJ and its performance tests. See [KotlinOpenTelemetryProvider].
 *
 * This class is the only IntelliJ project service registered for low-level API statistics collection. The single entry point simplifies
 * handling of whether statistics are enabled (see [KaStatisticsService.areStatisticsEnabled]).
 */
class LLStatisticsService(val project: Project) : Disposable {
    val scheduler: LLStatisticsScheduler = LLStatisticsScheduler(this)

    val analysisSessions: LLAnalysisSessionStatistics = LLAnalysisSessionStatistics(this)

    val symbolProviders: LLSymbolProviderStatistics = LLSymbolProviderStatistics(this)

    val domains: List<LLStatisticsDomain> = listOf(analysisSessions, symbolProviders)

    val openTelemetry: OpenTelemetry
        get() = KotlinOpenTelemetryProvider.getInstance(project)?.openTelemetry
            ?: error("${LLStatisticsService::class.simpleName} should not be used when OpenTelemetry is not available.")

    var hasStarted: Boolean = false

    /**
     * Schedules periodic updates and information gathering if statistics collection is [enabled][KaStatisticsService.areStatisticsEnabled].
     * These tasks contribute to the collected statistics.
     *
     * Meters don't require [start] for initialization and can be used even before [start] has been called.
     *
     * Statistics collection will remain active until disposal of this service.
     */
    fun start() {
        synchronized(this) {
            if (hasStarted) return

            scheduler.schedule()
            hasStarted = true
        }
    }

    override fun dispose() {
        synchronized(this) {
            if (hasStarted) {
                scheduler.cancel()
            }
        }
    }

    companion object {
        /**
         * Returns an instance of [LLStatisticsService] *if* statistics are [enabled][KaStatisticsService.areStatisticsEnabled] and an
         * [OpenTelemetry] instance is available via [KotlinOpenTelemetryProvider].
         */
        fun getInstance(project: Project): LLStatisticsService? {
            if (!KaStatisticsService.areStatisticsEnabled) {
                return null
            }

            // To avoid a nullable OpenTelemetry instance throughout the statistics code, we require OpenTelemetry to be available for
            // `LLStatisticsService`.
            if (KotlinOpenTelemetryProvider.getInstance(project)?.openTelemetry == null) {
                return null
            }

            return project.service()
        }
    }
}
