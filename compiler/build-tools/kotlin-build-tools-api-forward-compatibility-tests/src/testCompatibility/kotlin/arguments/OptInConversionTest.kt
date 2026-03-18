/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class OptInConversionTest : BaseArgumentTest<Array<String>>("opt-in") {

    @DisplayName("OptIn is converted to '-opt-in' argument")
    @Test
    fun testOptInToArgumentString() {
        val phases = arrayOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[OPT_IN] = phases
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-opt-in' has the default value when OptIn is not set")
    @Test
    fun testOptInNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("OptIn can be set and retrieved")
    @Test
    fun testOptInGetWhenSet() {
        val expectedPhases = arrayOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[OPT_IN] = expectedPhases
        }

        val actualPhases = jvmOperation.compilerArguments[OPT_IN]

        assertArrayEquals(expectedPhases, actualPhases)
    }

    @DisplayName("OptIn has the default value when not set")
    @Test
    fun testOptInGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val phases = jvmOperation.compilerArguments[OPT_IN]

        assertEquals(
            getDefaultValueString(), getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-opt-in=<value>' are converted to OptIn")
    @Test
    fun testRawArgumentsOptInConversion() {
        val expectedPhases = arrayOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedPhases))
        )

        assertArrayEquals(
            expectedPhases, operation.compilerArguments[OPT_IN]
        )
    }

    @DisplayName("OptIn has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsOptIn() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[OPT_IN])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
