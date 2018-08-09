/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.util.PerformanceCounter
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

abstract class CommonCompilerPerformanceManager(private val presentableName: String) {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val measurements: MutableList<PerformanceMeasurement> = mutableListOf()
    protected var isEnabled: Boolean = false
    private var initStartNanos = PerformanceCounter.currentTime()
    private var analysisStart: Long = 0
    private var generationStart: Long = 0

    fun getMeasurementResults(): List<PerformanceMeasurement> = measurements

    fun enableCollectingPerformanceStatistics() {
        isEnabled = true
        PerformanceCounter.setTimeCounterEnabled(true)
    }

    open fun notifyCompilerInitialized() {
        if (!isEnabled) return
        recordInitializationTime()
    }

    open fun notifyCompilationFinished() {
        if (!isEnabled) return
        recordGcTime()
        recordJitCompilationTime()
        recordPerfCountersMeasurements()
    }

    open fun notifyAnalysisStarted() {
        analysisStart = PerformanceCounter.currentTime()
    }

    open fun notifyAnalysisFinished(files: Int, lines: Int, additionalDescription: String?) {
        val time = PerformanceCounter.currentTime() - analysisStart
        measurements += CodeAnalysisMeasurement(files, lines, TimeUnit.NANOSECONDS.toMillis(time), additionalDescription)
    }

    open fun notifyGenerationStarted() {
        generationStart = PerformanceCounter.currentTime()
    }

    open fun notifyGenerationFinished(lines: Int, files: Int, additionalDescription: String) {
        val time = PerformanceCounter.currentTime() - generationStart
        measurements += CodeGenerationMeasurement(lines, files, TimeUnit.NANOSECONDS.toMillis(time), additionalDescription)
    }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport())
    }

    private fun recordGcTime() {
        if (!isEnabled) return

        ManagementFactory.getGarbageCollectorMXBeans().forEach {
            measurements += GarbageCollectionMeasurement(it.name, it.collectionTime)
        }
    }

    private fun recordJitCompilationTime() {
        if (!isEnabled) return

        val bean = ManagementFactory.getCompilationMXBean() ?: return
        measurements += JitCompilationMeasurement(bean.totalCompilationTime)
    }

    private fun recordInitializationTime() {
        val time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initStartNanos)
        measurements += CompilerInitializationMeasurement(time)
    }

    private fun recordPerfCountersMeasurements() {
        PerformanceCounter.report { s -> measurements += PerformanceCounterMeasurement(s) }
    }

    private fun createPerformanceReport(): ByteArray = buildString {
        appendln("$presentableName performance report")
        measurements.map { it.render() }.sorted().forEach { appendln(it) }
    }.toByteArray()
}
