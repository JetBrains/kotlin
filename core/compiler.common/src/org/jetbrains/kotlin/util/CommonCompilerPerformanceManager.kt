/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.addIfNotNull
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

    var isEnabled: Boolean = false
        protected set

    private var currentPhaseType: PhaseMeasurementType = PhaseMeasurementType.Initialization
    private var phaseStartNanos: Long? = currentTime()

    private val phaseMeasurements: MutableMap<PhaseMeasurementType, Long> = mutableMapOf()
    private var findJavaClassMeasurement: FindJavaClassMeasurement = FindJavaClassMeasurement(0, 0)
    private var binaryClassFromKotlinFileMeasurement: BinaryClassFromKotlinFileMeasurement = BinaryClassFromKotlinFileMeasurement(0, 0)
    private var gcMeasurements: MutableList<GarbageCollectionMeasurement> = mutableListOf()
    private var jitMeasurement: JitCompilationMeasurement? = null

    private var startGCData = mutableMapOf<String, GCData>()

    private var targetDescription: String? = null
    protected var files: Int? = null
    protected var lines: Int? = null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    val measurementResults: List<PerformanceMeasurement> by lazy {
        buildList {
            for ((phaseType, measurement) in phaseMeasurements) {
                add(
                    when (phaseType) {
                        PhaseMeasurementType.Initialization -> CompilerInitializationMeasurement(measurement)
                        PhaseMeasurementType.Analysis -> CodeAnalysisMeasurement(lines, measurement)
                        PhaseMeasurementType.IrTranslation -> IrTranslationMeasurement(lines, measurement)
                        PhaseMeasurementType.IrLowering -> IrLoweringMeasurement(lines, measurement)
                        PhaseMeasurementType.BackendOrMetadataGeneration -> BackendOrMetadataGenerationMeasurement(lines, measurement)
                    }
                )
            }
            add(findJavaClassMeasurement)
            add(binaryClassFromKotlinFileMeasurement)
            addAll(gcMeasurements)
            addIfNotNull(jitMeasurement)
        }
    }

    fun enableCollectingPerformanceStatistics() {
        isEnabled = true
        ManagementFactory.getGarbageCollectorMXBeans().associateTo(startGCData) { it.name to GCData(it) }
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

    open fun notifyCompilerInitialized(files: Int, lines: Int, targetDescription: String) {
        if (!isEnabled) return

        finishPhaseMeasurement(PhaseMeasurementType.Initialization)

        this.files = files
        this.lines = lines
        this.targetDescription = targetDescription
    }

    open fun notifyAnalysisStarted() {
        startPhaseMeasurement(PhaseMeasurementType.Analysis)
    }

    open fun notifyAnalysisFinished() {
        finishPhaseMeasurement(PhaseMeasurementType.Analysis)
    }

    open fun notifyIRTranslationStarted() {
        startPhaseMeasurement(PhaseMeasurementType.IrTranslation)
    }

    open fun notifyIRTranslationFinished() {
        finishPhaseMeasurement(PhaseMeasurementType.IrTranslation)
    }

    open fun notifyIRLoweringStarted() {
        startPhaseMeasurement(PhaseMeasurementType.IrLowering)
    }

    open fun notifyIRLoweringFinished() {
        finishPhaseMeasurement(PhaseMeasurementType.IrLowering)
    }

    open fun notifyBackendOrMetadataGenerationStarted() {
        startPhaseMeasurement(PhaseMeasurementType.BackendOrMetadataGeneration)
    }

    open fun notifyBackendOrMetadataGenerationFinished() {
        finishPhaseMeasurement(PhaseMeasurementType.BackendOrMetadataGeneration)
    }

    private fun startPhaseMeasurement(newPhaseType: PhaseMeasurementType) {
        assert(phaseStartNanos == null) { "The measurement for phase $currentPhaseType must have been finished before starting $newPhaseType" }
        assert(newPhaseType > currentPhaseType) { "The measurement for phase $newPhaseType must be performed before $currentPhaseType" }

        phaseStartNanos = currentTime()
        currentPhaseType = newPhaseType
    }

    private fun finishPhaseMeasurement(phaseType: PhaseMeasurementType) {
        assert(phaseType == currentPhaseType) { "The measurement for phase $currentPhaseType must be finished before finishing $phaseType" }
        assert(!phaseMeasurements.containsKey(phaseType)) { "The measurement for phase $phaseType is already performed" }

        phaseMeasurements[phaseType] = phaseStartNanos?.let { TimeUnit.NANOSECONDS.toMillis(currentTime() - it) }
            ?: error("The measurement for phase $phaseType must have been started before finishing")
        phaseStartNanos = null
    }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport().toByteArray())
    }

    fun createPerformanceReport(): String = buildString {
        append("$presentableName performance report\n")
        measurementResults.map { it.render() }.forEach { append("$it\n") }
    }

    private fun recordGcTime() {
        if (!isEnabled) return

        ManagementFactory.getGarbageCollectorMXBeans().forEach {
            val startCounts = startGCData[it.name]
            val startCollectionTime = startCounts?.collectionTime ?: 0
            val startCollectionCount = startCounts?.collectionCount ?: 0
            gcMeasurements += GarbageCollectionMeasurement(
                it.name,
                it.collectionTime - startCollectionTime,
                it.collectionCount - startCollectionCount
            )
        }
    }

    private fun recordJitCompilationTime() {
        if (!isEnabled) return

        val bean = ManagementFactory.getCompilationMXBean() ?: return
        jitMeasurement = JitCompilationMeasurement(bean.totalCompilationTime)
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
        return "Compiler perf stats:\n" + measurementResults.joinToString(separator = "\n") { "  ${it.render()}" }
    }
}

fun <T> CommonCompilerPerformanceManager?.tryMeasureTime(measurementClass: KClass<*>, block: () -> T): T {
    return if (this == null) return block() else measureTime(measurementClass, block)
}