/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SAM_CONVERSIONS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class SamConversionsModeConversionTest() : BaseArgumentTest<String>("Xsam-conversions") {

    @DisplayName("SamConversionsMode is converted to '-Xsam-conversions' argument")
    @Test
    fun testSamConversionsModeToArgumentString() {
        for (samConversionsMode in listOf("class", "indy")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_SAM_CONVERSIONS] = samConversionsMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(samConversionsMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xsam-conversions' has the default value when SamConversionsMode is not set")
    @Test
    fun testSamConversionsModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("SamConversionsMode can be set and retrieved")
    @Test
    fun testSamConversionsModeGetWhenSet() {
        for (expectedSamConversionsMode in listOf("class", "indy")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_SAM_CONVERSIONS] = expectedSamConversionsMode
            }

            val actualSamConversionsMode = jvmOperation.compilerArguments[X_SAM_CONVERSIONS]

            assertEquals(expectedSamConversionsMode, actualSamConversionsMode)
        }
    }

    @DisplayName("SamConversionsMode has the default value when not set")
    @Test
    fun testSamConversionsModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val samConversionsMode = jvmOperation.compilerArguments[X_SAM_CONVERSIONS]

        assertEquals(
            getDefaultValueString(),
            getValueString(samConversionsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xsam-conversions=<mode>' are converted to SamConversionsMode")
    @Test
    fun testRawArgumentsSamConversionsModeConversion() {
        for (expectedSamConversionsMode in listOf("class", "indy")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedSamConversionsMode),

                    )
            )

            assertEquals(
                expectedSamConversionsMode,
                operation.compilerArguments[X_SAM_CONVERSIONS]
            )
        }
    }

    @DisplayName("SamConversionsMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsSamConversionsMode() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_SAM_CONVERSIONS])
        )
    }

    @DisplayName("Raw argument with non-existent SamConversionsMode value fails conversion")
    @Test
    fun testInvalidSamConversionsModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xsam-conversions value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent SamConversionsMode value directly fails")
    @Test
    fun testInvalidSamConversionsModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_SAM_CONVERSIONS] = "non-existent-value"
        }

        assertEquals("Unknown -Xsam-conversions value: non-existent-value", exception.message)
    }

    @DisplayName("SamConversionsMode of null value is converted to '-Xsam-conversions' argument")
    @Test
    fun testNullSamConversionsMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SAM_CONVERSIONS] = null
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
