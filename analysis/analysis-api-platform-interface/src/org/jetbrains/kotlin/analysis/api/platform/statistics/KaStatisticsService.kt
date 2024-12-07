/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.statistics

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService

/**
 * Collects and reports statistics about the Analysis API's internal components.
 *
 * Statistics collection needs to be enabled with the registry key `kotlin.analysis.statistics` ([areStatisticsEnabled]). This is needed
 * because statistics incur an overhead. If [areStatisticsEnabled] is `false`, [getInstance] will return `null`.
 *
 * When statistics collection is enabled, the [start] function should be called soon after the project has been opened. While most metrics
 * will be collected independently of whether [start] is called, the statistics service may need to schedule periodic updates and
 * information gathering. These scheduled tasks contribute to the reported statistics.
 *
 * [KaStatisticsService] uses the platform's [OpenTelemetry][io.opentelemetry.api.OpenTelemetry] instance (provided by
 * [KotlinOpenTelemetryProvider]) to report metrics. If no OpenTelemetry provider is available, statistics will not be collected and
 * reported.
 *
 * The reporting of Analysis API statistics is entirely dependent on the OpenTelemetry configuration. The Analysis API itself does not
 * publish any data itself, neither locally nor over any network connection. The Analysis API platform has full control over this part of
 * the process. For example, IntelliJ gathers and exports metrics data *locally* to a file, while Standalone does not set up any publishing
 * whatsoever.
 *
 * Statistics collection is only implemented for the K2 backend. In the K1 backend, [getInstance] will always return `null`.
 *
 * ### Usage
 *
 * When the Analysis API is used on top of IntelliJ and statistics collection is enabled, the `logs` folder will contain CSV and JSON files
 * called `open-telemetry-metrics.*` (one file per IntelliJ run). In the same logs folder, there is a file called
 * `open-telemetry-metrics-plotter.html`, which can be opened locally. This HTML file allows plotting the metrics gathered during a run of
 * IntelliJ simply by opening the relevant CSV in the metrics plotter interface.
 *
 * The metrics plotter won't immediately plot Analysis API metrics. Instead, the "plot other" section can be used to search for and plot
 * such metrics. All Analysis API metrics have the prefix `kotlin.analysis`.
 *
 * While local logging of statistics comes out of the box in IntelliJ, other Analysis API platforms need to set up statistics collection on
 * their own. Notably, Standalone does not collect any statistics at all.
 */
public interface KaStatisticsService : KaEngineService {
    /**
     * Schedules periodic updates and information gathering if statistics collection is [enabled][areStatisticsEnabled]. These tasks
     * contribute to the collected statistics.
     *
     * [start] should be called soon after the project has been opened. The function implementation is thread-safe and may be called from
     * any thread.
     */
    public fun start()

    public companion object {
        public val areStatisticsEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Registry.`is`("kotlin.analysis.statistics", false)
        }

        public fun getInstance(project: Project): KaStatisticsService? = if (areStatisticsEnabled) project.serviceOrNull() else null
    }
}
