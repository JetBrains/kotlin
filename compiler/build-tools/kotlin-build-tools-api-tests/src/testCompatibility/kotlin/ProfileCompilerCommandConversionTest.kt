/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_PROFILE
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class ProfileCompilerCommandConversionTest : BaseCompilationTest() {

    @DisplayName("Test ProfileCompilerCommand is converted to a compiler argument correctly")
    @DefaultStrategyAgnosticCompilationTest
    fun testXprofileToArgumentString(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val profileCompilerCommand = ProfileCompilerCommand(
            profilerPath = workingDirectory.resolve("path/to/libasyncProfiler.so"),
            command = "event=cpu,interval=1ms,threads,start",
            outputDir = workingDirectory.resolve("/path/to/snapshots")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PROFILE] = profileCompilerCommand
        }.build()

        val valueString =
            jvmOperation.compilerArguments.toArgumentStrings().first { it.startsWith("-Xprofile=") }.removePrefix("-Xprofile=")
        val (profilerPath, command, outputDir) = valueString.split(File.pathSeparator)

        assertEquals(profileCompilerCommand.profilerPath, Paths.get(profilerPath))
        assertEquals(profileCompilerCommand.command, command)
        assertEquals(profileCompilerCommand.outputDir, Paths.get(outputDir))
    }

    @DisplayName("Test that ProfileCompilerCommand is not set by default")
    @DefaultStrategyAgnosticCompilationTest
    fun testXprofileNotSetByDefault(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val valueString =
            jvmOperation.compilerArguments.toArgumentStrings().firstOrNull { it.startsWith("-Xprofile=") }

        assertEquals(null, valueString)
    }

    @DisplayName("Test ProfileCompilerCommand is set and retrieved correctly")
    @DefaultStrategyAgnosticCompilationTest
    fun testXprofileGetWhenSet(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val profilerPath = workingDirectory.resolve("path/to/libasyncProfiler.so")
        val command = "event=cpu,interval=1ms,threads,start"
        val outputDir = workingDirectory.resolve("path/to/snapshots")
        val profileCompilerCommand = ProfileCompilerCommand(
            profilerPath = profilerPath,
            command = command,
            outputDir = outputDir
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PROFILE] = profileCompilerCommand
        }.build()

        val profileCommand = jvmOperation.compilerArguments[X_PROFILE]

        assertEquals(profilerPath, profileCommand?.profilerPath)
        assertEquals(command, profileCommand?.command)
        assertEquals(outputDir, profileCommand?.outputDir)
    }

    @DisplayName("Test ProfileCompilerCommand is retrieved correctly when it is not set")
    @DefaultStrategyAgnosticCompilationTest
    fun testXprofileGetWhenNull(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val profileCommand = jvmOperation.compilerArguments[X_PROFILE]

        assertEquals(null, profileCommand)
    }
}