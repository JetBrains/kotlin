/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

interface PerformanceMeasurement {
    fun render(): String
}

class JitCompilationMeasurement(private val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = "JIT time is $milliseconds ms"
}

class CompilerInitializationMeasurement(val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = "INIT: Compiler initialized in $milliseconds ms"
}

class CodeAnalysisMeasurement(val lines: Int?, val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("ANALYZE", milliseconds, lines)
}

class CodeGenerationMeasurement(val lines: Int?, val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("GENERATE", milliseconds, lines)
}

class GarbageCollectionMeasurement(val garbageCollectionKind: String, val milliseconds: Long, val count: Long) : PerformanceMeasurement {
    override fun render(): String = "GC time for $garbageCollectionKind is $milliseconds ms, $count collections"
}

class PerformanceCounterMeasurement(private val counterReport: String) : PerformanceMeasurement {
    override fun render(): String = counterReport
}

class TranslationToIrMeasurement(val lines: Int?, val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("TRANSLATION to IR", milliseconds, lines)
}

class IrLoweringMeasurement(val lines: Int?, val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("IR LOWERING", milliseconds, lines)
}

class BackendMeasurement(val lines: Int?, val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("BACKEND", milliseconds, lines)
}

sealed class CounterMeasurement(val count: Int, val milliseconds: Long) : PerformanceMeasurement {
    abstract val description: String
    override fun render(): String =
        "$description performed $count times, total time $milliseconds ms"
}

class FindJavaClassMeasurement(count: Int, milliseconds: Long) : CounterMeasurement(count, milliseconds) {
    override val description: String = "Find Java class"
}

class BinaryClassFromKotlinFileMeasurement(count: Int, milliseconds: Long) : CounterMeasurement(count, milliseconds) {
    override val description: String = "Binary class from Kotlin file"
}

private fun formatMeasurement(name: String, time: Long, lines: Int?): String =
    "%20s%8s ms".format(name, time) +
            (lines?.let {
                val lps = it.toDouble() * 1000 / time
                "%12.3f loc/s".format(lps)
            } ?: "")
