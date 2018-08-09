/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

interface PerformanceMeasurement {
    fun render(): String
}


class JitCompilationMeasurement(private val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = "JIT time is $milliseconds ms"
}


class CompilerInitializationMeasurement(private val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = "INIT: Compiler initialized in $milliseconds ms"
}


class CodeAnalysisMeasurement(private val files: Int, val lines: Int, private val milliseconds: Long, private val description: String?) :
    PerformanceMeasurement {

    private val speed: Double = lines.toDouble() * 1000 / milliseconds

    override fun render(): String =
        "ANALYZE: $files files ($lines lines) ${description ?: ""}in $milliseconds ms - ${"%.3f".format(speed)} loc/s"
}


class CodeGenerationMeasurement(private val files: Int, val lines: Int, private val milliseconds: Long, private val description: String?) :
    PerformanceMeasurement {

    private val speed: Double = lines.toDouble() * 1000 / milliseconds

    override fun render(): String =
        "GENERATE: $files files ($lines lines) ${description}in $milliseconds ms - ${"%.3f".format(speed)} loc/s"
}


class GarbageCollectionMeasurement(private val garbageCollectionKind: String, private val milliseconds: Long) : PerformanceMeasurement {
    override fun render(): String = "GC time for $garbageCollectionKind is $milliseconds ms"
}


class PerformanceCounterMeasurement(private val counterReport: String) : PerformanceMeasurement {
    override fun render(): String = counterReport
}
