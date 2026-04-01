/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERIFY_IR
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class VerifyIrModeConversionTest : BaseArgumentTest<String>("Xverify-ir") {

    @DisplayName("VerifyIrMode is converted to '-Xverify-ir' argument")
    @Test
    fun testVerifyIrModeToArgumentString() {
        for (verifyIrMode in listOf("none", "warning", "error")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_VERIFY_IR] = verifyIrMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(verifyIrMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xverify-ir' has the default value when VerifyIrMode is not set")
    @Test
    fun testVerifyIrModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("VerifyIrMode can be set and retrieved")
    @Test
    fun testVerifyIrModeGetWhenSet() {
        for (verifyIrMode in listOf("none", "warning", "error")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_VERIFY_IR] = verifyIrMode
            }

            val actualVerifyIrMode = jvmOperation.compilerArguments[X_VERIFY_IR]

            assertEquals(verifyIrMode, actualVerifyIrMode)
        }
    }

    @DisplayName("VerifyIrMode has the default value when not set")
    @Test
    fun testVerifyIrModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val verifyIrMode = jvmOperation.compilerArguments[X_VERIFY_IR]

        assertEquals(
            getDefaultValueString(),
            getValueString(verifyIrMode)
        )
    }

    @DisplayName("Raw argument strings '-Xverify-ir=<value>' are converted to VerifyIrMode")
    @Test
    fun testRawArgumentsVerifyIrConversion() {
        for (verifyIrMode in listOf("none", "warning", "error")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(verifyIrMode),
                )
            )

            assertEquals(
                verifyIrMode,
                operation.compilerArguments[X_VERIFY_IR]
            )
        }
    }

    @DisplayName("VerifyIrMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsVerifyIr() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_VERIFY_IR])
        )
    }

    @DisplayName("Raw argument with non-existent VerifyIrMode value fails conversion")
    @Test
    fun testInvalidVerifyIrModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xverify-ir value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent VerifyIrMode value directly fails")
    @Test
    fun testInvalidVerifyIrModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_VERIFY_IR] = "non-existent-value"
        }

        assertEquals("Unknown -Xverify-ir value: non-existent-value", exception.message)
    }

    @DisplayName("VerifyIrMode of null value is converted to '-Xverify-ir' argument")
    @Test
    fun testNullVerifyIrMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_VERIFY_IR] = null
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
