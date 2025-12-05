/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.round

/**
 * Represents aggregated info about performance and other measurements in a simple format that compatible with
 * JSON or serializers for using by third-party tools.
 * Also, it refines the collected measurements.
 *
 * Typically, it represents module info, but sometimes it can be used for measuring stats of multiple units that run in parallel in multithread mode or not.
 * In the case of multithreading, the phase properties hold aggregated time over multiple processes, but not the total.
 * It means that aggregated time could be much bigger than the total execution time (the more threads are used, the biggest difference between aggregated and total time is).
 * In this case maybe it's more meaningful to analyze a phase time ratio but not the absolute time itself.
 * However, properties [gcStats] and [jitTimeMillis] hold total time obtained from the root [PerformanceManager]; its children don't
 * track that stat. It's more reliable because the total time doesn't have overlapping, and it's a more meaningful measurement to analyze.
 *
 * Properties that are initialized with default values can't be skipped during serialization,
 * because the Gson library used in build tools doesn't support default values, see https://github.com/google/gson/issues/1657
 */
data class UnitStats(
    /** Typically it's a name of a module, but if multiple `PerformanceManager` are used within the single module it's a unit name.
    In the last case, it should have a suffix like `(Child)`.
     */
    val name: String?,
    val outputKind: String?,
    // Use plain millis to get rid of using a custom JSON serializer for `java.util.Date`
    val timeStampMs: Long = System.currentTimeMillis(),
    val platform: PlatformType = PlatformType.JVM,
    val compilerType: CompilerType = CompilerType.K2,
    val hasErrors: Boolean = false,
    val filesCount: Int,
    val linesCount: Int,

    // The following properties can be null in case of errors on previous stages.
    // For instance, if there is a syntax error in analysis, other stats info is not initialized.
    // In future `Time` can be replaced with a more extended type if needed.
    val initStats: Time?,
    val analysisStats: Time?,
    val translationToIrStats: Time?,
    val irPreLoweringStats: Time?,
    val irSerializationStats: Time?,
    val klibWritingStats: Time?,
    val irLoweringStats: Time?,
    val backendStats: Time?,

    val dynamicStats: List<DynamicStats>? = null,

    val klibElementStats: List<KlibElementStats>? = null,

    // Null in case of java files not used
    val findJavaClassStats: SideStats? = null,
    // Typically always not null because binary files are used for stdlib deserializing.
    val findKotlinClassStats: SideStats? = null,

    // Null/empty if extended measurements are not enabled
    val gcStats: List<GarbageCollectionStats> = listOf(),
    val jitTimeMillis: Long? = null,

    @property:DeprecatedMeasurementForBackCompatibility
    val extendedStats: List<String>? = null,
) : Comparable<UnitStats> {
    fun getTotalTime(): Time {
        return Time.ZERO +
                initStats +
                analysisStats +
                translationToIrStats +
                irPreLoweringStats +
                irSerializationStats +
                klibWritingStats +
                irLoweringStats +
                backendStats +
                findJavaClassStats?.time +
                findKotlinClassStats?.time
    }

    override fun compareTo(other: UnitStats): Int {
        return timeStampMs.compareTo(other.timeStampMs)
    }
}

enum class CompilerType {
    K1,
    K2,
    K1andK2;

    val isK2: Boolean
        get() = this == K2 || this == K1andK2

    operator fun plus(other: CompilerType?): CompilerType {
        if (other == null) return this
        return when (this) {
            K1 -> if (other.isK2) K1andK2 else K1
            K2 -> if (other == K1 || other == K1andK2) K1andK2 else K2
            K1andK2 -> K1andK2
        }
    }
}

enum class PhaseType {
    Initialization,
    Analysis,
    TranslationToIr,
    IrPreLowering,
    IrSerialization,
    KlibWriting,
    IrLowering,
    Backend,
}

enum class PlatformType {
    JVM,
    JS,
    Common,
    Native,
}

/**
 * [userNanos] and [cpuNanos] can be useful for measuring times in multithread environment.
 */
data class Time(val nanos: Long, val userNanos: Long, val cpuNanos: Long) {
    companion object {
        val ZERO = Time(0, 0, 0)
    }

    val millis: Long
        get() = TimeUnit.NANOSECONDS.toMillis(nanos)

    operator fun plus(other: Time?): Time {
        if (other == null) return this
        return Time(nanos + other.nanos, userNanos + other.userNanos, cpuNanos + other.cpuNanos)
    }

    operator fun minus(other: Time?): Time {
        if (other == null) return this
        return Time(nanos - other.nanos, userNanos - other.userNanos, cpuNanos - other.cpuNanos)
    }

    operator fun unaryMinus(): Time {
        return Time(-nanos, -userNanos, -cpuNanos)
    }

    operator fun div(other: Time): TimeRatio {
        return TimeRatio(nanos.toDouble() / other.nanos, userNanos.toDouble() / other.userNanos, cpuNanos.toDouble() / other.cpuNanos)
    }

    operator fun div(divider: Int): Time {
        return Time(nanos / divider, userNanos / divider, cpuNanos / divider)
    }

    operator fun times(multiplier: Double): Time {
        return Time(
            round(nanos * multiplier).toLong(),
            round(userNanos * multiplier).toLong(),
            round(cpuNanos * multiplier).toLong(),
        )
    }
}

