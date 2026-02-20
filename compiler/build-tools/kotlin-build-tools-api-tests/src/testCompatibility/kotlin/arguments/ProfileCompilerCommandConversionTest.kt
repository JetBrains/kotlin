/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_PROFILE
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class ProfileCompilerCommandConversionTest : BaseArgumentTest<ProfileCompilerCommand>("Xprofile") {

    @DisplayName("ProfileCompilerCommand is converted to '-Xprofile' argument")
    @BtaVersionsOnlyCompilationTest
    fun testXprofileToArgumentString(toolchain: KotlinToolchains) {
        val profileCompilerCommand = ProfileCompilerCommand(
            profilerPath = workingDirectory.resolve("path/to/libasyncProfiler.so"),
            command = "event=cpu,interval=1ms,threads,start",
            outputDir = workingDirectory.resolve("/path/to/snapshots")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PROFILE] = profileCompilerCommand
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(profileCompilerCommand), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xprofile' has the default value when ProfileCompilerCommand is not set")
    @BtaVersionsOnlyCompilationTest
    fun testXprofileNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ProfileCompilerCommand can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testXprofileGetWhenSet(toolchain: KotlinToolchains) {
        val expectedProfileCommand = ProfileCompilerCommand(
            profilerPath = workingDirectory.resolve("path/to/libasyncProfiler.so"),
            command = "event=cpu,interval=1ms,threads,start",
            outputDir = workingDirectory.resolve("path/to/snapshots")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PROFILE] = expectedProfileCommand
        }.build()

        val actualProfileCommand = jvmOperation.compilerArguments[X_PROFILE]

        assertEquals(expectedProfileCommand.profilerPath, actualProfileCommand?.profilerPath)
        assertEquals(expectedProfileCommand.command, actualProfileCommand?.command)
        assertEquals(expectedProfileCommand.outputDir, actualProfileCommand?.outputDir)
    }

    @DisplayName("ProfileCompilerCommand has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testXprofileGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val profileCompilerCommand = jvmOperation.compilerArguments[X_PROFILE]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(profileCompilerCommand)
        )
    }

    @DisplayName("Raw argument strings '-Xprofile=<value>' are converted to ProfileCompilerCommand")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsXprofileConversion(toolchain: KotlinToolchains) {
        val expectedProfileCommand = ProfileCompilerCommand(
            profilerPath = workingDirectory.resolve("path/to/libasyncProfiler.so"),
            command = "event=cpu,interval=1ms,threads,start",
            outputDir = workingDirectory.resolve("path/to/snapshots")
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedProfileCommand),
                toolchain.getCompilerVersion()
            )
        )
        val actualProfileCommand = operation.compilerArguments[X_PROFILE]

        assertEquals(expectedProfileCommand.profilerPath, actualProfileCommand?.profilerPath)
        assertEquals(expectedProfileCommand.command, actualProfileCommand?.command)
        assertEquals(expectedProfileCommand.outputDir, actualProfileCommand?.outputDir)
    }

    @DisplayName("ProfileCompilerCommand has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsXprofile(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_PROFILE])
        )
    }

    override fun expectedArgumentStringsFor(value: String?, compilerVersion: String): List<String> {
        if (value == null || value == getDefaultValueString(compilerVersion)) {
            return emptyList()
        }

        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: ProfileCompilerCommand?): String? {
        if (argument == null) return null

        return argument.profilerPath.toFile().absolutePath + File.pathSeparator + argument.command + File.pathSeparator + argument.outputDir.toFile().absolutePath
    }
}