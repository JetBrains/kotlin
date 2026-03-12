/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AssertionsMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AssertionsModeConversionTest : BaseArgumentTest<AssertionsMode>("Xassertions") {

    @DisplayName("AssertionsMode is converted to '-Xassertions' argument")
    @BtaVersionsOnlyCompilationTest
    fun testAssertionsModeToArgumentString(toolchain: KotlinToolchains) {
        for (assertionsMode in AssertionsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ASSERTIONS] = assertionsMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(assertionsMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xassertions' has the default value when AssertionsMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testAssertionsModeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AssertionsMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testAssertionsModeGetWhenSet(toolchain: KotlinToolchains) {
        for (assertionsMode in AssertionsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ASSERTIONS] = assertionsMode
            }.build()

            val actualAssertionsMode = jvmOperation.compilerArguments[X_ASSERTIONS]

            assertEquals(assertionsMode, actualAssertionsMode)
        }
    }

    @DisplayName("AssertionsMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testAssertionsModeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val assertionsMode = jvmOperation.compilerArguments[X_ASSERTIONS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(assertionsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xassertions <value>' are converted to AssertionsMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsAssertionsModeConversion(toolchain: KotlinToolchains) {
        for (assertionsMode in AssertionsMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(assertionsMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                assertionsMode,
                operation.compilerArguments[X_ASSERTIONS]
            )
        }
    }

    @DisplayName("AssertionsMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsAssertionsMode(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_ASSERTIONS])
        )
    }

    @DisplayName("Raw argument with non-existent AssertionsMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidAssertionsModeConversionFails(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_ASSERTIONS]
        }

        assertEquals("Unknown -Xassertions value: non-existent-value", exception.message)
    }

    @DisplayName("AssertionsMode of null value is converted to '-Xassertions' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullAssertionsMode(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ASSERTIONS] = null
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

    override fun getValueString(argument: AssertionsMode?): String? = argument?.stringValue
}
