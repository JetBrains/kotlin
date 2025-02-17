/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
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
    private val thread: Thread = Thread.currentThread()

    private fun currentTime(): Time = Time(System.nanoTime())

    private var currentPhaseType: PhaseMeasurementType = PhaseMeasurementType.Initialization
    private var phaseStartTime: Time? = currentTime()
    private var compilationMXBean: CompilationMXBean? = null
    private var jitStartTime: Long? = null
    private var garbageCollectorMXBeans: List<GarbageCollectorMXBean> = emptyList()

    private val phaseMeasurements: SortedMap<PhaseMeasurementType, Time> = sortedMapOf()
    private val phaseSideMeasurements: SortedMap<PhaseSideMeasurementType, SideStats> = sortedMapOf()
    private var gcMeasurements: SortedMap<String, GarbageCollectionStats> = sortedMapOf()
    private var jitTimeMillis: Long? = null
    private val extendedStats: MutableList<String> = mutableListOf()

    var isExtendedStatsEnabled: Boolean = false
        private set
    var isK2: Boolean = true
    var hasErrors: Boolean = false
        private set

    var targetDescription: String? = null
    var files: Int = 0
        private set
    var lines: Int = 0
        private set
    var isFinalized: Boolean = false
        private set
    val isMeasuring: Boolean
        get() = phaseStartTime != null

    fun getTargetInfo(): String =
        "$targetDescription, $files files ($lines lines)"

    val unitStats: UnitStats by lazy {
        isFinalized = true

        var initTime: Time? = null
        var analysisTime: Time? = null
        var irGenerationTime: Time? = null
        var irLoweringTime: Time? = null
        var backendTime: Time? = null

        for ((phaseType, time) in phaseMeasurements) {
            when (phaseType) {
                PhaseMeasurementType.Initialization -> initTime = time
                PhaseMeasurementType.Analysis -> analysisTime = time
                PhaseMeasurementType.IrGeneration -> irGenerationTime = time
                PhaseMeasurementType.IrLowering -> irLoweringTime = time
                PhaseMeasurementType.BackendGeneration -> backendTime = time
            }
        }

        var findJavaClassStats: SideStats? = null
        var findKotlinClassStats: SideStats? = null

        for ((phaseSideType, sideStats) in phaseSideMeasurements) {
            when (phaseSideType) {
                PhaseSideMeasurementType.FindJavaClass -> findJavaClassStats = sideStats
                PhaseSideMeasurementType.BinaryClassFromKotlinFile -> findKotlinClassStats = sideStats
            }
        }

        UnitStats(
            targetDescription,
            targetPlatform.getPlatformEnumValue(),
            isK2,
            hasErrors,
            files,
            lines,
            initTime,
            analysisTime,
            irGenerationTime,
            irLoweringTime,
            backendTime,
            findJavaClassStats,
            findKotlinClassStats,
            gcMeasurements.values.toList(),
            jitTimeMillis,
            extendedStats,
        )
    }

    fun addOtherUnitStats(otherUnitStats: UnitStats?) {
        ensureNotFinalizedAndSameThread()

        if (otherUnitStats == null) return

        assert(targetPlatform.getPlatformEnumValue() == otherUnitStats.platform)
        assert(isK2 == otherUnitStats.isK2)

        addSourcesStats(otherUnitStats.filesCount, otherUnitStats.linesCount)

        otherUnitStats.forEachPhaseMeasurement { phaseType, time ->
            if (time != null) {
                phaseMeasurements[phaseType] = (phaseMeasurements[phaseType] ?: Time.ZERO) + time
            }
        }

        otherUnitStats.forEachPhaseSideMeasurement { phaseSideType, sideStats ->
            if (sideStats != null) {
                phaseSideMeasurements[phaseSideType] = (phaseSideMeasurements[phaseSideType] ?: SideStats.EMPTY) + sideStats
            }
        }

        for (otherGcStats in otherUnitStats.gcStats) {
            val existingGcMeasurement = gcMeasurements[otherGcStats.kind]
            gcMeasurements[otherGcStats.kind] = GarbageCollectionStats(
                otherGcStats.kind,
                (existingGcMeasurement?.millis ?: 0) + otherGcStats.millis,
                (existingGcMeasurement?.count ?: 0) + otherGcStats.count,
            )
        }

        if (jitTimeMillis != null || otherUnitStats.jitTimeMillis != null) {
            jitTimeMillis = (jitTimeMillis ?: 0) + (otherUnitStats.jitTimeMillis ?: 0)
        }

        extendedStats.addAll(otherUnitStats.extendedStats)
    }

    private fun TargetPlatform.getPlatformEnumValue(): PlatformType {
        val firstPlatformName = componentPlatforms.first().platformName
        val platform = when {
            firstPlatformName.contains("JVM") -> PlatformType.JVM
            firstPlatformName.contains("Native") -> PlatformType.Native
            targetPlatform.isJs() -> PlatformType.JS
            targetPlatform.isCommon() -> PlatformType.Common
            else -> error("Unexpected platform $targetPlatform")
        }
        return platform
    }

    fun enableExtendedStats() {
        isExtendedStatsEnabled = true
        if (!isK2) {
            PerformanceCounter.setTimeCounterEnabled(true)
        }
        compilationMXBean = ManagementFactory.getCompilationMXBean()
        jitStartTime = compilationMXBean?.totalCompilationTime
        garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans()
        garbageCollectorMXBeans.associateTo(gcMeasurements) {
            it.name to GarbageCollectionStats(it.name, it.collectionTime, it.collectionCount)
        }
    }

    open fun addSourcesStats(files: Int, lines: Int) {
        ensureNotFinalizedAndSameThread()

        this.files += files
        this.lines += lines
    }

    fun notifyPhaseStarted(newPhaseType: PhaseMeasurementType) {
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

    fun notifyPhaseFinished(phaseType: PhaseMeasurementType) {
        ensureNotFinalizedAndSameThread()

        assert(phaseStartTime != null) { "The measurement for phase $phaseType hasn't been started or already finished" }
        finishPhase(phaseType)
    }

    open fun notifyCompilationFinished() {
        ensureNotFinalizedAndSameThread()
        isFinalized = true

        if (currentPhaseType != PhaseMeasurementType.BackendGeneration || phaseStartTime != null) {
            hasErrors = true
        }

        // Ideally, all phases should be finished explicitly by using `notifyPhaseFinished` call.
        // However, sometimes exceptions are thrown, and it's not always easy to handle them properly.
        // In this case, the current phase measurement is being finished here.
        @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
        notifyCurrentPhaseFinishedIfNeeded()

        if (!isExtendedStatsEnabled) return

        for (garbageCollectorMXBean in garbageCollectorMXBeans) {
            val startGcMeasurement = gcMeasurements.getValue(garbageCollectorMXBean.name)
            gcMeasurements[garbageCollectorMXBean.name] = GarbageCollectionStats(
                garbageCollectorMXBean.name,
                garbageCollectorMXBean.collectionTime - startGcMeasurement.millis,
                garbageCollectorMXBean.collectionCount - startGcMeasurement.count,
            )
        }

        if (compilationMXBean != null && jitStartTime != null) {
            jitTimeMillis = compilationMXBean!!.totalCompilationTime - jitStartTime!!
        }

        if (!isK2) {
            PerformanceCounter.report { s -> extendedStats += s }
        }
    }

    @PotentiallyIncorrectPhaseTimeMeasurement
    fun notifyCurrentPhaseFinishedIfNeeded() {
        if (phaseStartTime != null) {
            finishPhase(currentPhaseType)
        }
    }

    private fun finishPhase(phaseType: PhaseMeasurementType) {
        if (phaseType != currentPhaseType) { // It's allowed to measure the same phase multiple times (although it's better to avoid that)
            assert(!phaseMeasurements.containsKey(phaseType)) { "The measurement for phase $phaseType is already performed" }
        }
        phaseMeasurements[phaseType] = (phaseMeasurements[phaseType] ?: Time.ZERO) + (currentTime() - phaseStartTime!!)
        phaseStartTime = null
    }

    internal fun <T> measureSideTime(phaseSideMeasurementType: PhaseSideMeasurementType, block: () -> T): T {
        ensureNotFinalizedAndSameThread()

        val startTime = currentTime()
        try {
            return block()
        } finally {
            val elapsedTime = currentTime() - startTime

            assert(phaseStartTime != null) { "No phase started for $phaseSideMeasurementType side measurement" }

            // Subtract side measurement time to get a more refined result
            // The time could be negative at the moment but should be normalized after the `notifyPhaseFinished` call.
            phaseMeasurements[currentPhaseType] = phaseMeasurements.getOrDefault(currentPhaseType, Time.ZERO) - elapsedTime
            phaseSideMeasurements[phaseSideMeasurementType] =
                phaseSideMeasurements.getOrDefault(phaseSideMeasurementType, SideStats.EMPTY) + SideStats(1, elapsedTime)
        }
    }

    fun dumpPerformanceReport(destination: File) {
        destination.writeBytes(createPerformanceReport().toByteArray())
    }

    fun createPerformanceReport(): String = buildString {
        append("$presentableName performance report\n")
        unitStats.forEachStringMeasurement { appendLine(it) }
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
                    it.isK2 = mainPerformanceManager.isK2
                    if (mainPerformanceManager.isExtendedStatsEnabled) {
                        it.enableExtendedStats()
                    }
                }
            } else {
                null
            }
        }
    }
}

fun <T> PerformanceManager?.tryMeasureSideTime(phaseSideMeasurementType: PhaseSideMeasurementType, block: () -> T): T {
    return if (this == null) return block() else measureSideTime(phaseSideMeasurementType, block)
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