/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class KotlinHomeConversionTest : BaseArgumentTest<String>("kotlin-home") {

    @DisplayName("KotlinHome is converted to '-kotlin-home' argument")
    @Test
    fun testKotlinHomeToArgumentString() {
        val kotlinHomePath = workingDirectory.resolve("path/to/kotlin").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[KOTLIN_HOME] = kotlinHomePath
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(kotlinHomePath)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-kotlin-home' has the default value when KotlinHome is not set")
    @Test
    fun testKotlinHomeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("KotlinHome can be set and retrieved")
    @Test
    fun testKotlinHomeGetWhenSet() {
        val expectedKotlinHome = workingDirectory.resolve("path/to/kotlin").absolutePathString()
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[KOTLIN_HOME] = expectedKotlinHome
        }

        val actualKotlinHome = jvmOperation.compilerArguments[KOTLIN_HOME]

        assertEquals(expectedKotlinHome, actualKotlinHome)
    }

    @DisplayName("KotlinHome has the default value when not set")
    @Test
    fun testKotlinHomeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val kotlinHome = jvmOperation.compilerArguments[KOTLIN_HOME]

        assertEquals(
            getDefaultValueString(), getValueString(kotlinHome)
        )
    }

    @DisplayName("Raw argument strings '-kotlin-home <path>' are converted to KotlinHome")
    @Test
    fun testRawArgumentsKotlinHomeConversion() {
        val expectedKotlinHomePath = workingDirectory.resolve("path/to/kotlin").absolutePathString()
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedKotlinHomePath))
        )

        assertEquals(
            expectedKotlinHomePath, operation.compilerArguments[KOTLIN_HOME]
        )
    }

    @DisplayName("KotlinHome has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsKotlinHome() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[KOTLIN_HOME])
        )
    }

    @DisplayName("KotlinHome of null value is converted to '-kotlin-home' argument")
    @Test
    fun testNullKotlinHome() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[KOTLIN_HOME] = null
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
