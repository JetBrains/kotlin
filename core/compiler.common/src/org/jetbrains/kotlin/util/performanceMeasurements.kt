/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.util.concurrent.TimeUnit

interface PerformanceMeasurement {
    fun render(lines: Int): String
}

enum class PhaseType {
    Initialization,
    Analysis,
    TranslationToIr,
    IrLowering,
    Backend,
}

/**
 * Currently it holds only `System.nanoTime()` but later it can be adopted to hold User or CPU time as well.
 * It might be useful for time measurements in multithread mode.
 */
@JvmInline
value class Time(val nanos: Long) {
    companion object {
        val ZERO = Time(0)
    }

    val millis: Long
        get() = TimeUnit.NANOSECONDS.toMillis(nanos)

    operator fun plus(other: Time): Time {
        return Time(nanos + other.nanos)
    }

    operator fun minus(other: Time): Time {
        return Time(nanos - other.nanos)
    }
}

sealed class PhasePerformanceMeasurement(val time: Time) : PerformanceMeasurement {
    abstract val phase: PhaseType
    abstract val name: String
    override fun render(lines: Int): String = "%20s%8s ms".format(name, time.millis) +
            if (phase != PhaseType.Initialization && lines != 0) {
                val lps = lines.toDouble() * 1000 / time.millis
                "%12.3f loc/s".format(lps)
            } else {
                ""
            }
}

class CompilerInitializationMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase = PhaseType.Initialization
    override val name: String = "INIT"
}

class CodeAnalysisMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseType = PhaseType.Analysis
    override val name: String = "ANALYZE"
}

class TranslationToIrMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseType = PhaseType.TranslationToIr
    override val name: String = "TRANSLATION to IR"
}

class IrLoweringMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseType = PhaseType.IrLowering
    override val name: String = "IR LOWERING"
}

class BackendMeasurement(time: Time) : PhasePerformanceMeasurement(time) {
    override val phase: PhaseType = PhaseType.Backend
    override val name: String = "BACKEND"
}

class JitCompilationMeasurement(val milliseconds: Long) : PerformanceMeasurement {
    override fun render(lines: Int): String = "JIT time is $milliseconds ms"
}

class GarbageCollectionMeasurement(val garbageCollectionKind: String, val milliseconds: Long, val count: Long) : PerformanceMeasurement {
    override fun render(lines: Int): String = "GC time for $garbageCollectionKind is $milliseconds ms, $count collections"
}

enum class PhaseSideType {
    FindJavaClass,
    BinaryClassFromKotlinFile,
}

sealed class SidePerformanceMeasurement(val count: Int, val time: Time) : PerformanceMeasurement {
    abstract val type: PhaseSideType
    abstract val description: String
    override fun render(lines: Int): String =
        "$description performed $count times, total time ${time.millis} ms"
}

class FindJavaClassMeasurement(count: Int, time: Time) : SidePerformanceMeasurement(count, time) {
    override val type = PhaseSideType.FindJavaClass
    override val description: String = "Find Java class"
}

class BinaryClassFromKotlinFileMeasurement(count: Int, time: Time) : SidePerformanceMeasurement(count, time) {
    override val type = PhaseSideType.BinaryClassFromKotlinFile
    override val description: String = "Binary class from Kotlin file"
}

@DeprecatedPerformanceDeclaration
class PerformanceCounterMeasurement(private val counterReport: String) : PerformanceMeasurement {
    override fun render(lines: Int): String = counterReport
}

@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "The declaration is kept to have back compatibility with K1")
annotation class DeprecatedPerformanceDeclaration