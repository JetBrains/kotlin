/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.io.File
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Generally, the class is not thread-safe; all functions should be called sequentially phase-by-phase within a specific module
 * to get reliable performance measurements.
 * However, [measureTime] written to be thread-safe because there is no absolute guarantee
 * that external measurements are collected in a single thread.
 */
abstract class PerformanceManager(private val presentableName: String) {
    // The lock object is located not in a companion object because every module has its own instance of the performance manager
    private val counterMeasurementsLock = Any()

    private fun currentTime(): Long = System.nanoTime()

    @Suppress("MemberVisibilityCanBePrivate")
    private val measurements: MutableList<PerformanceMeasurement> = mutableListOf()

    // Initialize the counter measurements in strict order to get rid of difference in the same report
    private val counterMeasurements: MutableMap<KClass<*>, CounterMeasurement> = mutableMapOf(
        FindJavaClassMeasurement::class to FindJavaClassMeasurement(0, 0),
        BinaryClassFromKotlinFileMeasurement::class to BinaryClassFromKotlinFileMeasurement(0, 0),
    )

    var isEnabled: Boolean = false
        protected set
    private var isK2: Boolean = true
    private var initStartNanos = currentTime()
    private var analysisStart: Long = 0
    private var generationStart: Long = 0

    private var startGCData = mutableMapOf<String, GCData>()

    private var irTranslationStart: Long = 0
    private var irLoweringStart: Long = 0
    private var irGenerationStart: Long = 0

    var targetDescription: String? = null
    protected var files: Int? = null
    protected var lines: Int? = null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    fun getMeasurementResults(): List<PerformanceMeasurement> = measurements + counterMeasurements.values

    fun addMeasurementResults(newMeasurements: List<PerformanceMeasurement>) {
        measurements += newMeasurements
    }

    fun clearMeasurementResults() {
        measurements.clear()
    }

    fun enableCollectingPerformanceStatistics(isK2: Boolean) {
        isEnabled = true
        this.isK2 = isK2
        if (!isK2) {
            PerformanceCounter.setTimeCounterEnabled(true)
        }
        ManagementFactory.getGarbageCollectorMXBeans().associateTo(startGCData) { it.name to GCData(it) }
    }

    private fun deltaTime(start: Long): Long = currentTime() - start

    open fun notifyCompilerInitialized() {
        if (!isEnabled) return
        val time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initStartNanos)
        measurements += CompilerInitializationMeasurement(time)
    }

    open fun notifyCompilationFinished() {
        if (!isEnabled) return
        recordGcTime()
        recordJitCompilationTime()
        if (!isK2) {
            recordPerfCountersMeasurements()
        }
    }

    open fun addSourcesStats(files: Int, lines: Int) {
        if (!isEnabled) return
        this.files = this.files?.plus(files) ?: files
        this.lines = this.lines?.plus(lines) ?: lines
    }

    open fun notifyAnalysisStarted() {
        analysisStart = currentTime()
    }

    open fun notifyAnalysisFinished() {
        val time = currentTime() - analysisStart
        measurements += CodeAnalysisMeasurement(lines, TimeUnit.NANOSECONDS.toMillis(time))
    }

    open fun notifyGenerationStarted() {
        generationStart = currentTime()
    }

    open fun notifyGenerationFinished() {
        val time = currentTime() - generationStart
        measurements += CodeGenerationMeasurement(lines, TimeUnit.NANOSECONDS.toMillis(time))
    }

    open fun notifyIRTranslationStarted() {
        irTranslationStart = currentTime()
    }

    open fun notifyIRTranslationFinished() {
        val time = deltaTime(irTranslationStart)
        measurements += IrTranslationMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
        )
    }

    open fun notifyIRLoweringStarted() {
        irLoweringStart = currentTime()
    }

    open fun notifyIRLoweringFinished() {
        val time = deltaTime(irLoweringStart)
        measurements += IrLoweringMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
        )
    }

    open fun notifyIRGenerationStarted() {
        irGenerationStart = currentTime()
    }

    open fun notifyIRGenerationFinished() {
        val time = deltaTime(irGenerationStart)
        measurements += IrGenerationMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
        )
    }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport().toByteArray())
    }

    fun createPerformanceReport(): String = buildString {
        append("$presentableName performance report\n")
        getMeasurementResults().map { it.render() }.sorted().forEach { append("$it\n") }
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

    private fun recordPerfCountersMeasurements() {
        PerformanceCounter.report { s -> measurements += PerformanceCounterMeasurement(s) }
    }

    internal fun <T> measureTime(measurementClass: KClass<*>, block: () -> T): T {
        if (!isEnabled) block()

        val startTime = currentTime()
        try {
            return block()
        } finally {
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(currentTime() - startTime)
            synchronized(counterMeasurementsLock) {
                val currentMeasurement = counterMeasurements[measurementClass]
                    ?: error("No counter measurement initialized for $measurementClass")
                val newCount = currentMeasurement.count + 1
                val newElapsed = currentMeasurement.milliseconds + elapsedMillis
                val newMeasurement = when (measurementClass) {
                    FindJavaClassMeasurement::class -> FindJavaClassMeasurement(newCount, newElapsed)
                    BinaryClassFromKotlinFileMeasurement::class -> BinaryClassFromKotlinFileMeasurement(newCount, newElapsed)
                    else -> error("The measurement for $measurementClass is not supported")
                }
                counterMeasurements[measurementClass] = newMeasurement
            }
        }
    }

    private data class GCData(val name: String, val collectionTime: Long, val collectionCount: Long) {
        constructor(bean: GarbageCollectorMXBean) : this(bean.name, bean.collectionTime, bean.collectionCount)
    }

    fun renderCompilerPerformance(): String {
        val relevantMeasurements = getMeasurementResults().filter {
            it is CompilerInitializationMeasurement ||
                    it is CodeAnalysisMeasurement ||
                    it is CodeGenerationMeasurement ||
                    it is PerformanceCounterMeasurement ||
                    it is FindJavaClassMeasurement ||
                    it is BinaryClassFromKotlinFileMeasurement
        }

        return "Compiler perf stats:\n" + relevantMeasurements.joinToString(separator = "\n") { "  ${it.render()}" }
    }
}

fun <T> PerformanceManager?.tryMeasureTime(measurementClass: KClass<*>, block: () -> T): T {
    return if (this == null) return block() else measureTime(measurementClass, block)
}