/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

interface PerformanceMeasurement {
    fun render(lines: Int): String
}

enum class PhaseMeasurementType {
    Initialization,
    Analysis,
    IrGeneration,
    IrLowering,
    BackendGeneration,
}

/**
 * Currently it holds only `System.nanoTime()` but later it can be adopted to hold User or CPU time as well
 * that might be useful for time measurements in multithread mode.
 */
@JvmInline
value class Time(val nanoseconds: Long) {
    companion object {
        val ZERO = Time(0)
    }

    val milliseconds: Long
        get() = nanoseconds / 1_000_000L

    operator fun plus(other: Time): Time {
        return Time(nanoseconds + other.nanoseconds)
    }

    operator fun minus(other: Time): Time {
        return Time(nanoseconds - other.nanoseconds)
    }
}

sealed class PhasePerformanceMeasurement(val time: Time) : PerformanceMeasurement {
    abstract val phase: PhaseMeasurementType
    abstract val name: String
    override fun render(lines: Int): String = "%20s%8s ms".format(name, time.milliseconds) +
            if (phase != PhaseMeasurementType.Initialization && lines != 0) {
                val lps = lines.toDouble() * 1000 / time.milliseconds
                "%12.3f loc/s".format(lps)
            } else {
                ""
            }
}

class CompilerInitializationMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase = PhaseMeasurementType.Initialization
    override val name: String = "INIT"
}

class CodeAnalysisMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseMeasurementType = PhaseMeasurementType.Analysis
    override val name: String = "ANALYZE"
}

class IrGenerationMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseMeasurementType = PhaseMeasurementType.IrGeneration
    override val name: String = "IR GENERATION"
}

class IrLoweringMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseMeasurementType = PhaseMeasurementType.IrLowering
    override val name: String = "IR LOWERING"
}

class BackendGenerationMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseMeasurementType = PhaseMeasurementType.BackendGeneration
    override val name: String = "BACKEND GENERATION"
}

class JitCompilationMeasurement(val milliseconds: Long) : PerformanceMeasurement {
    override fun render(lines: Int): String = "JIT time is $milliseconds ms"
}

class GarbageCollectionMeasurement(val garbageCollectionKind: String, val milliseconds: Long, val count: Long) : PerformanceMeasurement {
    override fun render(lines: Int): String = "GC time for $garbageCollectionKind is $milliseconds ms, $count collections"
}

enum class PhaseSideMeasurementType {
    FindJavaClass,
    BinaryClassFromKotlinFile,
}

sealed class PhaseSidePerformanceMeasurement(val count: Int, val time: Time) : PerformanceMeasurement {
    abstract val type: PhaseSideMeasurementType
    abstract val description: String
    override fun render(lines: Int): String =
        "$description performed $count times, total time ${time.milliseconds} ms"
}

class FindJavaClassMeasurement(count: Int, time: Time) : PhaseSidePerformanceMeasurement(count, time) {
    override val type = PhaseSideMeasurementType.FindJavaClass
    override val description: String = "Find Java class"
}

class BinaryClassFromKotlinFileMeasurement(count: Int, time: Time) : PhaseSidePerformanceMeasurement(count, time) {
    override val type = PhaseSideMeasurementType.BinaryClassFromKotlinFile
    override val description: String = "Binary class from Kotlin file"
}

@DeprecatedPerformanceDeclaration
class PerformanceCounterMeasurement(private val counterReport: String) : PerformanceMeasurement {
    override fun render(lines: Int): String = counterReport
}

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "The declaration is kept to have back compatibility with K1")
annotation class DeprecatedPerformanceDeclaration