data class TimeRatio(
    val nanos: Double,
    val userNanos: Double,
    val cpuNanos: Double,
)

enum class PhaseSideType {
    FindJavaClass,
    BinaryClassFromKotlinFile,
}

data class SideStats(
    val count: Int,
    val time: Time,
) {
    companion object {
        val EMPTY = SideStats(0, Time.ZERO)
    }

    operator fun plus(other: SideStats?): SideStats {
        if (other == null) return this
        return SideStats(count + other.count, time + other.time)
    }

    operator fun div(divider: Int): SideStats {
        return SideStats(count / divider, time / divider)
    }
}

data class GarbageCollectionStats(val kind: String, val millis: Long, val count: Long) {
    companion object {
        val EMPTY = GarbageCollectionStats("", 0, 0)
    }
}

data class DynamicStats(val parentPhaseType: PhaseType, val name: String, val time: Time)

data class KlibElementStats(val path: String, val size: Long)

fun UnitStats.forEachPhaseMeasurement(action: (PhaseType, Time?) -> Unit) {
    action(PhaseType.Initialization, initStats)
    action(PhaseType.Analysis, analysisStats)
    action(PhaseType.TranslationToIr, translationToIrStats)
    action(PhaseType.IrPreLowering, irPreLoweringStats)
    action(PhaseType.IrSerialization, irSerializationStats)
    action(PhaseType.KlibWriting, klibWritingStats)
    action(PhaseType.IrLowering, irLoweringStats)
    action(PhaseType.Backend, backendStats)
}

fun UnitStats.forEachPhaseSideMeasurement(action: (PhaseSideType, SideStats?) -> Unit) {
    action(PhaseSideType.FindJavaClass, findJavaClassStats)
    action(PhaseSideType.BinaryClassFromKotlinFile, findKotlinClassStats)
}

val phaseTypeName = mapOf(
    PhaseType.Initialization to "INIT",
    PhaseType.Analysis to "ANALYZE",
    PhaseType.TranslationToIr to "TRANSLATION to IR",
    PhaseType.IrPreLowering to "IR PRE-LOWERING",
    PhaseType.IrSerialization to "IR SERIALIZATION",
    PhaseType.KlibWriting to "KLIB WRITING",
    PhaseType.IrLowering to "IR LOWERING",
    PhaseType.Backend to "BACKEND",
)

val phaseSideTypeName = mapOf(
    PhaseSideType.FindJavaClass to "Find Java class",
    PhaseSideType.BinaryClassFromKotlinFile to "Binary class from Kotlin file"
)

fun PerformanceManager.forEachStringMeasurement(action: (String) -> Unit) {
    with(unitStats) {
        forEachPhaseMeasurement { phaseType, time ->
            if (time == null) return@forEachPhaseMeasurement

            action(
                "%20s%8s ms".format(phaseTypeName.getValue(phaseType), time.millis) +
                        if (phaseType != PhaseType.Initialization && linesCount != 0) {
                            "%12.3f loc/s".format(Locale.ENGLISH, getLinesPerSecond(time))
                        } else {
                            ""
                        }
            )

            dynamicStats?.filter { it.parentPhaseType == phaseType }?.let { filteredDynamicStats ->
                if (detailedPerf) {
                    filteredDynamicStats.forEach { (_, dynamicName, dynamicTime) ->
                        action(
                            "%20s%8s ms".format("DYNAMIC PHASE", dynamicTime.millis) +
                                    if (linesCount != 0) {
                                        "%12.3f loc/s ($dynamicName)".format(Locale.ENGLISH, getLinesPerSecond(dynamicTime))
                                    } else {
                                        " ($dynamicName)"
                                    }
                        )
                    }
                } else {
                    if (filteredDynamicStats.isNotEmpty()) {
                        var totTime = Time.ZERO
                        filteredDynamicStats.forEach {
                            totTime += it.time
                        }
                        action(
                            "%20s%8s ms".format("DYNAMIC PHASES", totTime.millis) +
                                    if (linesCount != 0) "%12.3f loc/s".format(Locale.ENGLISH, getLinesPerSecond(totTime)) else ""
                        )
                    }
                }
            }
        }

        if (detailedPerf) {
            klibElementStats?.forEach { (path, size) ->
                action("KLIB element '$path' has size of $size Bytes")
            }
        }

        forEachPhaseSideMeasurement { phaseSideType, sideStats ->
            if (sideStats == null) return@forEachPhaseSideMeasurement

            val description = phaseSideTypeName.getValue(phaseSideType)

            action("$description performed ${sideStats.count} times, total time ${sideStats.time.millis} ms")
        }

        gcStats.forEach {
            action("GC time for ${it.kind} is ${it.millis} ms, ${it.count} collections")
        }

        jitTimeMillis?.let {
            action("JIT time is $it ms")
        }

        @OptIn(DeprecatedMeasurementForBackCompatibility::class)
        extendedStats?.forEach { action(it) }
    }
}

val nanosInSecond = TimeUnit.SECONDS.toNanos(1)

fun UnitStats.getLinesPerSecond(time: Time): Double {
    return linesCount.toDouble() / time.nanos * nanosInSecond // Assume `nanos` is never zero because it's unlikely possible to measure such a small time
}