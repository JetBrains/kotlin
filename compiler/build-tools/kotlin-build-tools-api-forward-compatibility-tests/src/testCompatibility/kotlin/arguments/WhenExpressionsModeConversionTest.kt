/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_WHEN_EXPRESSIONS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class WhenExpressionsModeConversionTest : BaseArgumentTest<String>("Xwhen-expressions") {

    @DisplayName("WhenExpressionsMode is converted to '-Xwhen-expressions' argument")
    @Test
    fun testWhenExpressionsModeToArgumentString() {
        for (whenExpressionsMode in listOf("indy", "inline")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_WHEN_EXPRESSIONS] = whenExpressionsMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(whenExpressionsMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xwhen-expressions' has the default value when WhenExpressionsMode is not set")
    @Test
    fun testWhenExpressionsModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("WhenExpressionsMode can be set and retrieved")
    @Test
    fun testWhenExpressionsModeGetWhenSet() {
        for (expectedWhenExpressionsMode in listOf("indy", "inline")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_WHEN_EXPRESSIONS] = expectedWhenExpressionsMode
            }

            val actualWhenExpressionsMode = jvmOperation.compilerArguments[X_WHEN_EXPRESSIONS]

            assertEquals(expectedWhenExpressionsMode, actualWhenExpressionsMode)
        }
    }

    @DisplayName("WhenExpressionsMode has the default value when not set")
    @Test
    fun testWhenExpressionsModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val whenExpressionsMode = jvmOperation.compilerArguments[X_WHEN_EXPRESSIONS]

        assertEquals(
            getDefaultValueString(),
            getValueString(whenExpressionsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xwhen-expressions=<mode>' are converted to WhenExpressionsMode")
    @Test
    fun testRawArgumentsWhenExpressionsModeConversion() {
        for (expectedWhenExpressionsMode in listOf("indy", "inline")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedWhenExpressionsMode),

                    )
            )

            assertEquals(
                expectedWhenExpressionsMode,
                operation.compilerArguments[X_WHEN_EXPRESSIONS]
            )
        }
    }

    @DisplayName("WhenExpressionsMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsWhenExpressionsMode() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_WHEN_EXPRESSIONS])
        )
    }

    @DisplayName("Raw argument with non-existent WhenExpressionsMode value fails conversion")
    @Test
    fun testInvalidWhenExpressionsModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xwhen-expressions value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent WhenExpressionsMode value directly fails")
    @Test
    fun testInvalidWhenExpressionsModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_WHEN_EXPRESSIONS] = "non-existent-value"
        }

        assertEquals("Unknown -Xwhen-expressions value: non-existent-value", exception.message)
    }

    @DisplayName("WhenExpressionsMode of null value is converted to '-Xwhen-expressions' argument")
    @Test
    fun testNullWhenExpressionsMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WHEN_EXPRESSIONS] = null
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
