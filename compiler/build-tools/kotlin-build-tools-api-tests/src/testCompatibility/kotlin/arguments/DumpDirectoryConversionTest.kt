/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class DumpDirectoryConversionTest : BaseArgumentTest<Path>("Xdump-directory") {

    @DisplayName("DumpDirectory is converted to '-Xdump-directory' argument")
    @BtaVersionsOnlyCompilationTest
    fun testDumpDirectoryToArgumentString(toolchain: KotlinToolchains) {
        val dumpDirectoryPath = workingDirectory.resolve("path/to/dump")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_DIRECTORY] = dumpDirectoryPath
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(dumpDirectoryPath), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xdump-directory' has the default value when DumpDirectory is not set")
    @BtaVersionsOnlyCompilationTest
    fun testDumpDirectoryNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("DumpDirectory can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testDumpDirectoryGetWhenSet(toolchain: KotlinToolchains) {
        val expectedDumpDirectory = workingDirectory.resolve("path/to/dump")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_DIRECTORY] = expectedDumpDirectory
        }.build()

        val actualDumpDirectory = jvmOperation.compilerArguments[X_DUMP_DIRECTORY]

        assertEquals(expectedDumpDirectory, actualDumpDirectory)
    }

    @DisplayName("DumpDirectory has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testDumpDirectoryGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val dumpDirectory = jvmOperation.compilerArguments[X_DUMP_DIRECTORY]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(dumpDirectory)
        )
    }

    @DisplayName("Raw argument strings '-Xdump-directory <path>' are converted to DumpDirectory")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsDumpDirectoryConversion(toolchain: KotlinToolchains) {
        val expectedDumpDirectoryPath = workingDirectory.resolve("path/to/dump")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedDumpDirectoryPath),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedDumpDirectoryPath,
            operation.compilerArguments[X_DUMP_DIRECTORY]
        )
    }

    @DisplayName("DumpDirectory has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsDumpDirectory(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_DUMP_DIRECTORY])
        )
    }

    @DisplayName("DumpDirectory of null value is converted to '-Xdump-directory' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullDumpDirectory(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_DIRECTORY] = null
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
