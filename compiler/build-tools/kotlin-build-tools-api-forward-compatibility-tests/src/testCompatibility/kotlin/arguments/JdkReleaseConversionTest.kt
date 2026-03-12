/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JDK_RELEASE
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class JdkReleaseConversionTest : BaseArgumentTest<String>("Xjdk-release") {

    @DisplayName("JdkRelease is converted to '-Xjdk-release' argument")
    @Test
    fun testJdkReleaseToArgumentString() {
        for (jdkRelease in getJdkReleaseEntries()) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JDK_RELEASE] = jdkRelease
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(jdkRelease)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xjdk-release' has the default value when JdkRelease is not set")
    @Test
    fun testJdkReleaseNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JdkRelease can be set and retrieved")
    @Test
    fun testJdkReleaseGetWhenSet() {
        for (jdkRelease in getJdkReleaseEntries()) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JDK_RELEASE] = jdkRelease
            }

            val actualJdkRelease = jvmOperation.compilerArguments[X_JDK_RELEASE]

            assertEquals(jdkRelease, actualJdkRelease)
        }
    }

    @DisplayName("JdkRelease has the default value when not set")
    @Test
    fun testJdkReleaseGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val jdkRelease = jvmOperation.compilerArguments[X_JDK_RELEASE]

        assertEquals(
            getDefaultValueString(),
            getValueString(jdkRelease)
        )
    }

    @DisplayName("Raw argument strings '-Xjdk-release <value>' are converted to JdkRelease")
    @Test
    fun testRawArgumentsJdkReleaseConversion() {
        for (jdkRelease in getJdkReleaseEntries()) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(jdkRelease)
                )
            )

            assertEquals(
                jdkRelease,
                operation.compilerArguments[X_JDK_RELEASE]
            )
        }
    }

    @DisplayName("JdkRelease has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsJdkRelease() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_JDK_RELEASE])
        )
    }

    @DisplayName("Raw argument with non-existent JdkRelease value fails conversion")
    @Test
    fun testInvalidJdkReleaseConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xjdk-release value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent JdkRelease value directly fails")
    @Test
    fun testInvalidJdkReleaseDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_JDK_RELEASE] = "non-existent-value"
        }

        assertEquals("Unknown -Xjdk-release value: non-existent-value", exception.message)
    }

    @DisplayName("JdkRelease of null value is converted to '-Xjdk-release' argument")
    @Test
    fun testNullJdkRelease() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JDK_RELEASE] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: String?): String? = argument

    private fun getJdkReleaseEntries() = listOf(
        "1.6", "1.7", "1.8", "8", "9", "10", "11", "12", "13", "14",
        "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"
    )
}
