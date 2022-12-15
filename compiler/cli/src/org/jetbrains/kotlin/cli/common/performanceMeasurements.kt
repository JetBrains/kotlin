/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

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

class IRMeasurement(val lines: Int?, val milliseconds: Long, val kind: Kind) : PerformanceMeasurement {
    override fun render(): String = formatMeasurement("IR $kind", milliseconds, lines)

    enum class Kind {
        TRANSLATION, LOWERING, GENERATION
    }
}

private fun formatMeasurement(name: String, time: Long, lines: Int?): String =
    "%15s%8s ms".format(name, time) +
            (lines?.let {
                val lps = it.toDouble() * 1000 / time
                "%12.3f loc/s".format(lps)
            } ?: "")
