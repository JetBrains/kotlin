/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.util.concurrent.TimeUnit

/**
 * Represents aggregated info about performance and other measurements in a simple format that compatible with
 * JSON serializer for using by third-party tools.
 * Also, it clarifies the collected measurements.
 */
data class UnitStats(
    /** Typically it should be a name of a module, but might be different
    if multiple `PerformanceManager` are used within the single module for handling multithreaded pipelines.
    In the last case, it should have a suffix like "(Child)".
     */
    val name: String?,
    val platform: PlatformType = PlatformType.JVM,
    val isK2: Boolean = true,
    val hasErrors: Boolean = false,

    // The following properties can be null in case of errors on previous stages.
    // For instance, if there is a syntax error in analysis, other stats info is not initialized.
    // In future `Time` can be replaced with more extended info if needed.
    val initStats: InitStats?,
    val analysisStats: AnalysisStats?,
    val irGenerationStats: IrStats?,
    val irLoweringStats: IrStats?,
    val backendStats: BinaryStats?,

    // Null in case of java or binary files not used
    val findJavaClassStats: BinaryStats? = null,
    val findKotlinClassStats: BinaryStats? = null,

    // Null/empty if extended measurements are not enabled
    val gcStats: List<GarbageCollectionStats> = listOf(),
    val jitTimeMillis: Long? = null,

    // Deprecated stats (from performance counter)
    val extendedStats: List<String> = emptyList(),
) {
    companion object {
        val EMPTY = UnitStats(
            name = "",
            PlatformType.JVM,
            isK2 = true,
            hasErrors = false,
            initStats = null,
            analysisStats = null,
            irGenerationStats = null,
            irLoweringStats = null,
            backendStats = null,
            findJavaClassStats = null,
            findKotlinClassStats = null,
            gcStats = emptyList(),
            jitTimeMillis = null,
        )
    }

    operator fun plus(other: UnitStats): UnitStats {
        if (isK2 != other.isK2 || platform != other.platform) return this

        val newName = when {
            name.isNullOrBlank() -> other.name
            other.name.isNullOrBlank() -> name
            else -> "$name, ${other.name}"
        }

        return UnitStats(
            name = newName,
            platform = platform,
            isK2 = isK2,
            hasErrors = hasErrors || other.hasErrors,
            initStats = (initStats ?: InitStats.EMPTY) + other.initStats,
            analysisStats = (analysisStats ?: AnalysisStats.EMPTY) + other.analysisStats,
            irGenerationStats = (irGenerationStats ?: IrStats.EMPTY) + other.irGenerationStats,
            irLoweringStats = (irLoweringStats ?: IrStats.EMPTY) + other.irLoweringStats,
            backendStats = (backendStats ?: BinaryStats.EMPTY) + other.backendStats,
            findJavaClassStats = (findJavaClassStats ?: BinaryStats.EMPTY) + other.findJavaClassStats,
            findKotlinClassStats = (findKotlinClassStats ?: BinaryStats.EMPTY) + other.findKotlinClassStats,
            gcStats = (gcStats + other.gcStats).groupBy { it.kind }
                .map { entry -> entry.component2().fold(GarbageCollectionStats.EMPTY) { result, gcStats -> result + gcStats } },
            jitTimeMillis = (jitTimeMillis ?: 0) + (other.jitTimeMillis ?: 0),
            extendedStats = extendedStats + other.extendedStats,
        )
    }
}

enum class PhaseMeasurementType {
    Initialization,
    Analysis,
    IrGeneration,
    IrLowering,
    BackendGeneration,
}

sealed interface PhaseStats<T : PhaseStats<T>> {
    val time: Time
    operator fun plus(otherPhaseStats: T?): T
    val empty: PhaseStats<T>
}

data class InitStats(
    override val time: Time,
    val filesCount: Int,
    val linesCount: Int,
) : PhaseStats<InitStats> {
    companion object {
        val EMPTY = InitStats(Time.ZERO, 0, 0)
    }

    override val empty: PhaseStats<InitStats>
        get() = EMPTY

    override fun plus(otherPhaseStats: InitStats?): InitStats {
        return otherPhaseStats?.let {
            InitStats(
                time + it.time,
                filesCount + it.filesCount,
                linesCount + it.linesCount
            )
        } ?: this
    }
}

data class AnalysisStats(
    override val time: Time,
    val allNodesCount: Int,
    val leafNodesCount: Int,
    val starImportsCount: Int,
) : PhaseStats<AnalysisStats> {
    companion object {
        val EMPTY = AnalysisStats(Time.ZERO, 0, 0, 0)
    }

    override val empty: PhaseStats<AnalysisStats>
        get() = EMPTY

    override fun plus(otherPhaseStats: AnalysisStats?): AnalysisStats {
        return otherPhaseStats?.let {
            AnalysisStats(
                time + it.time,
                allNodesCount + it.allNodesCount,
                leafNodesCount + it.leafNodesCount,
                starImportsCount + it.starImportsCount
            )
        } ?: this
    }
}

data class IrStats(
    override val time: Time,
    val allNodesAfterCount: Int,
    val leafNodesAfterCount: Int,
) : PhaseStats<IrStats> {
    companion object {
        val EMPTY = IrStats(Time.ZERO, 0, 0)
    }

    override val empty: PhaseStats<IrStats>
        get() = EMPTY

    override fun plus(otherPhaseStats: IrStats?): IrStats {
        return otherPhaseStats?.let {
            IrStats(
                time + it.time,
                allNodesAfterCount + it.allNodesAfterCount,
                leafNodesAfterCount + it.leafNodesAfterCount
            )
        } ?: this
    }
}

