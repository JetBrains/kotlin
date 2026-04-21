/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.*
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
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
        for (value in argumentRawValues) {
            val operation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments.applyArgumentStrings(expectedArgumentStringsFor(value))
            }.build()

            assertEquals(value, getValueString(operation.compilerArguments[argumentKey]))
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

    @InvalidArgumentValueJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument with non-existent argument value fails conversion")
    fun <T> JvmArgumentConfiguration<T>.testInvalidArgumentConversionFails() {
        assumeArgumentSupported()
        for (invalidValue in invalidArgumentValues) {
            assertThrows<CompilerArgumentsParseException> {
                kotlinToolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                    compilerArguments[argumentKey] = invalidValue
                }.build()
            }
        }
    }

    @InvalidRawValueJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument with non-existent BTA argument value is rejected at configuration or execution time")
    fun <T> JvmArgumentConfiguration<T>.testInvalidRawArgumentConversionFails() {
        assumeArgumentSupported()
        // Old BTA versions throw from applyArgumentStrings; new versions store the error and report it during executeOperation
        val kotlinToolingVersion = KotlinToolingVersion(kotlinToolchain.getCompilerVersion())
        assumeTrue { kotlinToolingVersion < KotlinToolingVersion(2, 4, 20, "snapshot") }

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

    @InvalidRawValueJvmCompilerArgumentsBtaV2StrategyAgnosticTest
    @DisplayName("Raw argument with non-existent BTA argument value is rejected at compilation time")
    fun testInvalidRawArgumentCompilationFails(config: Pair<JvmArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>) {
        val (argumentConfig, strategyConfig) = config
        argumentConfig.assumeArgumentSupported()
        // Old BTA versions throw from applyArgumentStrings; new versions store the error and report it during executeOperation
        val kotlinToolingVersion = KotlinToolingVersion(argumentConfig.kotlinToolchain.getCompilerVersion())
        assumeTrue { kotlinToolingVersion >= KotlinToolingVersion(2, 4, 20, "snapshot") }

        project(strategyConfig) {
            val module = module("jvm-module-1")
            for (invalidValue in argumentConfig.invalidRawValues) {
                module.compile(compilationConfigAction = {
                    it.compilerArguments.applyArgumentStrings(argumentConfig.expectedArgumentStringsFor(invalidValue))
                }) {
                    expectFail()
                    assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
                }
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
