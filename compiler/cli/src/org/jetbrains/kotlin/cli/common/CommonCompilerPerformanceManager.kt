/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.util.PerformanceCounter
import java.io.File
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

abstract class CommonCompilerPerformanceManager(private val presentableName: String) {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val measurements: MutableList<PerformanceMeasurement> = mutableListOf()
    protected var isEnabled: Boolean = false
    private var initStartNanos = PerformanceCounter.currentTime()
    private var analysisStart: Long = 0
    private var generationStart: Long = 0

    private var startGCData = mutableMapOf<String, GCData>()

    private var irTranslationStart: Long = 0
    private var irGenerationStart: Long = 0

    fun getMeasurementResults(): List<PerformanceMeasurement> = measurements

    fun enableCollectingPerformanceStatistics() {
        isEnabled = true
        PerformanceCounter.setTimeCounterEnabled(true)
        ManagementFactory.getGarbageCollectorMXBeans().associateTo(startGCData) { it.name to GCData(it) }
    }

    private fun deltaTime(start: Long): Long = PerformanceCounter.currentTime() - start

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

    open fun notifyGenerationFinished(files: Int, lines: Int, additionalDescription: String) {
        val time = PerformanceCounter.currentTime() - generationStart
        measurements += CodeGenerationMeasurement(files, lines, TimeUnit.NANOSECONDS.toMillis(time), additionalDescription)
    }

    open fun notifyIRTranslationStarted() {
        irTranslationStart = PerformanceCounter.currentTime()
    }

    open fun notifyIRTranslationFinished(files: Int, lines: Int, additionalDescription: String?) {
        val time = deltaTime(irTranslationStart)
        measurements += IRMeasurement(
            files,
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
            additionalDescription,
            IRMeasurement.Kind.Translation
        )
    }

    open fun notifyIRGenerationStarted() {
        irGenerationStart = PerformanceCounter.currentTime()
    }

    open fun notifyIRGenerationFinished(files: Int, lines: Int, additionalDescription: String) {
        val time = deltaTime(irGenerationStart)
        measurements += IRMeasurement(
            files,
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
            additionalDescription,
            IRMeasurement.Kind.Generation
        )
    }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport())
    }

    private fun recordGcTime() {
        if (!isEnabled) return

        ManagementFactory.getGarbageCollectorMXBeans().forEach {
            val startCounts = startGCData[it.name]
            val startCollectionTime = startCounts?.collectionTime ?: 0
            val startCollectionCount = startCounts?.collectionCount ?: 0
            measurements += GarbageCollectionMeasurement(
                it.name,
                it.collectionTime - startCollectionTime,
                it.collectionCount - startCollectionCount
            )
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

    open fun notifyRepeat(total: Int, number: Int) {}

    private data class GCData(val name: String, val collectionTime: Long, val collectionCount: Long) {
        constructor(bean: GarbageCollectorMXBean) : this(bean.name, bean.collectionTime, bean.collectionCount)
    }
}
