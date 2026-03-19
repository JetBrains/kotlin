/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class WarningLevelConfigConversionTest : BaseArgumentTest<Array<String>>("Xwarning-level") {

    @DisplayName("WarningLevelConfig is converted to '-Xwarning-level' argument")
    @Test
    fun testWarningLevelConfigToArgumentString() {
        val configs = arrayOf("DEPRECATION:error", "UNUSED_VARIABLE:disabled")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WARNING_LEVEL] = configs
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(configs)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xwarning-level' has the default value when WarningLevelConfig is not set")
    @Test
    fun testWarningLevelConfigNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("WarningLevelConfig can be set and retrieved")
    @Test
    fun testWarningLevelConfigGetWhenSet() {
        val expectedConfigs = arrayOf("DEPRECATION:error", "UNUSED_VARIABLE:disabled")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WARNING_LEVEL] = expectedConfigs
        }

        val actualConfigs = jvmOperation.compilerArguments[X_WARNING_LEVEL]

        assertArrayEquals(expectedConfigs, actualConfigs)
    }

    @DisplayName("WarningLevelConfig has the default value when not set")
    @Test
    fun testWarningLevelConfigGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val configs = jvmOperation.compilerArguments[X_WARNING_LEVEL]

        assertEquals(
            getDefaultValueString(), getValueString(configs)
        )
    }

    @DisplayName("Raw argument strings '-Xwarning-level=<value>' are converted to WarningLevelConfig")
    @Test
    fun testRawArgumentsWarningLevelConfigConversion() {
        val expectedConfigs = arrayOf("DEPRECATION:error", "UNUSED_VARIABLE:disabled")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedConfigs))
        )

        assertArrayEquals(
            expectedConfigs, operation.compilerArguments[X_WARNING_LEVEL]
        )
    }

    @DisplayName("WarningLevelConfig has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsWarningLevelConfig() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_WARNING_LEVEL])
        )
    }

    @DisplayName("WarningLevelConfig of null value is converted to '-Xwarning-level' argument")
    @Test
    fun testNullWarningLevelConfig() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WARNING_LEVEL] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    @DisplayName("Raw argument with non-existent WarningLevel value fails conversion")
    @Test
    fun testInvalidWarningLevelConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("DEPRECATION:non-existent-level")
            )
        }

        assertEquals("Unknown -Xwarning-level level: DEPRECATION:non-existent-level", exception.message)
    }

    @DisplayName("Setting non-existent WarningLevel value directly fails")
    @Test
    fun testInvalidWarningLevelDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_WARNING_LEVEL] = arrayOf("DEPRECATION:non-existent-level")
        }

        assertEquals("Unknown -Xwarning-level level: DEPRECATION:non-existent-level", exception.message)
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
