/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class VerbosePhasesConversionTest : BaseArgumentTest<Array<String>>("Xverbose-phases") {

    @DisplayName("VerbosePhases is converted to '-Xverbose-phases' argument")
    @Test
    fun testVerbosePhasesToArgumentString() {
        val phases = arrayOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_VERBOSE_PHASES] = phases
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xverbose-phases' has the default value when VerbosePhases is not set")
    @Test
    fun testVerbosePhasesNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("VerbosePhases can be set and retrieved")
    @Test
    fun testVerbosePhasesGetWhenSet() {
        val expectedPhases = arrayOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_VERBOSE_PHASES] = expectedPhases
        }

        val actualPhases = jvmOperation.compilerArguments[X_VERBOSE_PHASES]

        assertArrayEquals(expectedPhases, actualPhases)
    }

    @DisplayName("VerbosePhases has the default value when not set")
    @Test
    fun testVerbosePhasesGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val phases = jvmOperation.compilerArguments[X_VERBOSE_PHASES]

        assertEquals(
            getDefaultValueString(), getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-Xverbose-phases=<value>' are converted to VerbosePhases")
    @Test
    fun testRawArgumentsVerbosePhasesConversion() {
        val expectedPhases = arrayOf("phase1", "phase2", "phase3")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedPhases))
        )

        assertArrayEquals(
            expectedPhases, operation.compilerArguments[X_VERBOSE_PHASES]
        )
    }

    @DisplayName("VerbosePhases has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsVerbosePhases() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_VERBOSE_PHASES])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
