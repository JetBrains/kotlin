/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP_BEFORE
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class PhasesToDumpBeforeConversionTest : BaseArgumentTest<Array<String>>("Xphases-to-dump-before") {

    @DisplayName("PhasesToDumpBefore is converted to '-Xphases-to-dump-before' argument")
    @Test
    fun testPhasesToDumpBeforeToArgumentString() {
        val phases = arrayOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PHASES_TO_DUMP_BEFORE] = phases
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xphases-to-dump-before' has the default value when PhasesToDumpBefore is not set")
    @Test
    fun testPhasesToDumpBeforeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("PhasesToDumpBefore can be set and retrieved")
    @Test
    fun testPhasesToDumpBeforeGetWhenSet() {
        val expectedPhases = arrayOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PHASES_TO_DUMP_BEFORE] = expectedPhases
        }

        val actualPhases = jvmOperation.compilerArguments[X_PHASES_TO_DUMP_BEFORE]

        assertArrayEquals(expectedPhases, actualPhases)
    }

    @DisplayName("PhasesToDumpBefore has the default value when not set")
    @Test
    fun testPhasesToDumpBeforeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val phases = jvmOperation.compilerArguments[X_PHASES_TO_DUMP_BEFORE]

        assertEquals(
            getDefaultValueString(), getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-Xphases-to-dump-before=<value>' are converted to PhasesToDumpBefore")
    @Test
    fun testRawArgumentsPhasesToDumpBeforeConversion() {
        val expectedPhases = arrayOf("phase1", "phase2", "phase3")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedPhases))
        )

        assertArrayEquals(
            expectedPhases, operation.compilerArguments[X_PHASES_TO_DUMP_BEFORE]
        )
    }

    @DisplayName("PhasesToDumpBefore has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsPhasesToDumpBefore() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_PHASES_TO_DUMP_BEFORE])
        )
    }

    @DisplayName("PhasesToDumpBefore of null value is converted to '-Xphases-to-dump-before' argument")
    @Test
    fun testNullPhasesToDumpBefore() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PHASES_TO_DUMP_BEFORE] = null
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

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