data class BinaryStats(
    override val time: Time,
    val count: Int,
    val bytesCount: Long,
) : PhaseStats<BinaryStats> {
    companion object {
        val EMPTY = BinaryStats(Time.ZERO, 0, 0)
    }

    override val empty: BinaryStats
        get() = EMPTY

    override fun plus(otherPhaseStats: BinaryStats?): BinaryStats {
        return otherPhaseStats?.let {
            BinaryStats(
                time + it.time,
                count + it.count,
                bytesCount + it.bytesCount,
            )
        } ?: this
    }
}

enum class PlatformType {
    JVM,
    JS,
    Common,
    Native,
}

/**
 * [userNano] and [cpuNano] can be useful for measuring times in multithread environment.
 */
data class Time(val nano: Long, val userNano: Long, val cpuNano: Long) {
    companion object {
        val ZERO = Time(0, 0, 0)
    }

    val millis: Long
        get() = TimeUnit.NANOSECONDS.toMillis(nano)

    operator fun plus(other: Time?): Time {
        if (other == null) return this
        return Time(nano + other.nano, userNano + other.userNano, cpuNano + other.cpuNano)
    }

    operator fun minus(other: Time): Time {
        return Time(nano - other.nano,  userNano - other.userNano, cpuNano - other.cpuNano)
    }

    operator fun unaryMinus(): Time {
        return Time(-nano, -userNano, -cpuNano)
    }

    operator fun div(other: Time): TimeRatio {
        return TimeRatio(
            nano.toDouble() / other.nano,
            userNano.toDouble() / other.userNano,
            cpuNano.toDouble() / other.cpuNano
        )
    }
}

data class TimeRatio(
    val nanosRatio: Double,
    val userNanosRatio: Double,
    val cpuNanosRatio: Double,
)

val Time?.thisOrZero: Time
    get() = this ?: Time.ZERO

enum class PhaseSideMeasurementType {
    FindJavaClass,
    BinaryClassFromKotlinFile,
}

data class GarbageCollectionStats(val kind: String, val millis: Long, val count: Long) {
    companion object {
        val EMPTY = GarbageCollectionStats("", 0, 0)
    }

    operator fun plus(otherGcStats: GarbageCollectionStats?): GarbageCollectionStats {
        return otherGcStats?.let {
            val newKind = when {
                kind.isEmpty() -> it.kind
                it.kind.isEmpty() -> kind
                else -> "$kind, ${it.kind}"
            }
            GarbageCollectionStats(newKind, millis + it.millis, count + it.count)
        } ?: this
    }
}

fun UnitStats.forEachPhaseMeasurement(action: (PhaseMeasurementType, PhaseStats<*>?) -> Unit) {
    action(PhaseMeasurementType.Initialization, initStats)
    action(PhaseMeasurementType.Analysis, analysisStats)
    action(PhaseMeasurementType.IrGeneration, irGenerationStats)
    action(PhaseMeasurementType.IrLowering, irLoweringStats)
    action(PhaseMeasurementType.BackendGeneration, backendStats)
}

fun UnitStats.forEachPhaseSideMeasurement(action: (PhaseSideMeasurementType, BinaryStats?) -> Unit) {
    action(PhaseSideMeasurementType.FindJavaClass, findJavaClassStats)
    action(PhaseSideMeasurementType.BinaryClassFromKotlinFile, findKotlinClassStats)
}

fun UnitStats.forEachStringMeasurement(action: (String) -> Unit) {
    val linesCount = initStats?.linesCount ?: 0

    forEachPhaseMeasurement { phaseType, stats ->
        if (stats == null) return@forEachPhaseMeasurement

        val name = when (phaseType) {
            PhaseMeasurementType.Initialization -> "INIT"
            PhaseMeasurementType.Analysis -> "ANALYZE"
            PhaseMeasurementType.IrGeneration -> "IR GENERATION"
            PhaseMeasurementType.IrLowering -> "IR LOWERING"
            PhaseMeasurementType.BackendGeneration -> "BACKEND GENERATION"
        }

        action(
            "%20s%8s ms".format(name, stats.time.millis) +
                    if (phaseType != PhaseMeasurementType.Initialization && linesCount != 0) {
                        val lps = linesCount.toDouble() * 1000 / stats.time.millis
                        "%12.3f loc/s".format(lps)
                    } else {
                        ""
                    }
        )
    }

    forEachPhaseSideMeasurement { phaseSideType, sideStats ->
        if (sideStats == null) return@forEachPhaseSideMeasurement

        val description = when (phaseSideType) {
            PhaseSideMeasurementType.BinaryClassFromKotlinFile -> "Binary class from Kotlin file"
            PhaseSideMeasurementType.FindJavaClass -> "Find Java class"
        }

        action("$description performed ${sideStats.count} times, total time ${sideStats.time.millis} ms")
    }

    gcStats.forEach {
        action("GC time for ${it.kind} is ${it.millis} ms, ${it.count} collections")
    }

    jitTimeMillis?.let {
        action("JIT time is $it ms")
    }

    extendedStats.forEach { action(it) }
}