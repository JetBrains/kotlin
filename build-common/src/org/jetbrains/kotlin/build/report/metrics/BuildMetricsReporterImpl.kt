/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import java.util.*

class BuildMetricsReporterImpl : BuildMetricsReporter, Serializable {
    private val myBuildTimeStartNs: EnumMap<BuildTime, Long> =
        EnumMap(
            BuildTime::class.java
        )
    private val myBuildTimes = BuildTimes()
    private val myBuildMetrics = BuildPerformanceMetrics()
    private val myBuildAttributes = BuildAttributes()

    override fun startMeasure(time: BuildTime) {
        if (time in myBuildTimeStartNs) {
            error("$time was restarted before it finished")
        }
        myBuildTimeStartNs[time] = System.nanoTime()
    }

    override fun endMeasure(time: BuildTime) {
        val startNs = myBuildTimeStartNs.remove(time) ?: error("$time finished before it started")
        val durationNs = System.nanoTime() - startNs
        myBuildTimes.addTimeNs(time, durationNs)
    }

    override fun addTimeMetricNs(time: BuildTime, durationNs: Long) {
        myBuildTimes.addTimeNs(time, durationNs)
    }

    override fun addMetric(metric: BuildPerformanceMetric, value: Long) {
        myBuildMetrics.add(metric, value)
    }

    override fun addAttribute(attribute: BuildAttribute) {
        myBuildAttributes.add(attribute)
    }

    override fun getMetrics(): BuildMetrics =
        BuildMetrics(
            buildTimes = myBuildTimes,
            buildPerformanceMetrics = myBuildMetrics,
            buildAttributes = myBuildAttributes
        )

    override fun addMetrics(metrics: BuildMetrics) {
        myBuildAttributes.addAll(metrics.buildAttributes)
        myBuildTimes.addAll(metrics.buildTimes)
        myBuildMetrics.addAll(metrics.buildPerformanceMetrics)
    }
}