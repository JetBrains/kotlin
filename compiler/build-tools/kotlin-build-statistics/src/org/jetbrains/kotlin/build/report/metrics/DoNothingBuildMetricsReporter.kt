/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

object DoNothingBuildMetricsReporter : BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric> {
    override fun startMeasure(time: GradleBuildTime) {
    }

    override fun endMeasure(time: GradleBuildTime) {
    }

    override fun addTimeMetricNs(time: GradleBuildTime, durationNs: Long) {
    }

    override fun addMetric(metric: GradleBuildPerformanceMetric, value: Long) {
    }

    override fun addTimeMetric(metric: GradleBuildPerformanceMetric) {
    }

    override fun addAttribute(attribute: BuildAttribute) {
    }

    override fun addGcMetric(metric: String, value: GcMetric) {
    }

    override fun startGcMetric(name: String, value: GcMetric) {
    }

    override fun endGcMetric(name: String, value: GcMetric) {
    }

    override fun getMetrics(): BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric> =
        BuildMetrics(
            BuildTimes(),
            BuildPerformanceMetrics(),
            BuildAttributes()
        )

    override fun addMetrics(metrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>) {}
}