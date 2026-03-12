/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths


@OptIn(ExperimentalCompilerArgument::class)
internal class JvmDefaultModeConversionTest : BaseArgumentTest<String>("jvm-default") {

    @DisplayName("JvmDefaultMode is converted to '-jvm-default' argument")
    @Test
    fun testJvmDefaultModeToArgumentString() {
        for (jvmDefaultMode in listOf("enable", "no-compatibility", "disable")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[JVM_DEFAULT] = jvmDefaultMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(jvmDefaultMode)),
                actualArgumentStrings,
                "Failed for JvmDefaultMode.${jvmDefaultMode}"
            )
        }
    }

    @DisplayName("'-jvm-default' has the default value when JvmDefaultMode is not set")
    @Test
    fun testJvmDefaultModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JvmDefaultMode can be set and retrieved")
    @Test
    fun testJvmDefaultModeGetWhenSet() {
        for (jvmDefaultMode in listOf("enable", "no-compatibility", "disable")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[JVM_DEFAULT] = jvmDefaultMode
            }

            val actualJvmDefaultMode = jvmOperation.compilerArguments[JVM_DEFAULT]

            assertEquals(jvmDefaultMode, actualJvmDefaultMode, "Failed for JvmDefaultMode.$jvmDefaultMode")
        }
    }

    @DisplayName("JvmDefaultMode has the default value when not set")
    @Test
    fun testJvmDefaultModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val jvmDefaultMode = jvmOperation.compilerArguments[JVM_DEFAULT]

        assertEquals(
            getDefaultValueString(), getValueString(jvmDefaultMode)
        )
    }

    @DisplayName("Raw argument strings '-jvm-default <value>' are converted to JvmDefaultMode")
    @Test
    fun testRawArgumentsJvmDefaultConversion() {
        for (jvmDefaultMode in listOf("enable", "no-compatibility", "disable")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(jvmDefaultMode)
                )
            )

            assertEquals(
                jvmDefaultMode,
                operation.compilerArguments[JVM_DEFAULT],
                "Failed to convert '-jvm-default $jvmDefaultMode' to JvmDefaultMode.$jvmDefaultMode"
            )
        }
    }

    @DisplayName("JvmDefaultMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsJvmDefault() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[JVM_DEFAULT])
        )
    }

    @DisplayName("Raw argument with non-existent JvmDefaultMode value fails conversion")
    @Test
    fun testInvalidJvmDefaultModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -jvm-default value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent JvmDefaultMode value directly fails")
    @Test
    fun testInvalidJvmDefaultModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[JVM_DEFAULT] = "non-existent-value"
        }

        assertEquals("Unknown -jvm-default value: non-existent-value", exception.message)
    }

    @DisplayName("JvmDefaultMode of null value is converted to '-jvm-default' argument")
    @Test
    fun testNullJvmDefaultMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[JVM_DEFAULT] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: String?): String? = argument
}
