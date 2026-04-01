/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NAME_BASED_DESTRUCTURING
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class NameBasedDestructuringModeConversionTest : BaseArgumentTest<String>("Xname-based-destructuring") {

    @DisplayName("NameBasedDestructuringMode is converted to '-Xname-based-destructuring' argument")
    @Test
    fun testNameBasedDestructuringModeToArgumentString() {
        for (nameBasedDestructuringMode in listOf("only-syntax", "name-mismatch", "complete")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_NAME_BASED_DESTRUCTURING] = nameBasedDestructuringMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(nameBasedDestructuringMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xname-based-destructuring' has the default value when NameBasedDestructuringMode is not set")
    @Test
    fun testNameBasedDestructuringModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("NameBasedDestructuringMode can be set and retrieved")
    @Test
    fun testNameBasedDestructuringModeGetWhenSet() {
        for (nameBasedDestructuringMode in listOf("only-syntax", "name-mismatch", "complete")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_NAME_BASED_DESTRUCTURING] = nameBasedDestructuringMode
            }

            val actualNameBasedDestructuringMode = jvmOperation.compilerArguments[X_NAME_BASED_DESTRUCTURING]

            assertEquals(nameBasedDestructuringMode, actualNameBasedDestructuringMode)
        }
    }

    @DisplayName("NameBasedDestructuringMode has the default value when not set")
    @Test
    fun testNameBasedDestructuringModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val nameBasedDestructuringMode = jvmOperation.compilerArguments[X_NAME_BASED_DESTRUCTURING]

        assertEquals(
            getDefaultValueString(),
            getValueString(nameBasedDestructuringMode)
        )
    }

    @DisplayName("Raw argument strings '-Xname-based-destructuring=<value>' are converted to NameBasedDestructuringMode")
    @Test
    fun testRawArgumentsNameBasedDestructuringConversion() {
        for (nameBasedDestructuringMode in listOf("only-syntax", "name-mismatch", "complete")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(nameBasedDestructuringMode),
                )
            )

            assertEquals(
                nameBasedDestructuringMode,
                operation.compilerArguments[X_NAME_BASED_DESTRUCTURING]
            )
        }
    }

    @DisplayName("NameBasedDestructuringMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsNameBasedDestructuring() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_NAME_BASED_DESTRUCTURING])
        )
    }

    @DisplayName("Raw argument with non-existent NameBasedDestructuringMode value fails conversion")
    @Test
    fun testInvalidNameBasedDestructuringModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals(
            "Incorrect value for argument '-Xname-based-destructuring'. " +
                    "Actual value: 'non-existent-value', but allowed values: 'only-syntax', 'name-mismatch', 'complete'.",
            exception.message
        )
    }

    @DisplayName("Setting non-existent NameBasedDestructuringMode value directly fails")
    @Test
    fun testInvalidNameBasedDestructuringModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_NAME_BASED_DESTRUCTURING] = "non-existent-value"
        }

        assertEquals("Unknown -Xname-based-destructuring value: non-existent-value", exception.message)
    }

    @DisplayName("NameBasedDestructuringMode of null value is converted to '-Xname-based-destructuring' argument")
    @Test
    fun testNullNameBasedDestructuringMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_NAME_BASED_DESTRUCTURING] = null
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
