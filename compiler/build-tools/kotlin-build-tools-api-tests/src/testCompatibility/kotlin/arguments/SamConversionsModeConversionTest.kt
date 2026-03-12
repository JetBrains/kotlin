/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SAM_CONVERSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SamConversionsMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class SamConversionsModeConversionTest() : BaseArgumentTest<SamConversionsMode>("Xsam-conversions") {

    @DisplayName("SamConversionsMode is converted to '-Xsam-conversions' argument")
    @BtaVersionsOnlyCompilationTest
    fun testSamConversionsModeToArgumentString(toolchain: KotlinToolchains) {
        for (samConversionsMode in SamConversionsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_SAM_CONVERSIONS] = samConversionsMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(samConversionsMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xsam-conversions' has the default value when SamConversionsMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testSamConversionsModeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("SamConversionsMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testSamConversionsModeGetWhenSet(toolchain: KotlinToolchains) {
        for (expectedSamConversionsMode in SamConversionsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_SAM_CONVERSIONS] = expectedSamConversionsMode
            }.build()

            val actualSamConversionsMode = jvmOperation.compilerArguments[X_SAM_CONVERSIONS]

            assertEquals(expectedSamConversionsMode, actualSamConversionsMode)
        }
    }

    @DisplayName("SamConversionsMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testSamConversionsModeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val samConversionsMode = jvmOperation.compilerArguments[X_SAM_CONVERSIONS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(samConversionsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xsam-conversions=<mode>' are converted to SamConversionsMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsSamConversionsModeConversion(toolchain: KotlinToolchains) {
        for (expectedSamConversionsMode in SamConversionsMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedSamConversionsMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                expectedSamConversionsMode,
                operation.compilerArguments[X_SAM_CONVERSIONS]
            )
        }
    }

    @DisplayName("SamConversionsMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsSamConversionsMode(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_SAM_CONVERSIONS])
        )
    }

    @DisplayName("Raw argument with non-existent SamConversionsMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidSamConversionsModeConversionFails(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_SAM_CONVERSIONS]
        }

        assertEquals("Unknown -Xsam-conversions value: non-existent-value", exception.message)
    }

    @DisplayName("SamConversionsMode of null value is converted to '-Xsam-conversions' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullSamConversionsMode(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SAM_CONVERSIONS] = null
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: SamConversionsMode?): String? = argument?.stringValue
}
