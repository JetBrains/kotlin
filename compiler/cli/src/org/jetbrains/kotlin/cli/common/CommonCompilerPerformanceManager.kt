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
    private var irLoweringStart: Long = 0
    private var irGenerationStart: Long = 0

    private var targetDescription: String? = null
    protected var files: Int? = null
    protected var lines: Int? = null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    fun getMeasurementResults(): List<PerformanceMeasurement> = measurements

    fun enableCollectingPerformanceStatistics() {
        isEnabled = true
        PerformanceCounter.setTimeCounterEnabled(true)
        ManagementFactory.getGarbageCollectorMXBeans().associateTo(startGCData) { it.name to GCData(it) }
    }

    private fun deltaTime(start: Long): Long = PerformanceCounter.currentTime() - start

    open fun notifyCompilerInitialized(files: Int, lines: Int, targetDescription: String) {
        if (!isEnabled) return
        recordInitializationTime()

        this.files = files
        this.lines = lines
        this.targetDescription = targetDescription
    }

    open fun notifyCompilationFinished() {
        if (!isEnabled) return
        recordGcTime()
        recordJitCompilationTime()
        recordPerfCountersMeasurements()
    }

    open fun addSourcesStats(files: Int, lines: Int) {
        if (!isEnabled) return
        this.files = this.files?.plus(files) ?: files
        this.lines = this.lines?.plus(lines) ?: lines
    }

    open fun notifyAnalysisStarted() {
        analysisStart = PerformanceCounter.currentTime()
    }

    open fun notifyAnalysisFinished() {
        val time = PerformanceCounter.currentTime() - analysisStart
        measurements += CodeAnalysisMeasurement(lines, TimeUnit.NANOSECONDS.toMillis(time))
    }

    open fun notifyGenerationStarted() {
        generationStart = PerformanceCounter.currentTime()
    }

    open fun notifyGenerationFinished() {
        val time = PerformanceCounter.currentTime() - generationStart
        measurements += CodeGenerationMeasurement(lines, TimeUnit.NANOSECONDS.toMillis(time))
    }

    open fun notifyIRTranslationStarted() {
        irTranslationStart = PerformanceCounter.currentTime()
    }

    open fun notifyIRTranslationFinished() {
        val time = deltaTime(irTranslationStart)
        measurements += IRMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
            IRMeasurement.Kind.TRANSLATION
        )
    }

    open fun notifyIRLoweringStarted() {
        irLoweringStart = PerformanceCounter.currentTime()
    }

    open fun notifyIRLoweringFinished() {
        val time = deltaTime(irLoweringStart)
        measurements += IRMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
            IRMeasurement.Kind.LOWERING
        )
    }

    open fun notifyIRGenerationStarted() {
        irGenerationStart = PerformanceCounter.currentTime()
    }

    open fun notifyIRGenerationFinished() {
        val time = deltaTime(irGenerationStart)
        measurements += IRMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
            IRMeasurement.Kind.GENERATION
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
        append("$presentableName performance report\n")
        measurements.map { it.render() }.sorted().forEach { append("$it\n") }
    }.toByteArray()

    open fun notifyRepeat(total: Int, number: Int) {}

    private data class GCData(val name: String, val collectionTime: Long, val collectionCount: Long) {
        constructor(bean: GarbageCollectorMXBean) : this(bean.name, bean.collectionTime, bean.collectionCount)
    }

    fun renderCompilerPerformance(): String {
        val relevantMeasurements = getMeasurementResults().filter {
            it is CompilerInitializationMeasurement || it is CodeAnalysisMeasurement || it is CodeGenerationMeasurement || it is PerformanceCounterMeasurement
        }

        return "Compiler perf stats:\n" + relevantMeasurements.joinToString(separator = "\n") { "  ${it.render()}" }
    }
}
