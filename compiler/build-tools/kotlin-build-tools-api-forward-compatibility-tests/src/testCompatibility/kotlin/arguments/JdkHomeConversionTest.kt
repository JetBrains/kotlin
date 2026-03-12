/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JDK_HOME
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class JdkHomeConversionTest : BaseArgumentTest<String>("jdk-home") {

    @DisplayName("JdkHome is converted to '-jdk-home' argument")
    @Test
    fun testJdkHomeToArgumentString() {
        val jdkHomePath = workingDirectory.resolve("path/to/jdk").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[JDK_HOME] = jdkHomePath
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(jdkHomePath)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-jdk-home' has the default value when JdkHome is not set")
    @Test
    fun testJdkHomeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JdkHome can be set and retrieved")
    @Test
    fun testJdkHomeGetWhenSet() {
        val expectedJdkHome = workingDirectory.resolve("path/to/jdk").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[JDK_HOME] = expectedJdkHome
        }

        val actualJdkHome = jvmOperation.compilerArguments[JDK_HOME]

        assertEquals(expectedJdkHome, actualJdkHome)
    }

    @DisplayName("JdkHome has the default value when not set")
    @Test
    fun testJdkHomeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val jdkHome = jvmOperation.compilerArguments[JDK_HOME]

        assertEquals(
            getDefaultValueString(), getValueString(jdkHome)
        )
    }

    @DisplayName("Raw argument strings '-jdk-home <path>' are converted to JdkHome")
    @Test
    fun testRawArgumentsJdkHomeConversion() {
        val expectedJdkHomePath = workingDirectory.resolve("path/to/jdk").absolutePathString()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedJdkHomePath))
        )

        assertEquals(
            expectedJdkHomePath, operation.compilerArguments[JDK_HOME]
        )
    }

    @DisplayName("JdkHome has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsJdkHome() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[JDK_HOME])
        )
    }

    @DisplayName("JdkHome of null value is converted to '-jdk-home' argument")
    @Test
    fun testNullJdkHome() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[JDK_HOME] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: String?): String? = argument
}