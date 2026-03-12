/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

internal class ClasspathConversionTest : BaseArgumentTest<String>("classpath") {

    @DisplayName("Classpath is converted to '-classpath' argument")
    @Test
    fun testClasspathToArgumentString() {
        val classpathPaths = classpathStringOf(
            listOf(
                workingDirectory.resolve("path/to/lib1.jar"),
                workingDirectory.resolve("path/to/lib2.jar"),
                workingDirectory.resolve("path/to/classes")
            )
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[CLASSPATH] = classpathPaths
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(classpathPaths)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-classpath' has the default value when Classpath is not set")
    @Test
    fun testClasspathNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("Classpath can be set and retrieved")
    @Test
    fun testClasspathGetWhenSet() {
        val expectedClasspath = classpathStringOf(
            listOf(
                workingDirectory.resolve("path/to/lib1.jar"),
                workingDirectory.resolve("path/to/lib2.jar"),
                workingDirectory.resolve("path/to/classes")
            )
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[CLASSPATH] = expectedClasspath
        }

        val actualClasspath = jvmOperation.compilerArguments[CLASSPATH]

        assertEquals(expectedClasspath, actualClasspath)
    }

    @DisplayName("Classpath has the default value when not set")
    @Test
    fun testClasspathGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val classpath = jvmOperation.compilerArguments[CLASSPATH]

        assertEquals(
            getDefaultValueString(), getValueString(classpath)
        )
    }

    @DisplayName("Raw argument strings '-classpath <paths>' are converted to Classpath")
    @Test
    fun testRawArgumentsClasspathConversion() {
        val expectedClasspathPaths = classpathStringOf(
            listOf(
                workingDirectory.resolve("path/to/lib1.jar"),
                workingDirectory.resolve("path/to/lib2.jar"),
                workingDirectory.resolve("path/to/classes")
            )
        )
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedClasspathPaths))
        )

        assertEquals(
            expectedClasspathPaths, operation.compilerArguments[CLASSPATH]
        )
    }

    @DisplayName("Classpath has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsClasspath() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[CLASSPATH])
        )
    }

    @DisplayName("Classpath of null value is converted to '-classpath' argument")
    @Test
    fun testNullClasspath() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[CLASSPATH] = null
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

    private fun classpathStringOf(paths: List<Path>): String = paths.joinToString(File.pathSeparator) { it.absolutePathString() }
}
