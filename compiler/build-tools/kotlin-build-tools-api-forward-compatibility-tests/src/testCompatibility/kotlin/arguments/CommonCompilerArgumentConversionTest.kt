/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.common.AllCommonCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.common.CommonArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.common.InvalidValueCommonCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class CommonCompilerArgumentConversionTest : BaseCompilationTest() {

    @AllCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument is converted to a raw argument")
    fun <T> CommonArgumentConfiguration<T>.testBtaArgumentToArgumentString() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[argumentKey] = value
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(value)),
                actualArgumentStrings,
            )
        }
    }

    @AllCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument has the default value when BTA argument is not set")
    fun <T> CommonArgumentConfiguration<T>.testBtaArgumentNotSetByDefault() {
        assumeArgumentSupported()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @AllCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument can be set and retrieved")
    fun <T> CommonArgumentConfiguration<T>.testBtaArgumentGetWhenSet() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[argumentKey] = value
            }

            val actualValue = jvmOperation.compilerArguments[argumentKey]

            assertEquals(getValueString(value), getValueString(actualValue))
        }
    }

    @AllCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when not set")
    fun <T> CommonArgumentConfiguration<T>.testBtaArgumentGetWhenNull() {
        assumeArgumentSupported()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val value = jvmOperation.compilerArguments[argumentKey]

        assertEquals(
            getDefaultValueString(), getValueString(value)
        )
    }

    @AllCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument strings are converted to BTA argument")
    fun <T> CommonArgumentConfiguration<T>.testRawArgumentStringsConversion() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(getValueString(value))
            )

            assertEquals(getValueString(value), getValueString(operation.compilerArguments[argumentKey]))
        }
    }

    @AllCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when no raw arguments are applied")
    fun <T> CommonArgumentConfiguration<T>.testNoRawArgumentStrings() {
        assumeArgumentSupported()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[argumentKey])
        )
    }

    @InvalidValueCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument with non-existent BTA argument value fails conversion")
    fun CommonArgumentConfiguration<String?>.testInvalidRawArgumentConversionFails() {
        assumeArgumentSupported()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(getValueString(invalidValue))
            )
            operation.compilerArguments[argumentKey]
        }
    }

    @DisplayName("Setting non-existent BTA argument value directly fails")
    @InvalidValueCommonCompilerArgumentsWithBtaVersionsTest
    fun CommonArgumentConfiguration<String?>.testInvalidDirectAssignmentFails() {
        assumeArgumentSupported()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(getValueString(invalidValue))
            )
            operation.compilerArguments[argumentKey]
        }
    }

    @AllCommonCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument of null value is converted to raw argument")
    fun <T> CommonArgumentConfiguration<T?>.testNullBtaArgument() {
        assumeArgumentSupported()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[argumentKey] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    private fun CommonArgumentConfiguration<*>.assumeArgumentSupported() {
        val availableSince = argumentKey.availableSinceVersion
        val versionString = "${availableSince.major}.${availableSince.minor}.${availableSince.patch}"

        assumeTrue(
            toolchain.getCompilerVersion() >= versionString, "Test requires compiler version >= $versionString"
        )
    }
}
