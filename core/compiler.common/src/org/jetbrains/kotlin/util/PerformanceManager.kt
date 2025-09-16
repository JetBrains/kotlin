/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.stats.MarkdownReportRenderer
import org.jetbrains.kotlin.stats.SingleReportsData
import org.jetbrains.kotlin.stats.StatsCalculator
import java.io.File
import java.lang.management.CompilationMXBean
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.text.SimpleDateFormat
import java.util.*

/**
 * The class is not thread-safe; all functions should be called sequentially phase-by-phase within a specific module
 * to get reliable performance measurements.
 */
abstract class PerformanceManager(val targetPlatform: TargetPlatform, val presentableName: String) {
    companion object {
        private const val DEBUG_MODE: Boolean = false
    }

    private lateinit var thread: Thread
    private lateinit var threadMXBean: ThreadMXBean

    init {
        initializeCurrentThread()
    }

    private fun currentTime(): Time = Time(System.nanoTime(), threadMXBean.currentThreadUserTime, threadMXBean.currentThreadCpuTime)

    private var currentPhaseType: PhaseType = PhaseType.Initialization
    private var phaseStartTime: Time? = currentTime()
    private var compilationMXBean: CompilationMXBean? = null
    private var jitStartTime: Long? = null
    private var garbageCollectorMXBeans: List<GarbageCollectorMXBean> = emptyList()

    private val phaseMeasurements: SortedMap<PhaseType, Time> = sortedMapOf()
    private val phaseSideMeasurements: SortedMap<PhaseSideType, SideStats> = sortedMapOf()
    private var gcMeasurements: SortedMap<String, GarbageCollectionStats> = sortedMapOf()
    private var jitTimeMillis: Long? = null
    private val extendedStats: MutableList<String> = mutableListOf()

    private var currentDynamicPhaseTime: Time? = null
    private var currentDynamicPhase: String? = null
    private val dynamicPhaseMeasurements = LinkedHashMap<Pair<PhaseType, String>, Time>()

    var isExtendedStatsEnabled: Boolean = false
        private set
    var compilerType: CompilerType = CompilerType.K2
    var hasErrors: Boolean = false
        private set

    var targetDescription: String? = null
    var outputKind: String? = null
    var files: Int = 0
        private set
    var lines: Int = 0
        private set
    var isFinalized: Boolean = false
        private set
    val isPhaseMeasuring: Boolean
        get() = phaseStartTime != null

    fun getTargetInfo(): String =
        listOfNotNull(targetDescription, outputKind).joinToString("-") + " $files files ($lines lines)"

    fun initializeCurrentThread() {
        thread = Thread.currentThread()
        threadMXBean = ManagementFactory.getThreadMXBean().also { it.isThreadCpuTimeEnabled = true }
    }

