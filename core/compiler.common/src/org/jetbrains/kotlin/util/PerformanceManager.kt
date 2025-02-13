/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.util.SortedMap
import java.util.concurrent.TimeUnit
import kotlin.collections.set

/**
 * The class is not thread-safe; all functions should be called sequentially phase-by-phase within a specific module
 * to get reliable performance measurements.
 */
abstract class PerformanceManager(val targetPlatform: TargetPlatform, val presentableName: String) {
    private val thread: Thread = Thread.currentThread()

    private fun currentTime(): Long = System.nanoTime()

    private var currentPhaseType: PhaseMeasurementType = PhaseMeasurementType.Initialization
    private var phaseStartNanos: Long? = currentTime()

    private val phaseMeasurementsMs: SortedMap<PhaseMeasurementType, Long> = sortedMapOf()
    private val phaseSideMeasurements: SortedMap<PhaseMeasurementType, SortedMap<PhaseSideMeasurementType, PhaseSidePerformanceMeasurement>> = sortedMapOf()
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

    fun getLoweringAndBackendTimeMs(): Long = (measurements.filterIsInstance<IrLoweringMeasurement>().sumOf { it.milliseconds }) +
            (measurements.filterIsInstance<BackendGenerationMeasurement>().sumOf { it.milliseconds })

    val measurements: List<PerformanceMeasurement> by lazy {
        isFinalized = true
        buildList {
            for ((phaseType, millis) in phaseMeasurementsMs) {
                var refinedMillis = millis

                phaseSideMeasurements[phaseType]?.values?.forEach {
                    refinedMillis -= it.milliseconds
                }

                add(
                    when (phaseType) {
                        PhaseMeasurementType.Initialization -> CompilerInitializationMeasurement(refinedMillis)
                        PhaseMeasurementType.Analysis -> CodeAnalysisMeasurement(refinedMillis)
                        PhaseMeasurementType.IrGeneration -> IrGenerationMeasurement(refinedMillis)
                        PhaseMeasurementType.IrLowering -> IrLoweringMeasurement(refinedMillis)
                        PhaseMeasurementType.BackendGeneration -> BackendGenerationMeasurement(refinedMillis)
                    }
                )
            }

            val aggregatePhaseMeasurements = sortedMapOf<PhaseSideMeasurementType, PhaseSidePerformanceMeasurement>()
            for (sideMeasurements in phaseSideMeasurements.values) {
                for ((sideMeasurementType, sideMeasurement) in sideMeasurements) {
                    aggregatePhaseMeasurements.addSideMeasurement(sideMeasurementType, sideMeasurement)
                }
            }

            addAll(aggregatePhaseMeasurements.map { it.value })
            gcMeasurements.values.forEach { add(it) }
            addIfNotNull(jitMeasurement)
            addAll(extraMeasurements)
        }
    }

    fun addMeasurementResults(otherPerformanceManager: PerformanceManager?) {
        ensureNotFinalizedAndSameThread()

        if (otherPerformanceManager == null) return

        files += otherPerformanceManager.files
        lines += otherPerformanceManager.lines

        for ((phase, otherPhaseMeasurementMs) in otherPerformanceManager.phaseMeasurementsMs) {
            phaseMeasurementsMs[phase] = (phaseMeasurementsMs[phase] ?: 0) + otherPhaseMeasurementMs
        }

        for ((phaseType, otherSideMeasurements) in otherPerformanceManager.phaseSideMeasurements) {
            val sideMeasurements = phaseSideMeasurements.getOrPut(phaseType) { sortedMapOf() }
            for ((sideMeasurementType, otherSideMeasurement) in otherSideMeasurements) {
                sideMeasurements.addSideMeasurement(sideMeasurementType, otherSideMeasurement)
            }
        }

        for ((garbageCollectionKind, otherGcMeasurement) in otherPerformanceManager.gcMeasurements) {
            val existingGcMeasurement = gcMeasurements[garbageCollectionKind]
            gcMeasurements[garbageCollectionKind] = GarbageCollectionMeasurement(
                garbageCollectionKind,
                (existingGcMeasurement?.milliseconds ?: 0) + otherGcMeasurement.milliseconds,
                (existingGcMeasurement?.count ?: 0) + otherGcMeasurement.count,
            )
        }

        if (jitMeasurement != null || otherPerformanceManager.jitMeasurement != null) {
            jitMeasurement =
                JitCompilationMeasurement((jitMeasurement?.milliseconds ?: 0) + (otherPerformanceManager.jitMeasurement?.milliseconds ?: 0))
        }

        extraMeasurements.addAll(otherPerformanceManager.extraMeasurements)
    }

    private fun SortedMap<PhaseSideMeasurementType, PhaseSidePerformanceMeasurement>.addSideMeasurement(
        sideMeasurementType: PhaseSideMeasurementType,
        otherSideMeasurement: PhaseSidePerformanceMeasurement,
    ) {
        val existingSideMeasurement = this[sideMeasurementType]
        this[sideMeasurementType] =
            sideMeasurementType.createSideMeasurement(
                (existingSideMeasurement?.count ?: 0) + otherSideMeasurement.count,
                (existingSideMeasurement?.milliseconds ?: 0) + otherSideMeasurement.milliseconds
            )
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

        ensureNotFinalizedAndSameThread()

        this.files = this.files + files
        this.lines = this.lines + lines
    }

