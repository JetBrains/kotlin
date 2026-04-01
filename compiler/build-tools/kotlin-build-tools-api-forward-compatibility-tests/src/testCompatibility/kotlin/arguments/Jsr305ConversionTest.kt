/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSR305
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class Jsr305ConversionTest : BaseArgumentTest<Array<String>>("Xjsr305") {

    @DisplayName("Jsr305 is converted to '-Xjsr305' argument")
    @Test
    fun testJsr305ToArgumentString() {
        val jsr305 = arrayOf("strict", "under-migration:warn", "@com.example.Nullable:ignore")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JSR305] = jsr305
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(jsr305)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xjsr305' has the default value when Jsr305 is not set")
    @Test
    fun testJsr305NotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("Jsr305 can be set and retrieved")
    @Test
    fun testJsr305GetWhenSet() {
        val expectedJsr305 = arrayOf("strict", "under-migration:warn", "@com.example.Nullable:ignore")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JSR305] = expectedJsr305
        }

        val actualJsr305 = jvmOperation.compilerArguments[X_JSR305]

        assertArrayEquals(expectedJsr305, actualJsr305)
    }

    @DisplayName("Jsr305 has the default value when not set")
    @Test
    fun testJsr305GetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val jsr305 = jvmOperation.compilerArguments[X_JSR305]

        assertEquals(
            getDefaultValueString(), getValueString(jsr305)
        )
    }

    @DisplayName("Raw argument strings '-Xjsr305=<value>' are converted to Jsr305")
    @Test
    fun testRawArgumentsJsr305Conversion() {
        val expectedJsr305 = arrayOf("strict", "under-migration:warn", "@com.example.Nullable:ignore")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedJsr305))
        )

        assertArrayEquals(
            expectedJsr305, operation.compilerArguments[X_JSR305]
        )
    }

    @DisplayName("Jsr305 has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsJsr305() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_JSR305])
        )
    }

    @DisplayName("Jsr305 of null value is converted to '-Xjsr305' argument")
    @Test
    fun testNullJsr305() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JSR305] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    @DisplayName("Raw argument with non-existent Jsr305 mode value fails conversion")
    @Test
    fun testInvalidJsr305ModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("@com.example.Nullable:non-existent-mode")
            )
        }

        assertEquals("Unknown -Xjsr305 mode: non-existent-mode", exception.message)
    }

    @DisplayName("Setting non-existent Jsr305 mode value directly fails")
    @Test
    fun testInvalidJsr305ModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_JSR305] = arrayOf("@com.example.Nullable:non-existent-mode")
        }

        assertEquals("Unknown -Xjsr305 mode: non-existent-mode", exception.message)
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
