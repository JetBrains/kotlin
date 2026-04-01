/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_STRING_CONCAT
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class StringConcatModeConversionTest : BaseArgumentTest<String>("Xstring-concat") {

    @DisplayName("StringConcatMode is converted to '-Xstring-concat' argument")
    @Test
    fun testStringConcatModeToArgumentString() {
        for (stringConcatMode in listOf("indy-with-constants", "indy", "inline")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_STRING_CONCAT] = stringConcatMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(stringConcatMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xstring-concat' has the default value when StringConcatMode is not set")
    @Test
    fun testStringConcatModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("StringConcatMode can be set and retrieved")
    @Test
    fun testStringConcatModeGetWhenSet() {
        for (expectedStringConcatMode in listOf("indy-with-constants", "indy", "inline")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_STRING_CONCAT] = expectedStringConcatMode
            }

            val actualStringConcatMode = jvmOperation.compilerArguments[X_STRING_CONCAT]

            assertEquals(expectedStringConcatMode, actualStringConcatMode)
        }
    }

    @DisplayName("StringConcatMode has the default value when not set")
    @Test
    fun testStringConcatModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val stringConcatMode = jvmOperation.compilerArguments[X_STRING_CONCAT]

        assertEquals(
            getDefaultValueString(),
            getValueString(stringConcatMode)
        )
    }

    @DisplayName("Raw argument strings '-Xstring-concat=<mode>' are converted to StringConcatMode")
    @Test
    fun testRawArgumentsStringConcatModeConversion() {
        for (expectedStringConcatMode in listOf("indy-with-constants", "indy", "inline")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedStringConcatMode),

                    )
            )

            assertEquals(
                expectedStringConcatMode,
                operation.compilerArguments[X_STRING_CONCAT]
            )
        }
    }

    @DisplayName("StringConcatMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsStringConcatMode() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_STRING_CONCAT])
        )
    }

    @DisplayName("Raw argument with non-existent StringConcatMode value fails conversion")
    @Test
    fun testInvalidStringConcatModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xstring-concat value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent StringConcatMode value directly fails")
    @Test
    fun testInvalidStringConcatModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_STRING_CONCAT] = "non-existent-value"
        }

        assertEquals("Unknown -Xstring-concat value: non-existent-value", exception.message)
    }

    @DisplayName("StringConcatMode of null value is converted to '-Xstring-concat' argument")
    @Test
    fun testNullStringConcatMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_STRING_CONCAT] = null
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