    val unitStats: UnitStats by lazy {
        if (!isFinalized) {
            notifyCompilationFinished()
        }

        var initTime: Time? = null
        var analysisTime: Time? = null
        var translationToIrTime: Time? = null
        var irPreLoweringTime: Time? = null
        var irSerializationTime: Time? = null
        var klibWritingTime: Time? = null
        var irLoweringTime: Time? = null
        var backendTime: Time? = null

        for ((phaseType, time) in phaseMeasurements) {
            when (phaseType) {
                PhaseType.Initialization -> initTime = time
                PhaseType.Analysis -> analysisTime = time
                PhaseType.TranslationToIr -> translationToIrTime = time
                PhaseType.IrPreLowering -> irPreLoweringTime = time
                PhaseType.IrSerialization -> irSerializationTime = time
                PhaseType.KlibWriting -> klibWritingTime = time
                PhaseType.IrLowering -> irLoweringTime = time
                PhaseType.Backend -> backendTime = time
            }
        }

        var findJavaClassStats: SideStats? = null
        var findKotlinClassStats: SideStats? = null

        for ((phaseSideType, sideStats) in phaseSideMeasurements) {
            when (phaseSideType) {
                PhaseSideType.FindJavaClass -> findJavaClassStats = sideStats
                PhaseSideType.BinaryClassFromKotlinFile -> findKotlinClassStats = sideStats
            }
        }

        UnitStats(
            targetDescription,
            outputKind,
            System.currentTimeMillis(),
            targetPlatform.getPlatformEnumValue(),
            compilerType,
            hasErrors,
            files,
            lines,
            initTime,
            analysisTime,
            translationToIrTime,
            irPreLoweringTime,
            irSerializationTime,
            klibWritingTime,
            irLoweringTime,
            backendTime,
            dynamicPhaseMeasurements.map { (key, time) ->
                val (phaseType, name) = key
                DynamicStats(phaseType, name, time)
            },
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

        assertIfDebug(targetPlatform.getPlatformEnumValue() == otherUnitStats.platform)
        compilerType += otherUnitStats.compilerType
        hasErrors = hasErrors || otherUnitStats.hasErrors

        addSourcesStats(otherUnitStats.filesCount, otherUnitStats.linesCount)

        otherUnitStats.forEachPhaseMeasurement { phaseType, time ->
            if (time != null) {
                phaseMeasurements[phaseType] = (phaseMeasurements[phaseType] ?: Time.ZERO) + time
            }
        }

        otherUnitStats.dynamicStats?.forEach { (phaseType, name, time) ->
            dynamicPhaseMeasurements[phaseType to name] = (dynamicPhaseMeasurements[phaseType to name] ?: Time.ZERO) + time
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

        @OptIn(DeprecatedMeasurementForBackCompatibility::class)
        otherUnitStats.extendedStats?.let { extendedStats.addAll(it) }
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
        if (!compilerType.isK2) {
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

    fun notifyDynamicPhaseStarted(name: String) {
        currentDynamicPhaseTime = currentTime()
        currentDynamicPhase = name
    }

    fun notifyDynamicPhaseFinished(name: String, parentPhaseType: PhaseType) {
        assertIfDebug(currentDynamicPhaseTime != null)
        assertIfDebug(currentDynamicPhase == name)

        val localCurrentDynamicPhaseTime = currentDynamicPhaseTime
        assertIfDebug(localCurrentDynamicPhaseTime != null) { "Dynamic measurement $name must have been started before finishing" }
        if (localCurrentDynamicPhaseTime != null) {
            dynamicPhaseMeasurements[parentPhaseType to name] =
                (dynamicPhaseMeasurements[parentPhaseType to name] ?: Time.ZERO) + (currentTime() - localCurrentDynamicPhaseTime)
        }
        currentDynamicPhaseTime = null
    }

    fun notifyPhaseStarted(newPhaseType: PhaseType) {
        assertIfDebug(phaseStartTime == null) { "The measurement for phase $currentPhaseType must have been finished before starting $newPhaseType" }

        // All phases should always be executed sequentially.
        // TODO KT-75227 However, some Web pipelines are written in a way where `BackendGeneration` executed before `Analysis` or `IrLowering`.
        //   Consider using multiple `PerformanceManager` for measuring times per each unit
        //   or fix a time measurement bug where `BackendGeneration` is measured before `Analysis` or `IrLowering`
        if (!targetPlatform.isJs()) {
            assertIfDebug(newPhaseType >= currentPhaseType) { "The measurement for phase $newPhaseType must be performed before $currentPhaseType" }
        }

        phaseStartTime = currentTime()
        currentPhaseType = newPhaseType
    }

    fun notifyPhaseFinished(phaseType: PhaseType) {
        ensureNotFinalizedAndSameThread()

        assertIfDebug(phaseStartTime != null) { "The measurement for phase $phaseType hasn't been started or already finished" }
        finishPhase(phaseType)
    }

    open fun notifyCompilationFinished() {
        ensureNotFinalizedAndSameThread()
        isFinalized = true

        if (currentPhaseType != PhaseType.Backend || phaseStartTime != null) {
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

        val localCompilationMXBean = compilationMXBean
        val localJitStartTime = jitStartTime
        if (localCompilationMXBean != null && localJitStartTime != null) {
            jitTimeMillis = localCompilationMXBean.totalCompilationTime - localJitStartTime
        }

        if (!compilerType.isK2) {
            PerformanceCounter.report { s -> extendedStats += s }
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
            assertIfDebug(!phaseMeasurements.containsKey(phaseType)) { "The measurement for phase $phaseType is already performed" }
        }
        val localPhaseStartTime = phaseStartTime
        assertIfDebug(localPhaseStartTime != null) { "Measurement of $phaseType must have been started before finishing" }
        if (localPhaseStartTime != null) {
            phaseMeasurements[phaseType] = (phaseMeasurements[phaseType] ?: Time.ZERO) + (currentTime() - localPhaseStartTime)
        }
        phaseStartTime = null
    }

    internal fun <T> measureSideTime(phaseSideType: PhaseSideType, block: () -> T): T {
        ensureNotFinalizedAndSameThread()

        val startTime = currentTime()
        try {
            return block()
        } finally {
            val elapsedTime = currentTime() - startTime

            if (isPhaseMeasuring) {
                // Subtract side measurement time to get a more refined result
                // The time could be negative at the moment but should be normalized after the `notifyPhaseFinished` call.
                phaseMeasurements[currentPhaseType] = (phaseMeasurements[currentPhaseType] ?: Time.ZERO) - elapsedTime
            }
            phaseSideMeasurements[phaseSideType] =
                (phaseSideMeasurements[phaseSideType] ?: SideStats.EMPTY) + SideStats(1, elapsedTime)
        }
    }

    fun dumpPerformanceReport(destFileNameOrPlaceholder: String) {
        val refinedFileName: String = if (File(destFileNameOrPlaceholder).isDirectory) {
            // We can't use `Paths.get` because of its absence in earlier Android SDKs
            val separator = if (destFileNameOrPlaceholder.lastOrNull().let { it == null || it == File.separatorChar }) {
                ""
            } else {
                File.separatorChar
            }
            destFileNameOrPlaceholder + separator + generateFileName() + ".json"
        } else {
            val lastSlashIndex = destFileNameOrPlaceholder.indexOfLast { it == File.separatorChar }
            val extensionDotIndex =
                destFileNameOrPlaceholder.indexOf('.', lastSlashIndex).let { if (it == -1) destFileNameOrPlaceholder.length else it }
            // It's ok if `lastSlashIndex` == -1
            val fileNameOrPlaceholder = destFileNameOrPlaceholder.substring(lastSlashIndex + 1, extensionDotIndex)
            if (fileNameOrPlaceholder == "*") {
                val pathString = if (lastSlashIndex != -1) destFileNameOrPlaceholder.take(lastSlashIndex + 1) else ""
                val fileName = generateFileName()
                val extension = destFileNameOrPlaceholder.substring(extensionDotIndex)
                pathString + fileName + extension
            } else {
                destFileNameOrPlaceholder
            }
        }

        val destinationFile = File(refinedFileName)
        val dumpFormat = DumpFormat.entries.firstOrNull { it.extension == destinationFile.extension } ?: DumpFormat.PlainText
        destinationFile.writeBytes(createPerformanceReport(dumpFormat).toByteArray())
    }

    /**
     * Generate a unique name to avoid files overwriting.
     * Use the current date-time stamp with millis precision as a part of the unique name.
     */
    private fun generateFileName(): String {
        return "${unitStats.name}_${dateFormatterForFileName.format(unitStats.timeStampMs)}"
    }

    private val dateFormatterForFileName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    }

    enum class DumpFormat(val extension: String) {
        PlainText("log"),
        Json("json"),
        Markdown("md"),
    }

    fun createPerformanceReport(dumpFormat: DumpFormat): String = when (dumpFormat) {
        DumpFormat.PlainText -> buildString {
            append("$presentableName performance report\n")
            unitStats.forEachStringMeasurement { appendLine(it) }
        }
        DumpFormat.Json -> UnitStatsJsonDumper.dump(unitStats)
        DumpFormat.Markdown -> MarkdownReportRenderer(StatsCalculator(SingleReportsData(unitStats))).render()
    }

    private fun ensureNotFinalizedAndSameThread() {
        if (!targetPlatform.isJs()) { // TODO: KT-75227
            assertIfDebug(!isFinalized) { "Cannot add a performance measurements because it's already finalized" }
        }
        assertIfDebug(Thread.currentThread() == thread) { "PerformanceManager functions can be run only from the same thread" }
    }

    private fun assertIfDebug(value: Boolean, lazyMessage: (() -> Any)? = null) {
        if (DEBUG_MODE) {
            if (lazyMessage != null) {
                assert(value, lazyMessage)
            } else {
                assert(value)
            }
        }
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
                    it.compilerType = mainPerformanceManager.compilerType
                }
            } else {
                null
            }
        }
    }
}

fun <T> PerformanceManager?.tryMeasureSideTime(phaseSideType: PhaseSideType, block: () -> T): T {
    return if (this == null) return block() else measureSideTime(phaseSideType, block)
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

inline fun <T> PerformanceManager?.tryMeasureDynamicPhaseTime(name: String, parentPhaseType: PhaseType, block: () -> T): T {
    if (this == null) return block()

    try {
        notifyDynamicPhaseStarted(name)
        return block()
    } finally {
        notifyDynamicPhaseFinished(name, parentPhaseType)
    }
}

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "All phase performance measurements should be finished explicitly")
annotation class PotentiallyIncorrectPhaseTimeMeasurement

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "Don't use in K2")
annotation class DeprecatedMeasurementForBackCompatibility