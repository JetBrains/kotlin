/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

object DoNothingBuildMetricsReporter : BuildMetricsReporter {
    override fun startMeasure(time: BuildTime, startNs: Long) {
    }

    override fun endMeasure(time: BuildTime, endNs: Long) {
    }

    override fun addTimeMetric(time: BuildTime, durationMs: Long) {
    }

    override fun addMetric(metric: BuildPerformanceMetric, value: Long) {
    }

    override fun addAttribute(attribute: BuildAttribute) {
    }

    override fun getMetrics(): BuildMetrics =
        BuildMetrics(
            BuildTimes(),
            BuildPerformanceMetrics(),
            BuildAttributes()
        )

    override fun addMetrics(metrics: BuildMetrics?) {}
}