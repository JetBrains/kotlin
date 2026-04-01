/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_PERF
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class DumpPerfConversionTest : BaseArgumentTest<Path>("Xdump-perf") {

    @DisplayName("DumpPerf is converted to '-Xdump-perf' argument")
    @BtaVersionsOnlyCompilationTest
    fun testDumpPerfToArgumentString(toolchain: KotlinToolchains) {
        val dumpPerfPath = workingDirectory.resolve("path/to/perf.log")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_PERF] = dumpPerfPath
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(dumpPerfPath), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xdump-perf' has the default value when DumpPerf is not set")
    @BtaVersionsOnlyCompilationTest
    fun testDumpPerfNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("DumpPerf can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testDumpPerfGetWhenSet(toolchain: KotlinToolchains) {
        val expectedDumpPerf = workingDirectory.resolve("path/to/perf.log")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_PERF] = expectedDumpPerf
        }.build()

        val actualDumpPerf = jvmOperation.compilerArguments[X_DUMP_PERF]

        assertEquals(expectedDumpPerf, actualDumpPerf)
    }

    @DisplayName("DumpPerf has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testDumpPerfGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val dumpPerf = jvmOperation.compilerArguments[X_DUMP_PERF]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(dumpPerf)
        )
    }

    @DisplayName("Raw argument strings '-Xdump-perf <path>' are converted to DumpPerf")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsDumpPerfConversion(toolchain: KotlinToolchains) {
        val expectedDumpPerfPath = workingDirectory.resolve("path/to/perf.log")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedDumpPerfPath),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedDumpPerfPath,
            operation.compilerArguments[X_DUMP_PERF]
        )
    }

    @DisplayName("DumpPerf has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsDumpPerf(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_DUMP_PERF])
        )
    }

    @DisplayName("DumpPerf of null value is converted to '-Xdump-perf' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullDumpPerf(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_PERF] = null
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Path?): String? = argument?.toFile()?.absolutePath
}
