/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_LAMBDAS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class LambdasModeConversionTest : BaseArgumentTest<String>("Xlambdas") {

    @DisplayName("LambdasMode is converted to '-Xlambdas' argument")
    @Test
    fun testLambdasModeToArgumentString() {
        for (lambdasMode in listOf("indy", "class")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_LAMBDAS] = lambdasMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(lambdasMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xlambdas' has the default value when LambdasMode is not set")
    @Test
    fun testLambdasModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("LambdasMode can be set and retrieved")
    @Test
    fun testLambdasModeGetWhenSet() {
        for (expectedLambdasMode in listOf("indy", "class")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_LAMBDAS] = expectedLambdasMode
            }

            val actualLambdasMode = jvmOperation.compilerArguments[X_LAMBDAS]

            assertEquals(expectedLambdasMode, actualLambdasMode)
        }
    }

    @DisplayName("LambdasMode has the default value when not set")
    @Test
    fun testLambdasModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val lambdasMode = jvmOperation.compilerArguments[X_LAMBDAS]

        assertEquals(
            getDefaultValueString(),
            getValueString(lambdasMode)
        )
    }

    @DisplayName("Raw argument strings '-Xlambdas=<mode>' are converted to LambdasMode")
    @Test
    fun testRawArgumentsLambdasModeConversion() {
        for (expectedLambdasMode in listOf("indy", "class")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedLambdasMode),

                    )
            )

            assertEquals(
                expectedLambdasMode,
                operation.compilerArguments[X_LAMBDAS]
            )
        }
    }

    @DisplayName("LambdasMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsLambdasMode() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_LAMBDAS])
        )
    }

    @DisplayName("Raw argument with non-existent LambdasMode value fails conversion")
    @Test
    fun testInvalidLambdasModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xlambdas value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent LambdasMode value directly fails")
    @Test
    fun testInvalidLambdasModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_LAMBDAS] = "non-existent-value"
        }

        assertEquals("Unknown -Xlambdas value: non-existent-value", exception.message)
    }

    @DisplayName("LambdasMode of null value is converted to '-Xlambdas' argument")
    @Test
    fun testNullLambdasMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_LAMBDAS] = null
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
