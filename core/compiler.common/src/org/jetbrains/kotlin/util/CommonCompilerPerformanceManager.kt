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

abstract class CommonCompilerPerformanceManager(private val presentableName: String) {
    companion object {
        val findJavaClassLock = Any()
        val binaryClassFromKotlinFileLock = Any()

        fun currentTime(): Long = System.nanoTime()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    private val measurements: MutableList<PerformanceMeasurement> = mutableListOf()
    private var findJavaClassMeasurement: FindJavaClassMeasurement = FindJavaClassMeasurement(0, 0)
    private var binaryClassFromKotlinFileMeasurement: BinaryClassFromKotlinFileMeasurement = BinaryClassFromKotlinFileMeasurement(0, 0)
    var isEnabled: Boolean = false
        protected set
    private var initStartNanos = currentTime()
    private var analysisStart: Long = 0

    private var startGCData = mutableMapOf<String, GCData>()

    private var irTranslationStart: Long = 0
    private var irLoweringStart: Long = 0
    private var backendOrMetadataGenerationStart: Long = 0

    private var targetDescription: String? = null
    protected var files: Int? = null
    protected var lines: Int? = null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    fun getMeasurementResults(): List<PerformanceMeasurement> = measurements + findJavaClassMeasurement + binaryClassFromKotlinFileMeasurement

    fun enableCollectingPerformanceStatistics() {
        isEnabled = true
        ManagementFactory.getGarbageCollectorMXBeans().associateTo(startGCData) { it.name to GCData(it) }
    }

    private fun deltaTime(start: Long): Long = currentTime() - start

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

    open fun notifyBackendOrMetadataGenerationStarted() {
        backendOrMetadataGenerationStart = currentTime()
    }

    open fun notifyBackendOrMetadataGenerationFinished() {
        val time = deltaTime(backendOrMetadataGenerationStart)
        measurements += BackendOrMetadataGenerationMeasurement(
            lines,
            TimeUnit.NANOSECONDS.toMillis(time),
        )
    }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport().toByteArray())
    }

    fun createPerformanceReport(): String = buildString {
        append("$presentableName performance report\n")
        measurements.map { it.render() }.sorted().forEach { append("$it\n") }
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

    internal fun <T> measureTime(measurementClass: KClass<*>, block: () -> T): T {
        if (!isEnabled) block()

        val startTime = currentTime()
        try {
            return block()
        } finally {
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(currentTime() - startTime)
            when (measurementClass) {
                FindJavaClassMeasurement::class -> {
                    synchronized(findJavaClassLock) {
                        findJavaClassMeasurement =
                            FindJavaClassMeasurement(
                                findJavaClassMeasurement.count + 1,
                                findJavaClassMeasurement.milliseconds + elapsedMillis
                            )
                    }
                }
                BinaryClassFromKotlinFileMeasurement::class -> {
                    synchronized(binaryClassFromKotlinFileLock) {
                        binaryClassFromKotlinFileMeasurement = BinaryClassFromKotlinFileMeasurement(
                            binaryClassFromKotlinFileMeasurement.count + 1,
                            binaryClassFromKotlinFileMeasurement.milliseconds + elapsedMillis
                        )
                    }
                }
                else -> {
                    error("The measurement for $measurementClass is not supposed")
                }
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
                    it is IrTranslationMeasurement ||
                    it is IrLoweringMeasurement ||
                    it is BackendOrMetadataGenerationMeasurement ||
                    it is FindJavaClassMeasurement ||
                    it is BinaryClassFromKotlinFileMeasurement
        }

        return "Compiler perf stats:\n" + relevantMeasurements.joinToString(separator = "\n") { "  ${it.render()}" }
    }
}

fun <T> CommonCompilerPerformanceManager?.tryMeasureTime(measurementClass: KClass<*>, block: () -> T): T {
    return if (this == null) return block() else measureTime(measurementClass, block)
}