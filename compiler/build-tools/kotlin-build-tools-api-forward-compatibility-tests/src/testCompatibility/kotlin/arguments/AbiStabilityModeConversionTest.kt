/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ABI_STABILITY
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AbiStabilityModeConversionTest : BaseArgumentTest<String>("Xabi-stability") {

    @DisplayName("AbiStabilityMode is converted to '-Xabi-stability' argument")
    @Test
    fun testAbiStabilityModeToArgumentString() {
        for (abiStabilityMode in listOf("stable", "unstable")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ABI_STABILITY] = abiStabilityMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(abiStabilityMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xabi-stability' has the default value when AbiStabilityMode is not set")
    @Test
    fun testAbiStabilityModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AbiStabilityMode can be set and retrieved")
    @Test
    fun testAbiStabilityModeGetWhenSet() {
        for (abiStabilityMode in listOf("stable", "unstable")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ABI_STABILITY] = abiStabilityMode
            }

            val actualAbiStabilityMode = jvmOperation.compilerArguments[X_ABI_STABILITY]

            assertEquals(abiStabilityMode, actualAbiStabilityMode)
        }
    }

    @DisplayName("AbiStabilityMode has the default value when not set")
    @Test
    fun testAbiStabilityModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val abiStabilityMode = jvmOperation.compilerArguments[X_ABI_STABILITY]

        assertEquals(
            getDefaultValueString(),
            getValueString(abiStabilityMode)
        )
    }

    @DisplayName("Raw argument strings '-Xabi-stability=<value>' are converted to AbiStabilityMode")
    @Test
    fun testRawArgumentsAbiStabilityConversion() {
        for (abiStabilityMode in listOf("stable", "unstable")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(abiStabilityMode),
                )
            )

            assertEquals(
                abiStabilityMode,
                operation.compilerArguments[X_ABI_STABILITY]
            )
        }
    }

    @DisplayName("AbiStabilityMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsAbiStability() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_ABI_STABILITY])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: String?): String? = argument
}
