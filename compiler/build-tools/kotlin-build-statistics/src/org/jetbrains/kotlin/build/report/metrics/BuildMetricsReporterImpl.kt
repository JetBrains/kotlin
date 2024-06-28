/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import kotlin.collections.HashMap

open class BuildMetricsReporterImpl<B : BuildTime, P : BuildPerformanceMetric> : BuildMetricsReporter<B, P>, Serializable {
    private val myBuildTimeStartNs = HashMap<B, Long>()
    private val myGcPerformance = HashMap<String, GcMetric>()
    private val myBuildTimes = BuildTimes<B>()
    private val myBuildMetrics = BuildPerformanceMetrics<P>()
    private val myBuildAttributes = BuildAttributes()
    private val myGcMetrics = GcMetrics()

    override fun startMeasure(time: B) {
        if (time in myBuildTimeStartNs) {
            error("$time was restarted before it finished")
        }
        myBuildTimeStartNs[time] = System.nanoTime()
    }

    override fun endMeasure(time: B) {
        val startNs = myBuildTimeStartNs.remove(time) ?: error("$time finished before it started")
        val durationNs = System.nanoTime() - startNs
        myBuildTimes.addTimeNs(time, durationNs)
    }

    override fun startGcMetric(name: String, value: GcMetric) {
        if (name in myGcPerformance) {
            error("$name was restarted before it finished")
        }
        myGcPerformance[name] = value
    }

    override fun endGcMetric(name: String, value: GcMetric) {
        val startValue = myGcPerformance.remove(name) ?: error("$name finished before it started")
        val diff = value - startValue
        myGcMetrics.add(name, diff)
    }

    override fun addTimeMetricNs(time: B, durationNs: Long) {
        myBuildTimes.addTimeNs(time, durationNs)
    }

    override fun addMetric(metric: P, value: Long) {
        myBuildMetrics.add(metric, value)
    }

    override fun addTimeMetric(metric: P) {
        when (metric.getType()) {
            ValueType.NANOSECONDS -> myBuildMetrics.add(metric, System.nanoTime())
            ValueType.MILLISECONDS -> myBuildMetrics.add(metric, System.currentTimeMillis())
            ValueType.TIME -> myBuildMetrics.add(metric, System.currentTimeMillis())
            else -> error("Unable to add time metric for '${metric.getType()}' type")
        }

    }

    override fun addGcMetric(metric: String, value: GcMetric) {
        myGcMetrics.add(metric, value)
    }

    override fun addAttribute(attribute: BuildAttribute) {
        myBuildAttributes.add(attribute)
    }

    override fun getMetrics(): BuildMetrics<B, P> =
        BuildMetrics(
            buildTimes = myBuildTimes,
            buildPerformanceMetrics = myBuildMetrics,
            buildAttributes = myBuildAttributes,
            gcMetrics = myGcMetrics
        )

    override fun addMetrics(metrics: BuildMetrics<B, P>) {
        myBuildAttributes.addAll(metrics.buildAttributes)
        myBuildTimes.addAll(metrics.buildTimes)
        myBuildMetrics.addAll(metrics.buildPerformanceMetrics)
        myGcMetrics.addAll(metrics.gcMetrics)
    }
}