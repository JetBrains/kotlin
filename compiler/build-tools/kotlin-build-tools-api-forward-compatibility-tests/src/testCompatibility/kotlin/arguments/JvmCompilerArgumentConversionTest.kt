/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.AllJvmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.InvalidArgumentValueJvmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.InvalidRawValueJvmCompilerArgumentsStrategyAgnosticTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm.JvmArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCompilerArgument::class)
@Suppress("DEPRECATION")
internal class JvmCompilerArgumentConversionTest : BaseCompilationTest() {

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument is converted to a raw argument")
    fun <T> JvmArgumentConfiguration<T>.testBtaArgumentToArgumentString() {
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

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument has the default value when BTA argument is not set")
    fun <T> JvmArgumentConfiguration<T>.testBtaArgumentNotSetByDefault() {
        assumeArgumentSupported()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

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
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[argumentKey] = value
            }

            val actualValue = jvmOperation.compilerArguments[argumentKey]

            assertEquals(getValueString(value), getValueString(actualValue))
        }
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when not set")
    fun <T> JvmArgumentConfiguration<T>.testBtaArgumentGetWhenNull() {
        assumeArgumentSupported()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

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
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(expectedArgumentStringsFor(value))

            assertEquals(value, getValueString(operation.compilerArguments[argumentKey]))
        }
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when no raw arguments are applied")
    fun <T> JvmArgumentConfiguration<T>.testNoRawArgumentStrings() {
        assumeArgumentSupported()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[argumentKey])
        )
    }

    @InvalidRawValueJvmCompilerArgumentsStrategyAgnosticTest
    @DisplayName("Raw argument with non-existent BTA argument value is rejected at compilation time")
    fun testInvalidRawArgumentCompilationFails(config: Pair<JvmArgumentConfiguration<*>, ExecutionPolicy>) {
        val (argumentConfig, executionPolicy) = config
        argumentConfig.assumeArgumentSupported()

        for (invalidValue in argumentConfig.invalidRawValues) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))
            operation.compilerArguments.applyArgumentStrings(argumentConfig.expectedArgumentStringsFor(invalidValue))
            val logger = CapturingLogger()
            val result = toolchain.createBuildSession().use {
                it.executeOperation(operation, executionPolicy, logger)
            }
            assertEquals(CompilationResult.COMPILATION_ERROR, result)
            assertTrue(logger.errors.any { it.contains(invalidValue) }) {
                "Expected error containing '$invalidValue', but errors were: ${logger.errors}"
            }
        }
    }

    @InvalidArgumentValueJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Setting non-existent BTA argument value directly fails")
    fun <T> JvmArgumentConfiguration<T>.testInvalidDirectAssignmentFails() {
        assumeArgumentSupported()
        for (invalidValue in invalidArgumentValues) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            assertThrows<CompilerArgumentsParseException> {
                operation.compilerArguments[argumentKey] = invalidValue
            }
        }
    }

    @AllJvmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument of null value is converted to raw argument")
    fun <T> JvmArgumentConfiguration<T?>.testNullBtaArgument() {
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

    private class CapturingLogger : KotlinLogger {
        override val isDebugEnabled = false
        val errors = CopyOnWriteArrayList<String>()
        override fun debug(msg: String) {}
        override fun error(msg: String, throwable: Throwable?) {
            errors.add(msg)
        }

        override fun info(msg: String) {}
        override fun lifecycle(msg: String) {}
        override fun warn(msg: String, throwable: Throwable?) {}
    }

    private fun JvmArgumentConfiguration<*>.assumeArgumentSupported() {
        val availableSince = argumentKey.availableSinceVersion
        val versionString = "${availableSince.major}.${availableSince.minor}.${availableSince.patch}"

        assumeTrue(
            toolchain.getCompilerVersion() >= versionString, "Test requires compiler version >= $versionString"
        )
    }
}
