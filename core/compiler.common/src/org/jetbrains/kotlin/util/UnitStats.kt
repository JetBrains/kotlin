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
class UnitStats(
    /** Typically it should be a name of a module, but might be different
    if multiple `PerformanceManager` are used within the single module for handling multithreaded pipelines.
    In the last case, it should have a suffix like "(Child)".
     */
    val name: String?,
    val platform: PlatformType = PlatformType.JVM,
    val isK2: Boolean = true,
    val hasErrors: Boolean = false,
    val filesCount: Int,
    val linesCount: Int,

    // The following properties can be null in case of errors on previous stages.
    // For instance, if there is a syntax error in analysis, other stats info is not initialized.
    // In future `Time` can be replaced with more extended info if needed.
    val initStats: Time?,
    val analysisStats: Time?,
    val irGenerationStats: Time?,
    val irLoweringStats: Time?,
    val backendStats: Time?,

    // Null in case of java or binary files not used
    val findJavaClassStats: SideStats?,
    val findKotlinClassStats: SideStats?,

    // Null/empty if extended measurements are not enabled
    val gcStats: List<GarbageCollectionStats>,
    val jitTimeMillis: Long?,

    // Deprecated stats (from performance counter)
    val extendedStats: List<String> = emptyList(),
)

enum class PhaseMeasurementType {
    Initialization,
    Analysis,
    IrGeneration,
    IrLowering,
    BackendGeneration,
}

enum class PlatformType {
    JVM,
    JS,
    Common,
    Native,
}

/**
 * Currently it holds only `System.nanoTime()` but later it can be adopted to hold User or CPU time as well
 * that might be useful for time measurements in multithread mode.
 */
@JvmInline
value class Time(val nano: Long) {
    companion object {
        val ZERO = Time(0)
    }

    val millis: Long
        get() = TimeUnit.NANOSECONDS.toMillis(nano)

    operator fun plus(other: Time): Time {
        return Time(nano + other.nano)
    }

    operator fun minus(other: Time): Time {
        return Time(nano - other.nano)
    }
}

enum class PhaseSideMeasurementType {
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

fun UnitStats.forEachPhaseMeasurement(action: (PhaseMeasurementType, Time?) -> Unit) {
    action(PhaseMeasurementType.Initialization, initStats)
    action(PhaseMeasurementType.Analysis, analysisStats)
    action(PhaseMeasurementType.IrGeneration, irGenerationStats)
    action(PhaseMeasurementType.IrLowering, irLoweringStats)
    action(PhaseMeasurementType.BackendGeneration, backendStats)
}

fun UnitStats.forEachPhaseSideMeasurement(action: (PhaseSideMeasurementType, SideStats?) -> Unit) {
    action(PhaseSideMeasurementType.FindJavaClass, findJavaClassStats)
    action(PhaseSideMeasurementType.BinaryClassFromKotlinFile, findKotlinClassStats)
}

fun UnitStats.forEachStringMeasurement(action: (String) -> Unit) {
    forEachPhaseMeasurement { phaseType, time ->
        if (time == null) return@forEachPhaseMeasurement

        val name = when (phaseType) {
            PhaseMeasurementType.Initialization -> "INIT"
            PhaseMeasurementType.Analysis -> "ANALYZE"
            PhaseMeasurementType.IrGeneration -> "IR GENERATION"
            PhaseMeasurementType.IrLowering -> "IR LOWERING"
            PhaseMeasurementType.BackendGeneration -> "BACKEND GENERATION"
        }

        action(
            "%20s%8s ms".format(name, time.millis) +
                    if (phaseType != PhaseMeasurementType.Initialization && linesCount != 0) {
                        val lps = linesCount.toDouble() * 1000 / time.millis
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