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
abstract class CommonCompilerPerformanceManager(private val presentableName: String) {
    // The lock object is located not in a companion object because every module has its own instance of the performance manager
    private val counterMeasurementsLock = Any()

    @Suppress("MemberVisibilityCanBePrivate")
    private val measurements: MutableList<PerformanceMeasurement> = mutableListOf()

    // Initialize the counter measurements in strict order to get rid of difference in the same report
    private val counterMeasurements: MutableMap<KClass<*>, CounterMeasurement> = mutableMapOf(
        FindJavaClassMeasurement::class to FindJavaClassMeasurement(0, 0),
        BinaryClassFromKotlinFileMeasurement::class to BinaryClassFromKotlinFileMeasurement(0, 0),
    )

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

    fun getMeasurementResults(): List<PerformanceMeasurement> = measurements + counterMeasurements.values

    fun addMeasurementResults(newMeasurements:  List<PerformanceMeasurement>) {
        measurements += newMeasurements
    }

    fun clearMeasurementResults() {
        measurements.clear()
    }

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

    internal fun <T> measureTime(measurementClass: KClass<*>, block: () -> T): T {
        if (!isEnabled) block()

        val startTime = PerformanceCounter.currentTime()
        try {
            return block()
        } finally {
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(PerformanceCounter.currentTime() - startTime)
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

    private fun createPerformanceReport(): ByteArray = buildString {
        append("$presentableName performance report\n")
        getMeasurementResults().map { it.render() }.sorted().forEach { append("$it\n") }
    }.toByteArray()

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

fun <T> CommonCompilerPerformanceManager?.tryMeasureTime(measurementClass: KClass<*>, block: () -> T): T {
    return if (this == null) return block() else measureTime(measurementClass, block)
}