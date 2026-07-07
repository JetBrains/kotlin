/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

object DoNothingBuildMetricsReporter : BuildMetricsReporter<BuildTimeMetric<out BuildPerformanceMetric>, BuildPerformanceMetric> {
    override fun startMeasure(time: BuildTimeMetric<out BuildPerformanceMetric>) {
    }

    override fun endMeasure(time: BuildTimeMetric<out BuildPerformanceMetric>) {
    }

    override fun addTimeMetricNs(time: BuildTimeMetric<out BuildPerformanceMetric>, durationNs: Long) {
    }

    override fun addMetric(metric: BuildPerformanceMetric, value: Long) {
    }

    override fun addTimeMetric(metric: BuildPerformanceMetric) {
    }

    override fun addAttribute(attribute: BuildAttribute) {
    }

    override fun addGcMetric(metric: String, value: GcMetric) {
    }

    override fun startGcMetric(name: String, value: GcMetric) {
    }

    override fun endGcMetric(name: String, value: GcMetric) {
    }

    override fun getMetrics(): BuildMetrics<BuildTimeMetric<out BuildPerformanceMetric>, BuildPerformanceMetric> =
        BuildMetrics(
            BuildTimes(),
            BuildPerformanceMetrics(),
            BuildAttributes()
        )

    override fun addMetrics(metrics: BuildMetrics<out BuildTimeMetric<out BuildPerformanceMetric>, out BuildPerformanceMetric>) {}
}
