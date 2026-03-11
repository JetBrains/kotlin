/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class SuppressWarningConversionTest : BaseArgumentTest<Array<String>>("Xsuppress-warning") {

    @DisplayName("SuppressWarning is converted to '-Xsuppress-warning' argument")
    @Test
    fun testSuppressWarningToArgumentString() {
        val phases = arrayOf("warning1", "warning2", "warning3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SUPPRESS_WARNING] = phases
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xsuppress-warning' has the default value when SuppressWarning is not set")
    @Test
    fun testSuppressWarningNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("SuppressWarning can be set and retrieved")
    @Test
    fun testSuppressWarningGetWhenSet() {
        val expectedPhases = arrayOf("warning1", "warning2", "warning3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SUPPRESS_WARNING] = expectedPhases
        }

        val actualPhases = jvmOperation.compilerArguments[X_SUPPRESS_WARNING]

        assertArrayEquals(expectedPhases, actualPhases)
    }

    @DisplayName("SuppressWarning has the default value when not set")
    @Test
    fun testSuppressWarningGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val phases = jvmOperation.compilerArguments[X_SUPPRESS_WARNING]

        assertEquals(
            getDefaultValueString(), getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-Xsuppress-warning=<value>' are converted to SuppressWarning")
    @Test
    fun testRawArgumentsSuppressWarningConversion() {
        val expectedPhases = arrayOf("warning1", "warning2", "warning3")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedPhases))
        )

        assertArrayEquals(
            expectedPhases, operation.compilerArguments[X_SUPPRESS_WARNING]
        )
    }

    @DisplayName("SuppressWarning has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsSuppressWarning() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_SUPPRESS_WARNING])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
