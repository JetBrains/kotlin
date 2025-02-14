/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.lang.management.CompilationMXBean
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.util.SortedMap
import kotlin.collections.set

/**
 * The class is not thread-safe; all functions should be called sequentially phase-by-phase within a specific module
 * to get reliable performance measurements.
 */
abstract class PerformanceManager(val targetPlatform: TargetPlatform, val presentableName: String) {
    private lateinit var thread: Thread

    init {
        initializeCurrentThread()
    }

    private fun currentTime(): Time = Time(System.nanoTime())

    private var currentPhaseType: PhaseType = PhaseType.Initialization
    private var phaseStartTime: Time? = currentTime()
    private var compilationMXBean: CompilationMXBean? = null
    private var jitStartTime: Long? = null
    private var garbageCollectorMXBeans: List<GarbageCollectorMXBean> = emptyList()

    private val phaseMeasurements: SortedMap<PhaseType, Time> = sortedMapOf()
    private val phaseSideMeasurements: SortedMap<PhaseType, SortedMap<PhaseSideType, SidePerformanceMeasurement>> = sortedMapOf()
    private var gcMeasurements: SortedMap<String, GarbageCollectionMeasurement> = sortedMapOf()
    private var jitMeasurement: JitCompilationMeasurement? = null
    private val extraMeasurements: MutableList<PerformanceMeasurement> = mutableListOf()

    var isEnabled: Boolean = false
        protected set
    var isK2: Boolean = true
        private set

    var targetDescription: String? = null
    var files: Int = 0
        protected set
    var lines: Int = 0
        protected set
    var isFinalized: Boolean = false
        private set
    val isMeasuring: Boolean
        get() = phaseStartTime != null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    fun initializeCurrentThread() {
        thread = Thread.currentThread()
    }

    fun getLoweringAndBackendTimeMs(): Long = (measurements.filterIsInstance<IrLoweringMeasurement>().sumOf { it.time.millis }) +
            (measurements.filterIsInstance<BackendMeasurement>().sumOf { it.time.millis })

