/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JAVA_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class JavaSourceRootsConversionTest : BaseArgumentTest<Array<String>>("Xjava-source-roots") {

    @DisplayName("JavaSourceRoots is converted to '-Xjava-source-roots' argument")
    @Test
    fun testJavaSourceRootsToArgumentString() {
        val javaSourceRoots = arrayOf(
            workingDirectory.resolve("path/to/java/src1").absolutePathString(),
            workingDirectory.resolve("path/to/java/src2").absolutePathString(),
            workingDirectory.resolve("path/to/java/src3").absolutePathString()
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JAVA_SOURCE_ROOTS] = javaSourceRoots
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(javaSourceRoots)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xjava-source-roots' has the default value when JavaSourceRoots is not set")
    @Test
    fun testJavaSourceRootsNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JavaSourceRoots can be set and retrieved")
    @Test
    fun testJavaSourceRootsGetWhenSet() {
        val expectedJavaSourceRoots = arrayOf(
            workingDirectory.resolve("path/to/java/src1").absolutePathString(),
            workingDirectory.resolve("path/to/java/src2").absolutePathString(),
            workingDirectory.resolve("path/to/java/src3").absolutePathString()
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JAVA_SOURCE_ROOTS] = expectedJavaSourceRoots
        }

        val actualJavaSourceRoots = jvmOperation.compilerArguments[X_JAVA_SOURCE_ROOTS]

        assertArrayEquals(expectedJavaSourceRoots, actualJavaSourceRoots)
    }

    @DisplayName("JavaSourceRoots has the default value when not set")
    @Test
    fun testJavaSourceRootsGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val javaSourceRoots = jvmOperation.compilerArguments[X_JAVA_SOURCE_ROOTS]

        assertEquals(
            getDefaultValueString(), getValueString(javaSourceRoots)
        )
    }

    @DisplayName("Raw argument strings '-Xjava-source-roots=<paths>' are converted to JavaSourceRoots")
    @Test
    fun testRawArgumentsJavaSourceRootsConversion() {
        val expectedJavaSourceRoots = arrayOf(
            workingDirectory.resolve("path/to/java/src1").absolutePathString(),
            workingDirectory.resolve("path/to/java/src2").absolutePathString(),
            workingDirectory.resolve("path/to/java/src3").absolutePathString()
        )
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedJavaSourceRoots))
        )

        assertArrayEquals(
            expectedJavaSourceRoots, operation.compilerArguments[X_JAVA_SOURCE_ROOTS]
        )
    }

    @DisplayName("JavaSourceRoots has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsJavaSourceRoots() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_JAVA_SOURCE_ROOTS])
        )
    }

    @DisplayName("JavaSourceRoots of null value is converted to '-Xjava-source-roots' argument")
    @Test
    fun testNullJavaSourceRoots() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JAVA_SOURCE_ROOTS] = null
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

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
