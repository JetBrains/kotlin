/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_PERF
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class DumpPerfConversionTest : BaseArgumentTest<String>("Xdump-perf") {

    @DisplayName("DumpPerf is converted to '-Xdump-perf' argument")
    @Test
    fun testDumpPerfToArgumentString() {
        val dumpPerfPath = workingDirectory.resolve("path/to/perf.log").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_PERF] = dumpPerfPath
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(dumpPerfPath)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xdump-perf' has the default value when DumpPerf is not set")
    @Test
    fun testDumpPerfNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("DumpPerf can be set and retrieved")
    @Test
    fun testDumpPerfGetWhenSet() {
        val expectedDumpPerf = workingDirectory.resolve("path/to/perf.log").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_PERF] = expectedDumpPerf
        }

        val actualDumpPerf = jvmOperation.compilerArguments[X_DUMP_PERF]

        assertEquals(expectedDumpPerf, actualDumpPerf)
    }

    @DisplayName("DumpPerf has the default value when not set")
    @Test
    fun testDumpPerfGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val dumpPerf = jvmOperation.compilerArguments[X_DUMP_PERF]

        assertEquals(
            getDefaultValueString(), getValueString(dumpPerf)
        )
    }

    @DisplayName("Raw argument strings '-Xdump-perf <path>' are converted to DumpPerf")
    @Test
    fun testRawArgumentsDumpPerfConversion() {
        val expectedDumpPerfPath = workingDirectory.resolve("path/to/perf.log").absolutePathString()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedDumpPerfPath))
        )

        assertEquals(
            expectedDumpPerfPath, operation.compilerArguments[X_DUMP_PERF]
        )
    }

    @DisplayName("DumpPerf has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsDumpPerf() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_DUMP_PERF])
        )
    }

    @DisplayName("DumpPerf of null value is converted to '-Xdump-perf' argument")
    @Test
    fun testNullDumpPerf() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_PERF] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: String?): String? = argument
}