    fun notifyPhaseStarted(newPhaseType: PhaseMeasurementType) {
        // Here should be the following check: `if (!isEnabled) return`.
        // However, currently it's dropped to keep compatibility with build systems that don't call `enableCollectingPerformanceStatistics`,
        // but take some measurements from this manager (old behavior is preserved).

        assert(phaseStartNanos == null) { "The measurement for phase $currentPhaseType must have been finished before starting $newPhaseType" }

        // Ideally, here should be the following check (phases are almost always executed sequentially):
        // assert(newPhaseType >= currentPhaseType) { "The measurement for phase $newPhaseType must be performed before $currentPhaseType" }
        // However, currently some pipelines are written in a way where `BackendGeneration` executed before `IrLowering` (JS).
        // If needed, it can be fixed later, or an option that disables the check for specified pipelines can be added.

        phaseStartNanos = currentTime()
        currentPhaseType = newPhaseType
    }

    fun notifyPhaseFinished(phaseType: PhaseMeasurementType) {
        // Here should be the following check: `if (!isEnabled) return`.
        // However, currently it's dropped to keep compatibility with build systems that don't call `enableCollectingPerformanceStatistics`
        // but take some measurements from this manager (old behavior is preserved).

        ensureNotFinalizedAndSameThread()

        assert(phaseStartNanos != null) { "The measurement for phase $phaseType hasn't been started or already finished" }
        finishPhase(phaseType)
    }

    open fun notifyCompilationFinished() {
        if (!isEnabled) return

        ensureNotFinalizedAndSameThread()
        isFinalized = true

        // Ideally, all phases should be finished explicitly by using `notifyPhaseFinished` call.
        // However, sometimes exceptions are thrown, and it's not always easy to handle them properly.
        // In this case, the current phase measurement is being finished here.
        @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
        notifyCurrentPhaseFinishedIfNeeded()

        recordGcTime()
        recordJitCompilationTime()
        if (!isK2) {
            recordPerfCountersMeasurements()
        }
    }

    @PotentiallyIncorrectPhaseTimeMeasurement
    fun notifyCurrentPhaseFinishedIfNeeded() {
        if (phaseStartNanos != null) {
            finishPhase(currentPhaseType)
        }
    }

    private fun finishPhase(phaseType: PhaseMeasurementType) {
        if (phaseType != currentPhaseType) { // It's allowed to measure the same phase multiple times (although it's better to avoid that)
            assert(!phaseMeasurementsMs.containsKey(phaseType)) { "The measurement for phase $phaseType is already performed" }
        }
        phaseMeasurementsMs[phaseType] =
            (phaseMeasurementsMs[phaseType] ?: 0) + TimeUnit.NANOSECONDS.toMillis(currentTime() - phaseStartNanos!!)
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

    internal fun <T> measureTime(phaseSideMeasurementType: PhaseSideMeasurementType, block: () -> T): T {
        if (!isEnabled) block()

        ensureNotFinalizedAndSameThread()

        val startTime = currentTime()
        try {
            return block()
        } finally {
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(currentTime() - startTime)

            assert(phaseStartNanos != null) { "No phase started for side measurement" }

            val sideMeasurements = phaseSideMeasurements.getOrPut(currentPhaseType) { sortedMapOf() }
            sideMeasurements.addSideMeasurement(phaseSideMeasurementType, phaseSideMeasurementType.createSideMeasurement(1, elapsedMillis))
        }
    }

    private fun PhaseSideMeasurementType.createSideMeasurement(newCount: Int, newElapsed: Long): PhaseSidePerformanceMeasurement =
        when (this) {
            PhaseSideMeasurementType.FindJavaClass -> FindJavaClassMeasurement(newCount, newElapsed)
            PhaseSideMeasurementType.BinaryClassFromKotlinFile -> BinaryClassFromKotlinFileMeasurement(newCount, newElapsed)
        }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport().toByteArray())
    }

    fun createPerformanceReport(): String = buildString {
        append("$presentableName performance report\n")
        measurements.map { it.render(lines) }.forEach { append("$it\n") }
    }

    private fun ensureNotFinalizedAndSameThread() {
        assert(!isFinalized) { "Cannot add performance measurements because it's already finalized" }
        assert(Thread.currentThread() == thread) { "PerformanceManager functions can be run only from the same thread" }
    }

    private data class GCData(val name: String, val collectionTime: Long, val collectionCount: Long) {
        constructor(bean: GarbageCollectorMXBean) : this(bean.name, bean.collectionTime, bean.collectionCount)
    }
}

class PerformanceManagerImpl(targetPlatform: TargetPlatform, presentableName: String) : PerformanceManager(targetPlatform, presentableName) {
    companion object {
        /**
         * Useful for measuring time when a pipeline is split on multiple parallel steps (multithread mode or not)
         */
        fun createAndEnableChildIfNeeded(mainPerformanceManager: PerformanceManager?): PerformanceManagerImpl? {
            return if (mainPerformanceManager != null) {
                PerformanceManagerImpl(mainPerformanceManager.targetPlatform, mainPerformanceManager.presentableName + " (Child)").also {
                    if (mainPerformanceManager.isEnabled) {
                        it.enableCollectingPerformanceStatistics(mainPerformanceManager.isK2)
                    }
                }
            } else {
                null
            }
        }
    }
}

fun <T> PerformanceManager?.tryMeasureSideTime(phaseSideMeasurementType: PhaseSideMeasurementType, block: () -> T): T {
    return if (this == null) return block() else measureTime(phaseSideMeasurementType, block)
}

inline fun <T> PerformanceManager?.tryMeasurePhaseTime(phaseType: PhaseMeasurementType, block: () -> T): T {
    if (this == null) return block()

    try {
        notifyPhaseStarted(phaseType)
        return block()
    } finally {
        notifyPhaseFinished(phaseType)
    }
}

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "All phase performance measurements should be finished explicitly")
annotation class PotentiallyIncorrectPhaseTimeMeasurement