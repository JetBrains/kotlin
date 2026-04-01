/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_PROFILE
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class ProfileCompilerCommandConversionTest : BaseArgumentTest<String>("Xprofile") {

    @DisplayName("ProfileCompilerCommand is converted to '-Xprofile' argument")
    @Test
    fun testXprofileToArgumentString() {
        val profileCompilerCommand = createProfileCompilerCommandStringRepresentation(
            workingDirectory.resolve("path/to/libasyncProfiler.so"),
            "event=cpu,interval=1ms,threads,start",
            workingDirectory.resolve("path/to/snapshots")
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PROFILE] = profileCompilerCommand
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(profileCompilerCommand)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xprofile' has the default value when ProfileCompilerCommand is not set")
    @Test
    fun testXprofileNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ProfileCompilerCommand can be set and retrieved")
    @Test
    fun testXprofileGetWhenSet() {
        val expectedProfileCommand = createProfileCompilerCommandStringRepresentation(
            workingDirectory.resolve("path/to/libasyncProfiler.so"),
            "event=cpu,interval=1ms,threads,start",
            workingDirectory.resolve("path/to/snapshots")
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PROFILE] = expectedProfileCommand
        }

        val actualProfileCommand = jvmOperation.compilerArguments[X_PROFILE]

        assertEquals(expectedProfileCommand, actualProfileCommand)
    }

    @DisplayName("ProfileCompilerCommand has the default value when not set")
    @Test
    fun testXprofileGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val profileCompilerCommand = jvmOperation.compilerArguments[X_PROFILE]

        assertEquals(
            getDefaultValueString(), getValueString(profileCompilerCommand)
        )
    }

    @DisplayName("Raw argument strings '-Xprofile=<value>' are converted to ProfileCompilerCommand")
    @Test
    fun testRawArgumentsXprofileConversion() {
        val expectedProfileCommand = createProfileCompilerCommandStringRepresentation(
            workingDirectory.resolve("path/to/libasyncProfiler.so"),
            "event=cpu,interval=1ms,threads,start",
            workingDirectory.resolve("path/to/snapshots")
        )
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedProfileCommand))
        )
        val actualProfileCommand = operation.compilerArguments[X_PROFILE]

        assertEquals(expectedProfileCommand, actualProfileCommand)
    }

    @DisplayName("ProfileCompilerCommand has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsXprofile() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_PROFILE])
        )
    }

    @DisplayName("ProfileCompilerCommand of null value is converted to '-Xprofile' argument")
    @Test
    fun testNullProfileCompilerCommand() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PROFILE] = null
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

    private fun createProfileCompilerCommandStringRepresentation(
        profilerPath: Path,
        command: String,
        outputDir: Path,
    ) = profilerPath.absolutePathString() + File.pathSeparatorChar + command + File.pathSeparatorChar + outputDir.absolutePathString()
}