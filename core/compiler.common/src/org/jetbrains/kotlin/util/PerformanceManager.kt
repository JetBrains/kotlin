/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.io.File
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.util.SortedMap
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

    private val phaseMeasurementsMs: SortedMap<PhaseMeasurementType, Long> = sortedMapOf()

    // Initialize the counter measurements in strict order to get rid of difference in the same report
    private val counterMeasurements: MutableMap<KClass<*>, CounterMeasurement> = mutableMapOf(
        FindJavaClassMeasurement::class to FindJavaClassMeasurement(0, 0),
        BinaryClassFromKotlinFileMeasurement::class to BinaryClassFromKotlinFileMeasurement(0, 0),
    )

    private var gcMeasurements: SortedMap<String, GarbageCollectionMeasurement> = sortedMapOf()
    private var jitMeasurement: JitCompilationMeasurement? = null

    private val extraMeasurements: MutableList<PerformanceMeasurement> = mutableListOf()

    var isEnabled: Boolean = false
        protected set
    var isK2: Boolean = true
        private set
    private var startGCData = mutableMapOf<String, GCData>()

    var targetDescription: String? = null
    var files: Int = 0
        protected set
    var lines: Int = 0
        protected set
    var isFinalized: Boolean = false
        private set
    val isMeasuring: Boolean
        get() = phaseStartNanos != null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    val measurements: List<PerformanceMeasurement> by lazy {
        isFinalized = true
        buildList { forEachMeasurement { add(it) } }
    }

    fun getLoweringAndBackendTimeMs(): Long = (measurements.filterIsInstance<IrLoweringMeasurement>().sumOf { it.milliseconds }) +
            (measurements.filterIsInstance<BackendGenerationMeasurement>().sumOf { it.milliseconds })

    fun addMeasurementResults(otherPerformanceManager: PerformanceManager?) {
        ensureNotFinalized()

        if (otherPerformanceManager == null) return

        files += otherPerformanceManager.files
        lines += otherPerformanceManager.lines

        otherPerformanceManager.forEachMeasurement {
            when (it) {
                is PhasePerformanceMeasurement -> {
                    phaseMeasurementsMs[it.phase] = it.milliseconds + (phaseMeasurementsMs[it.phase] ?: 0)
                }
                is CounterMeasurement -> {
                    val existingMeasurement = counterMeasurements.getValue(it::class)
                    val newCount = it.count + existingMeasurement.count
                    val newMillis = it.milliseconds + existingMeasurement.milliseconds
                    counterMeasurements[it::class] = when (it) {
                        is FindJavaClassMeasurement -> {
                            FindJavaClassMeasurement(newCount, newMillis)
                        }
                        is BinaryClassFromKotlinFileMeasurement -> {
                            BinaryClassFromKotlinFileMeasurement(newCount, newMillis)
                        }
                    }
                }
                is GarbageCollectionMeasurement -> {
                    val existingGcMeasurement = gcMeasurements[it.garbageCollectionKind]
                    gcMeasurements[it.garbageCollectionKind] = GarbageCollectionMeasurement(
                        it.garbageCollectionKind,
                        it.milliseconds + (existingGcMeasurement?.milliseconds ?: 0),
                        it.count + (existingGcMeasurement?.count ?: 0)
                    )
                }
                is JitCompilationMeasurement -> {
                    jitMeasurement = JitCompilationMeasurement(it.milliseconds + (jitMeasurement?.milliseconds ?: 0))
                }
                else -> {
                    extraMeasurements += it
                }
            }
        }
    }

    private fun forEachMeasurement(onMeasurement: (PerformanceMeasurement) -> Unit) {
        for ((phaseType, measurement) in phaseMeasurementsMs) {
            onMeasurement(
                when (phaseType) {
                    PhaseMeasurementType.Initialization -> CompilerInitializationMeasurement(measurement)
                    PhaseMeasurementType.Analysis -> CodeAnalysisMeasurement(measurement)
                    PhaseMeasurementType.IrGeneration -> IrGenerationMeasurement(measurement)
                    PhaseMeasurementType.IrLowering -> IrLoweringMeasurement(measurement)
                    PhaseMeasurementType.BackendGeneration -> BackendGenerationMeasurement(measurement)
                }
            )
        }
        onMeasurement(counterMeasurements.getValue(FindJavaClassMeasurement::class))
        onMeasurement(counterMeasurements.getValue(BinaryClassFromKotlinFileMeasurement::class))
        gcMeasurements.values.forEach { onMeasurement(it) }
        jitMeasurement?.let { onMeasurement(it) }
        extraMeasurements.forEach { onMeasurement(it) }
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

        this.files = this.files + files
        this.lines = this.lines + lines
    }

    fun notifyPhaseStarted(newPhaseType: PhaseMeasurementType) {
        if (!isEnabled) return

        assert(phaseStartNanos == null) { "The measurement for phase $currentPhaseType must have been finished before starting $newPhaseType" }
        assert(newPhaseType > currentPhaseType) { "The measurement for phase $newPhaseType must be performed before $currentPhaseType" }

        phaseStartNanos = currentTime()
        currentPhaseType = newPhaseType
    }

    fun notifyPhaseFinished(phaseType: PhaseMeasurementType) {
        if (!isEnabled) return

        ensureNotFinalized()

        assert(phaseStartNanos != null) { "The measurement for phase $phaseType hasn't been started or already finished" }
        finishPhase(phaseType)
    }

    open fun notifyCompilationFinished() {
        if (!isEnabled) return

        ensureNotFinalized()
        isFinalized = true

        // Finish the current phase in case of error and missing the `notifyPhaseFinished` call
        if (phaseStartNanos != null) {
            finishPhase(currentPhaseType)
        }

        recordGcTime()
        recordJitCompilationTime()
        if (!isK2) {
            recordPerfCountersMeasurements()
        }
    }

    private fun finishPhase(phaseType: PhaseMeasurementType) {
        assert(!phaseMeasurementsMs.containsKey(phaseType)) { "The measurement for phase $phaseType is already performed" }
        phaseMeasurementsMs[phaseType] = TimeUnit.NANOSECONDS.toMillis(currentTime() - phaseStartNanos!!)
        phaseStartNanos = null
    }

    private fun recordGcTime() {
        if (!isEnabled) return

        ManagementFactory.getGarbageCollectorMXBeans().forEach {
            val startCounts = startGCData[it.name]
            val startCollectionTime = startCounts?.collectionTime ?: 0
            val startCollectionCount = startCounts?.collectionCount ?: 0
            val existingGcMeasurement = gcMeasurements[it.name]
            gcMeasurements[it.name] = GarbageCollectionMeasurement(
                it.name,
                (existingGcMeasurement?.milliseconds ?: 0) + it.collectionTime - startCollectionTime,
                (existingGcMeasurement?.count ?: 0) + it.collectionCount - startCollectionCount
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
        measurements.map { it.render(lines) }.forEach { append("$it\n") }
    }

    private fun ensureNotFinalized() {
        assert(!isFinalized) { "Cannot add performance measurements because it's already finalized" }
    }

    private data class GCData(val name: String, val collectionTime: Long, val collectionCount: Long) {
        constructor(bean: GarbageCollectorMXBean) : this(bean.name, bean.collectionTime, bean.collectionCount)
    }
}

fun <T> PerformanceManager?.tryMeasureTime(measurementClass: KClass<*>, block: () -> T): T {
    return if (this == null) return block() else measureTime(measurementClass, block)
}

inline fun <T> PerformanceManager?.trackPhase(phase: PhaseMeasurementType, fn: () -> T): T {
    this?.notifyPhaseStarted(phase)
    try {
        return fn()
    } finally {
        this?.notifyPhaseFinished(phase)
    }
}