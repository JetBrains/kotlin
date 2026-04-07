/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.AllJvmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.InvalidRawValueJvmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.JvmArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.NullableJvmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

internal class JvmCompilerArgumentConversionTest : BaseCompilationTest() {

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument is converted to a raw argument")
    fun <T> JvmArgumentConfiguration<T>.testBtaArgumentToArgumentString() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val jvmOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[argumentKey] = value
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(value)),
                actualArgumentStrings,
            )
        }
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument has the default value when BTA argument is not set")
    fun <T> JvmArgumentConfiguration<T>.testBtaArgumentNotSetByDefault() {
        assumeArgumentSupported()
        val jvmOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument can be set and retrieved")
    fun <T> JvmArgumentConfiguration<T>.testBtaArgumentGetWhenSet() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val jvmOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[argumentKey] = value
            }.build()

            val actualValue = jvmOperation.compilerArguments[argumentKey]

            assertEquals(value, actualValue)
        }
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when not set")
    fun <T> JvmArgumentConfiguration<T>.testBtaArgumentGetWhenNull() {
        assumeArgumentSupported()
        val jvmOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val value = jvmOperation.compilerArguments[argumentKey]

        assertEquals(
            getDefaultValueString(), getValueString(value)
        )
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument strings are converted to BTA argument")
    fun <T> JvmArgumentConfiguration<T>.testRawArgumentStringsConversion() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val operation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments.applyArgumentStrings(
                    expectedArgumentStringsFor(
                        getValueString(value),
                    )
                )
            }.build()

            assertEquals(value, operation.compilerArguments[argumentKey])
        }
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when no raw arguments are applied")
    fun <T> JvmArgumentConfiguration<T>.testNoRawArgumentStrings() {
        assumeArgumentSupported()
        val operation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments.applyArgumentStrings(listOf())
        }.build()

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[argumentKey])
        )
    }

    @InvalidRawValueJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument with non-existent BTA argument value fails conversion")
    fun <T> JvmArgumentConfiguration<T>.testInvalidRawArgumentConversionFails() {
        assumeArgumentSupported()
        for (invalidValue in invalidRawValues) {
            assertThrows<CompilerArgumentsParseException> {
                kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                    compilerArguments.applyArgumentStrings(
                        expectedArgumentStringsFor(invalidValue)
                    )
                    compilerArguments[argumentKey]
                }.build()
            }
        }
    }

    @NullableJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument of null value is converted to raw argument")
    fun <T> JvmArgumentConfiguration<T?>.testNullBtaArgument() {
        assumeArgumentSupported()
        val jvmOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[argumentKey] = null
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    private fun JvmArgumentConfiguration<*>.assumeArgumentSupported() {
        assumeTrue(
            kotlinToolchain.getCompilerVersion() >= argumentKey.availableSinceVersion.toString(),
            "Test requires compiler version >= ${argumentKey.availableSinceVersion}"
        )
    }
}
