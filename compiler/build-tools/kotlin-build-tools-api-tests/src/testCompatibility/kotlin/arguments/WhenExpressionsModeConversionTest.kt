/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_WHEN_EXPRESSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WhenExpressionsMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class WhenExpressionsModeConversionTest : BaseArgumentTest<WhenExpressionsMode>("Xwhen-expressions") {

    @DisplayName("WhenExpressionsMode is converted to '-Xwhen-expressions' argument")
    @BtaVersionsOnlyCompilationTest
    fun testWhenExpressionsModeToArgumentString(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        for (whenExpressionsMode in WhenExpressionsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_WHEN_EXPRESSIONS] = whenExpressionsMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(whenExpressionsMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xwhen-expressions' has the default value when WhenExpressionsMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testWhenExpressionsModeNotSetByDefault(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("WhenExpressionsMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testWhenExpressionsModeGetWhenSet(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        for (expectedWhenExpressionsMode in WhenExpressionsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_WHEN_EXPRESSIONS] = expectedWhenExpressionsMode
            }.build()

            val actualWhenExpressionsMode = jvmOperation.compilerArguments[X_WHEN_EXPRESSIONS]

            assertEquals(expectedWhenExpressionsMode, actualWhenExpressionsMode)
        }
    }

    @DisplayName("WhenExpressionsMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testWhenExpressionsModeGetWhenNull(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val whenExpressionsMode = jvmOperation.compilerArguments[X_WHEN_EXPRESSIONS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(whenExpressionsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xwhen-expressions=<mode>' are converted to WhenExpressionsMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsWhenExpressionsModeConversion(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        for (expectedWhenExpressionsMode in WhenExpressionsMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedWhenExpressionsMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                expectedWhenExpressionsMode,
                operation.compilerArguments[X_WHEN_EXPRESSIONS]
            )
        }
    }

    @DisplayName("WhenExpressionsMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsWhenExpressionsMode(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_WHEN_EXPRESSIONS])
        )
    }

    @DisplayName("Raw argument with non-existent WhenExpressionsMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidWhenExpressionsModeConversionFails(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_WHEN_EXPRESSIONS]
        }

        assertEquals("Unknown -Xwhen-expressions value: non-existent-value", exception.message)
    }

    @DisplayName("WhenExpressionsMode of null value is converted to '-Xwhen-expressions' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullWhenExpressionsMode(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WHEN_EXPRESSIONS] = null
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

    override fun getValueString(argument: WhenExpressionsMode?): String? = argument?.stringValue

    private fun assumeArgumentSupported(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_WHEN_EXPRESSIONS.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_WHEN_EXPRESSIONS.availableSinceVersion}"
        )
    }
}
