/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.util.Locale
import java.util.concurrent.TimeUnit

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
    val irLoweringStats: Time?,
    val backendStats: Time?,

    // Null in case of java files not used
    val findJavaClassStats: SideStats? = null,
    // Typically always not null because binary files are used for stdlib deserializing.
    val findKotlinClassStats: SideStats? = null,

    // Null/empty if extended measurements are not enabled
    val gcStats: List<GarbageCollectionStats> = listOf(),
    val jitTimeMillis: Long? = null,

    @property:DeprecatedMeasurementForBackCompatibility
    val extendedStats: List<String>? = null,
)

enum class CompilerType {
    K1,
    K2,
    K1andK2;

    val isK2: Boolean
        get() = this == K2 || this == K1andK2

    operator fun plus(other: CompilerType): CompilerType {
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

    operator fun plus(other: Time): Time {
        return Time(nanos + other.nanos, userNanos + other.userNanos, cpuNanos + other.cpuNanos)
    }

    operator fun minus(other: Time): Time {
        return Time(nanos - other.nanos, userNanos - other.userNanos, cpuNanos - other.cpuNanos)
    }
}

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

    operator fun plus(other: SideStats): SideStats {
        return SideStats(count + other.count, time + other.time)
    }
}

data class GarbageCollectionStats(val kind: String, val millis: Long, val count: Long)

fun UnitStats.forEachPhaseMeasurement(action: (PhaseType, Time?) -> Unit) {
    action(PhaseType.Initialization, initStats)
    action(PhaseType.Analysis, analysisStats)
    action(PhaseType.TranslationToIr, translationToIrStats)
    action(PhaseType.IrLowering, irLoweringStats)
    action(PhaseType.Backend, backendStats)
}

fun UnitStats.forEachPhaseSideMeasurement(action: (PhaseSideType, SideStats?) -> Unit) {
    action(PhaseSideType.FindJavaClass, findJavaClassStats)
    action(PhaseSideType.BinaryClassFromKotlinFile, findKotlinClassStats)
}

fun UnitStats.forEachStringMeasurement(action: (String) -> Unit) {
    forEachPhaseMeasurement { phaseType, time ->
        if (time == null) return@forEachPhaseMeasurement

        val name = when (phaseType) {
            PhaseType.Initialization -> "INIT"
            PhaseType.Analysis -> "ANALYZE"
            PhaseType.TranslationToIr -> "TRANSLATION to IR"
            PhaseType.IrLowering -> "IR LOWERING"
            PhaseType.Backend -> "BACKEND"
        }

        action(
            "%20s%8s ms".format(name, time.millis) +
                    if (phaseType != PhaseType.Initialization && linesCount != 0) {
                        "%12.3f loc/s".format(Locale.ENGLISH, getLinesPerSecond(time))
                    } else {
                        ""
                    }
        )
    }

    forEachPhaseSideMeasurement { phaseSideType, sideStats ->
        if (sideStats == null) return@forEachPhaseSideMeasurement

        val description = when (phaseSideType) {
            PhaseSideType.BinaryClassFromKotlinFile -> "Binary class from Kotlin file"
            PhaseSideType.FindJavaClass -> "Find Java class"
        }

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

private val nanosInSecond = TimeUnit.SECONDS.toNanos(1)

fun UnitStats.getLinesPerSecond(time: Time): Double {
    return linesCount.toDouble() / time.nanos * nanosInSecond // Assume `nanos` is never zero because it's unlikely possible to measure such a small time
}