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

    override fun startMeasure(time: BuildTime, startNs: Long) {
        if (time in myBuildTimeStartNs) {
            error("$time was restarted before it finished")
        }
        myBuildTimeStartNs[time] = startNs
    }

    override fun endMeasure(time: BuildTime, endNs: Long) {
        val startNs = myBuildTimeStartNs.remove(time) ?: error("$time finished before it started")
        val durationMs = (endNs - startNs) / 1_000_000
        myBuildTimes.add(time, durationMs)
    }

    override fun addTimeMetric(time: BuildTime, durationMs: Long) {
        myBuildTimes.add(time, durationMs)
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

    override fun addMetrics(metrics: BuildMetrics?) {
        if (metrics == null) return

        myBuildAttributes.addAll(metrics.buildAttributes)
        myBuildTimes.addAll(metrics.buildTimes)
        myBuildMetrics.addAll(metrics.buildPerformanceMetrics)
    }
}