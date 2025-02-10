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

    private var currentPhaseType: PhaseMeasurementType = PhaseMeasurementType.Initialization
    private var phaseStartNanos: Long? = currentTime()

    private val phaseMeasurements: MutableMap<PhaseMeasurementType, Long> = mutableMapOf()

    // Initialize the counter measurements in strict order to get rid of difference in the same report
    private val counterMeasurements: MutableMap<KClass<*>, CounterMeasurement> = mutableMapOf(
        FindJavaClassMeasurement::class to FindJavaClassMeasurement(0, 0),
        BinaryClassFromKotlinFileMeasurement::class to BinaryClassFromKotlinFileMeasurement(0, 0),
    )

    private var gcMeasurements: MutableList<GarbageCollectionMeasurement> = mutableListOf()
    private var jitMeasurement: JitCompilationMeasurement? = null

    private val extraMeasurements: MutableList<PerformanceMeasurement> = mutableListOf()

    var isEnabled: Boolean = false
        protected set
    private var isK2: Boolean = true
    private var startGCData = mutableMapOf<String, GCData>()

    var targetDescription: String? = null
    var files: Int? = null
        protected set
    var lines: Int? = null
        protected set
    private var finalized: Boolean = false

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    val measurements: List<PerformanceMeasurement> by lazy {
        finalized = true
        buildList {
            for ((phaseType, measurement) in phaseMeasurements) {
                add(
                    when (phaseType) {
                        PhaseMeasurementType.Initialization -> CompilerInitializationMeasurement(measurement)
                        PhaseMeasurementType.Analysis -> CodeAnalysisMeasurement(lines, measurement)
                        PhaseMeasurementType.IrGeneration -> IrGenerationMeasurement(lines, measurement)
                        PhaseMeasurementType.IrLowering -> IrLoweringMeasurement(lines, measurement)
                        PhaseMeasurementType.BackendGeneration -> BackendGenerationMeasurement(lines, measurement)
                    }
                )
            }
            add(counterMeasurements.getValue(FindJavaClassMeasurement::class))
            add(counterMeasurements.getValue(BinaryClassFromKotlinFileMeasurement::class))
            addAll(gcMeasurements)
            addIfNotNull(jitMeasurement)
            addAll(extraMeasurements)
        }
    }

    fun getLoweringAndBackendTimeMs(): Long = (measurements.filterIsInstance<IrLoweringMeasurement>().sumOf { it.milliseconds }) +
            (measurements.filterIsInstance<BackendGenerationMeasurement>().sumOf { it.milliseconds })

    fun addMeasurementResults(otherPerformanceManager: PerformanceManager) {
        ensureNotFinalized()
        extraMeasurements += otherPerformanceManager.extraMeasurements
    }

    fun enableCollectingPerformanceStatistics(isK2: Boolean) {
        isEnabled = true
        this.isK2 = isK2
        if (!isK2) {
            PerformanceCounter.setTimeCounterEnabled(true)
        }
        ManagementFactory.getGarbageCollectorMXBeans().associateTo(startGCData) { it.name to GCData(it) }
    }

    open fun addSourcesStats(files: Int, lines: Int) {
        if (!isEnabled) return

        ensureNotFinalized()

        this.files = this.files?.plus(files) ?: files
        this.lines = this.lines?.plus(lines) ?: lines
    }

    open fun notifyCompilerInitialized() {
        notifyPhaseFinished(PhaseMeasurementType.Initialization)
    }

    open fun notifyAnalysisStarted() {
        notifyPhaseStarted(PhaseMeasurementType.Analysis)
    }

    open fun notifyAnalysisFinished() {
        notifyPhaseFinished(PhaseMeasurementType.Analysis)
    }

    open fun notifyIRGenerationStarted() {
        notifyPhaseStarted(PhaseMeasurementType.IrGeneration)
    }

    open fun notifyIRGenerationFinished() {
        notifyPhaseFinished(PhaseMeasurementType.IrGeneration)
    }

    open fun notifyIRLoweringStarted() {
        notifyPhaseStarted(PhaseMeasurementType.IrLowering)
    }

    open fun notifyIRLoweringFinished() {
        notifyPhaseFinished(PhaseMeasurementType.IrLowering)
    }

    open fun notifyBackendGenerationStarted() {
        notifyPhaseStarted(PhaseMeasurementType.BackendGeneration)
    }

    open fun notifyBackendGenerationFinished() {
        notifyPhaseFinished(PhaseMeasurementType.BackendGeneration)
    }

    private fun notifyPhaseStarted(newPhaseType: PhaseMeasurementType) {
        if (!isEnabled) return

        assert(phaseStartNanos == null) { "The measurement for phase $currentPhaseType must have been finished before starting $newPhaseType" }
        assert(newPhaseType > currentPhaseType) { "The measurement for phase $newPhaseType must be performed before $currentPhaseType" }

        phaseStartNanos = currentTime()
        currentPhaseType = newPhaseType
    }

    private fun notifyPhaseFinished(phaseType: PhaseMeasurementType) {
        if (!isEnabled) return

        ensureNotFinalized()

        assert(phaseType == currentPhaseType) { "The measurement for phase $currentPhaseType must be finished before finishing $phaseType" }
        assert(!phaseMeasurements.containsKey(phaseType)) { "The measurement for phase $phaseType is already performed" }

        phaseMeasurements[phaseType] = phaseStartNanos?.let { TimeUnit.NANOSECONDS.toMillis(currentTime() - it) }
            ?: error("The measurement for phase $phaseType must have been started before finishing")
        phaseStartNanos = null
    }

    open fun notifyCompilationFinished() {
        if (!isEnabled) return

        ensureNotFinalized()

        recordGcTime()
        recordJitCompilationTime()
        if (!isK2) {
            recordPerfCountersMeasurements()
        }
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

    private fun recordPerfCountersMeasurements() {
        PerformanceCounter.report { s -> extraMeasurements += PerformanceCounterMeasurement(s) }
    }

    internal fun <T> measureTime(measurementClass: KClass<*>, block: () -> T): T {
        if (!isEnabled) block()

        ensureNotFinalized()

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

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport().toByteArray())
    }

    fun createPerformanceReport(): String = buildString {
        append("$presentableName performance report\n")
        measurements.map { it.render() }.forEach { append("$it\n") }
    }

    private fun ensureNotFinalized() {
        assert(!finalized) { "Cannot add performance measurements because it's already finalized" }
    }

    private data class GCData(val name: String, val collectionTime: Long, val collectionCount: Long) {
        constructor(bean: GarbageCollectorMXBean) : this(bean.name, bean.collectionTime, bean.collectionCount)
    }

    fun renderCompilerPerformance(): String {
        return "Compiler perf stats:\n" + measurements.joinToString(separator = "\n") { "  ${it.render()}" }
    }
}

fun <T> PerformanceManager?.tryMeasureTime(measurementClass: KClass<*>, block: () -> T): T {
    return if (this == null) return block() else measureTime(measurementClass, block)
}