    val measurements: List<PerformanceMeasurement> by lazy {
        isFinalized = true
        buildList {
            for ((phaseType, time) in phaseMeasurements) {
                var refinedTime = time

                // Subtract side measurement times to get a more refined result
                phaseSideMeasurements[phaseType]?.values?.forEach {
                    refinedTime -= it.time
                }
                add(
                    when (phaseType) {
                        PhaseType.Initialization -> CompilerInitializationMeasurement(refinedTime)
                        PhaseType.Analysis -> CodeAnalysisMeasurement(refinedTime)
                        PhaseType.TranslationToIr -> TranslationToIrMeasurement(refinedTime)
                        PhaseType.IrLowering -> IrLoweringMeasurement(refinedTime)
                        PhaseType.Backend -> BackendMeasurement(refinedTime)
                    }
                )
            }

            val aggregatePhaseSideMeasurements = sortedMapOf<PhaseSideType, SidePerformanceMeasurement>()
            for (sideMeasurements in phaseSideMeasurements.values) {
                for ((sideMeasurementType, sideMeasurement) in sideMeasurements) {
                    aggregatePhaseSideMeasurements.addSideMeasurement(sideMeasurementType, sideMeasurement)
                }
            }

            addAll(aggregatePhaseSideMeasurements.map { it.value })
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

        for ((phase, otherPhaseMeasurement) in otherPerformanceManager.phaseMeasurements) {
            phaseMeasurements[phase] = (phaseMeasurements[phase] ?: Time.ZERO) + otherPhaseMeasurement
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

    private fun SortedMap<PhaseSideType, SidePerformanceMeasurement>.addSideMeasurement(
        sideMeasurementType: PhaseSideType,
        otherSideMeasurement: SidePerformanceMeasurement,
    ) {
        val existingSideMeasurement = this[sideMeasurementType]
        this[sideMeasurementType] =
            sideMeasurementType.createSideMeasurement(
                (existingSideMeasurement?.count ?: 0) + otherSideMeasurement.count,
                (existingSideMeasurement?.time ?: Time.ZERO) + otherSideMeasurement.time
            )
    }

    fun enableCollectingPerformanceStatistics(isK2: Boolean) {
        isEnabled = true
        this.isK2 = isK2
        if (!isK2) {
            PerformanceCounter.setTimeCounterEnabled(true)
        }
        compilationMXBean = ManagementFactory.getCompilationMXBean()
        jitStartTime = compilationMXBean?.totalCompilationTime
        garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans()
        garbageCollectorMXBeans.associateTo(gcMeasurements) {
            it.name to GarbageCollectionMeasurement(it.name, it.collectionTime, it.collectionCount)
        }
    }

    open fun addSourcesStats(files: Int, lines: Int) {
        if (!isEnabled) return

        ensureNotFinalizedAndSameThread()

        this.files = this.files + files
        this.lines = this.lines + lines
    }

    fun notifyPhaseStarted(newPhaseType: PhaseType) {
        // Here should be the following check: `if (!isEnabled) return`.
        // However, currently it's dropped to keep compatibility with build systems that don't call `enableCollectingPerformanceStatistics`,
        // but take some measurements from this manager (old behavior is preserved).

        assert(phaseStartTime == null) { "The measurement for phase $currentPhaseType must have been finished before starting $newPhaseType" }

        // Ideally, all phases always should be executed sequentially.
        // However, some pipelines are written in a way where `BackendGeneration` executed before `Analysis` or `IrLowering` (Web).
        // TODO: KT-75227 Consider using multiple `PerformanceManager` for measuring times per each unit
        // or fixing a time measurement bug where `BackendGeneration` is executed before `Analysis` or `IrLowering`
        if (!targetPlatform.isJs()) {
            assert(newPhaseType >= currentPhaseType) { "The measurement for phase $newPhaseType must be performed before $currentPhaseType" }
        }

        phaseStartTime = currentTime()
        currentPhaseType = newPhaseType
    }

    fun notifyPhaseFinished(phaseType: PhaseType) {
        // Here should be the following check: `if (!isEnabled) return`.
        // However, currently it's dropped to keep compatibility with build systems that don't call `enableCollectingPerformanceStatistics`
        // but take some measurements from this manager (old behavior is preserved).

        ensureNotFinalizedAndSameThread()

        assert(phaseStartTime != null) { "The measurement for phase $phaseType hasn't been started or already finished" }
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

        for (garbageCollectorMXBean in garbageCollectorMXBeans) {
            val startGcMeasurement = gcMeasurements.getValue(garbageCollectorMXBean.name)
            gcMeasurements[garbageCollectorMXBean.name] = GarbageCollectionMeasurement(
                garbageCollectorMXBean.name,
                garbageCollectorMXBean.collectionTime - startGcMeasurement.milliseconds,
                garbageCollectorMXBean.collectionCount - startGcMeasurement.count,
            )
        }

        if (compilationMXBean != null && jitStartTime != null) {
            jitMeasurement = JitCompilationMeasurement(compilationMXBean!!.totalCompilationTime - jitStartTime!!)
        }

        if (!isK2) {
            @OptIn(DeprecatedPerformanceDeclaration::class)
            PerformanceCounter.report { s -> extraMeasurements += PerformanceCounterMeasurement(s) }
        }
    }

    @PotentiallyIncorrectPhaseTimeMeasurement
    fun notifyCurrentPhaseFinishedIfNeeded() {
        if (phaseStartTime != null) {
            finishPhase(currentPhaseType)
        }
    }

    private fun finishPhase(phaseType: PhaseType) {
        if (phaseType != currentPhaseType) { // It's allowed to measure the same phase multiple times (although it's better to avoid that)
            assert(!phaseMeasurements.containsKey(phaseType)) { "The measurement for phase $phaseType is already performed" }
        }
        phaseMeasurements[phaseType] = (phaseMeasurements[phaseType] ?: Time.ZERO) + (currentTime() - phaseStartTime!!)
        phaseStartTime = null
    }

    internal fun <T> measureTime(phaseSideType: PhaseSideType, block: () -> T): T {
        ensureNotFinalizedAndSameThread()

        val startTime = currentTime()
        try {
            return block()
        } finally {
            val elapsedTime = currentTime() - startTime

            assert(phaseStartTime != null) { "No phase started for $phaseSideType side measurement" }

            val sideMeasurements = phaseSideMeasurements.getOrPut(currentPhaseType) { sortedMapOf() }
            sideMeasurements.addSideMeasurement(phaseSideType, phaseSideType.createSideMeasurement(1, elapsedTime))
        }
    }

    private fun PhaseSideType.createSideMeasurement(newCount: Int, newElapsed: Time): SidePerformanceMeasurement =
        when (this) {
            PhaseSideType.FindJavaClass -> FindJavaClassMeasurement(newCount, newElapsed)
            PhaseSideType.BinaryClassFromKotlinFile -> BinaryClassFromKotlinFileMeasurement(newCount, newElapsed)
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
}

class PerformanceManagerImpl(targetPlatform: TargetPlatform, presentableName: String) : PerformanceManager(targetPlatform, presentableName) {
    companion object {
        /**
         * Useful for measuring time when a pipeline is split on multiple parallel steps (in multithread mode or not)
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

fun <T> PerformanceManager?.tryMeasureSideTime(phaseSideType: PhaseSideType, block: () -> T): T {
    return if (this == null) return block() else measureTime(phaseSideType, block)
}

inline fun <T> PerformanceManager?.tryMeasurePhaseTime(phaseType: PhaseType, block: () -> T): T {
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