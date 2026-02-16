/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ABI_STABILITY
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AbiStabilityMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AbiStabilityModeConversionTest : BaseArgumentTest<AbiStabilityMode>("Xabi-stability") {

    @DisplayName("AbiStabilityMode is converted to '-Xabi-stability' argument")
    @BtaVersionsOnlyCompilationTest
    fun testAbiStabilityModeToArgumentString(toolchain: KotlinToolchains) {
        for (abiStabilityMode in AbiStabilityMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ABI_STABILITY] = abiStabilityMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(abiStabilityMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xabi-stability' has the default value when AbiStabilityMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testAbiStabilityModeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AbiStabilityMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testAbiStabilityModeGetWhenSet(toolchain: KotlinToolchains) {
        for (abiStabilityMode in AbiStabilityMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ABI_STABILITY] = abiStabilityMode
            }.build()

            val actualAbiStabilityMode = jvmOperation.compilerArguments[X_ABI_STABILITY]

            assertEquals(abiStabilityMode, actualAbiStabilityMode)
        }
    }

    @DisplayName("AbiStabilityMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testAbiStabilityModeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val abiStabilityMode = jvmOperation.compilerArguments[X_ABI_STABILITY]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(abiStabilityMode)
        )
    }

    @DisplayName("Raw argument strings '-Xabi-stability=<value>' are converted to AbiStabilityMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsAbiStabilityConversion(toolchain: KotlinToolchains) {
        for (abiStabilityMode in AbiStabilityMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(abiStabilityMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                abiStabilityMode,
                operation.compilerArguments[X_ABI_STABILITY]
            )
        }
    }

    @DisplayName("AbiStabilityMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsAbiStability(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_ABI_STABILITY])
        )
    }

    override fun expectedArgumentStringsFor(value: String?, compilerVersion: String): List<String> {
        if (value == null || value == getDefaultValueString(compilerVersion)) {
            return emptyList()
        }

        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: AbiStabilityMode?): String? = argument?.stringValue
}
