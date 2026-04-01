/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_STRING_CONCAT
import org.jetbrains.kotlin.buildtools.api.arguments.enums.StringConcatMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class StringConcatModeConversionTest : BaseArgumentTest<StringConcatMode>("Xstring-concat") {

    @DisplayName("StringConcatMode is converted to '-Xstring-concat' argument")
    @BtaVersionsOnlyCompilationTest
    fun testStringConcatModeToArgumentString(toolchain: KotlinToolchains) {
        for (stringConcatMode in StringConcatMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_STRING_CONCAT] = stringConcatMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(stringConcatMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xstring-concat' has the default value when StringConcatMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testStringConcatModeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("StringConcatMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testStringConcatModeGetWhenSet(toolchain: KotlinToolchains) {
        for (expectedStringConcatMode in StringConcatMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_STRING_CONCAT] = expectedStringConcatMode
            }.build()

            val actualStringConcatMode = jvmOperation.compilerArguments[X_STRING_CONCAT]

            assertEquals(expectedStringConcatMode, actualStringConcatMode)
        }
    }

    @DisplayName("StringConcatMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testStringConcatModeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val stringConcatMode = jvmOperation.compilerArguments[X_STRING_CONCAT]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(stringConcatMode)
        )
    }

    @DisplayName("Raw argument strings '-Xstring-concat=<mode>' are converted to StringConcatMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsStringConcatModeConversion(toolchain: KotlinToolchains) {
        for (expectedStringConcatMode in StringConcatMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedStringConcatMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                expectedStringConcatMode,
                operation.compilerArguments[X_STRING_CONCAT]
            )
        }
    }

    @DisplayName("StringConcatMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsStringConcatMode(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_STRING_CONCAT])
        )
    }

    @DisplayName("Raw argument with non-existent StringConcatMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidStringConcatModeConversionFails(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_STRING_CONCAT]
        }

        assertEquals("Unknown -Xstring-concat value: non-existent-value", exception.message)
    }

    @DisplayName("StringConcatMode of null value is converted to '-Xstring-concat' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullStringConcatMode(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_STRING_CONCAT] = null
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

    override fun getValueString(argument: StringConcatMode?): String? = argument?.stringValue
}
