/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.utils

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime

class TestBuildReporter(
    val testICReporter: TestICReporter,
    buildMetricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>(testICReporter, buildMetricsReporter) {
    fun reportCachesDump(cachesDump: String) {
        testICReporter.cachesDump = cachesDump
    }
}