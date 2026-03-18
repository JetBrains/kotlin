/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AssertionsModeConversionTest : BaseArgumentTest<String>("Xassertions") {

    @DisplayName("AssertionsMode is converted to '-Xassertions' argument")
    @Test
    fun testAssertionsModeToArgumentString() {
        for (assertionsMode in listOf("always-enable", "always-disable", "jvm", "legacy")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ASSERTIONS] = assertionsMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(assertionsMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xassertions' has the default value when AssertionsMode is not set")
    @Test
    fun testAssertionsModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AssertionsMode can be set and retrieved")
    @Test
    fun testAssertionsModeGetWhenSet() {
        for (assertionsMode in listOf("always-enable", "always-disable", "jvm", "legacy")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ASSERTIONS] = assertionsMode
            }

            val actualAssertionsMode = jvmOperation.compilerArguments[X_ASSERTIONS]

            assertEquals(assertionsMode, actualAssertionsMode)
        }
    }

    @DisplayName("AssertionsMode has the default value when not set")
    @Test
    fun testAssertionsModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val assertionsMode = jvmOperation.compilerArguments[X_ASSERTIONS]

        assertEquals(
            getDefaultValueString(),
            getValueString(assertionsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xassertions <value>' are converted to AssertionsMode")
    @Test
    fun testRawArgumentsAssertionsModeConversion() {
        for (assertionsMode in listOf("always-enable", "always-disable", "jvm", "legacy")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(assertionsMode)
                )
            )

            assertEquals(
                assertionsMode,
                operation.compilerArguments[X_ASSERTIONS]
            )
        }
    }

    @DisplayName("AssertionsMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsAssertionsMode() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_ASSERTIONS])
        )
    }

    @DisplayName("Raw argument with non-existent AssertionsMode value fails conversion")
    @Test
    fun testInvalidAssertionsModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xassertions value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent AssertionsMode value directly fails")
    @Test
    fun testInvalidAssertionsModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_ASSERTIONS] = "non-existent-value"
        }

        assertEquals("Unknown -Xassertions value: non-existent-value", exception.message)
    }

    @DisplayName("AssertionsMode of null value is converted to '-Xassertions' argument")
    @Test
    fun testNullAssertionsMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ASSERTIONS] = null
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
