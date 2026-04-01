/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class DumpDirectoryConversionTest : BaseArgumentTest<String>("Xdump-directory") {

    @DisplayName("DumpDirectory is converted to '-Xdump-directory' argument")
    @Test
    fun testDumpDirectoryToArgumentString() {
        val dumpDirectoryPath = workingDirectory.resolve("path/to/dump").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_DIRECTORY] = dumpDirectoryPath
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(dumpDirectoryPath)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xdump-directory' has the default value when DumpDirectory is not set")
    @Test
    fun testDumpDirectoryNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("DumpDirectory can be set and retrieved")
    @Test
    fun testDumpDirectoryGetWhenSet() {
        val expectedDumpDirectory = workingDirectory.resolve("path/to/dump").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_DIRECTORY] = expectedDumpDirectory
        }

        val actualDumpDirectory = jvmOperation.compilerArguments[X_DUMP_DIRECTORY]

        assertEquals(expectedDumpDirectory, actualDumpDirectory)
    }

    @DisplayName("DumpDirectory has the default value when not set")
    @Test
    fun testDumpDirectoryGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val dumpDirectory = jvmOperation.compilerArguments[X_DUMP_DIRECTORY]

        assertEquals(
            getDefaultValueString(), getValueString(dumpDirectory)
        )
    }

    @DisplayName("Raw argument strings '-Xdump-directory <path>' are converted to DumpDirectory")
    @Test
    fun testRawArgumentsDumpDirectoryConversion() {
        val expectedDumpDirectoryPath = workingDirectory.resolve("path/to/dump").absolutePathString()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedDumpDirectoryPath))
        )

        assertEquals(
            expectedDumpDirectoryPath, operation.compilerArguments[X_DUMP_DIRECTORY]
        )
    }

    @DisplayName("DumpDirectory has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsDumpDirectory() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_DUMP_DIRECTORY])
        )
    }

    @DisplayName("DumpDirectory of null value is converted to '-Xdump-directory' argument")
    @Test
    fun testNullDumpDirectory() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DUMP_DIRECTORY] = null
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
