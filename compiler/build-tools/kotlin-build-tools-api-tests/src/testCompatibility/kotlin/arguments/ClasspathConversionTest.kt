/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

internal class ClasspathConversionTest : BaseArgumentTest<List<Path>>("classpath") {

    @DisplayName("Classpath is converted to '-classpath' argument")
    @BtaVersionsOnlyCompilationTest
    fun testClasspathToArgumentString(toolchain: KotlinToolchains) {
        val classpathPaths = listOf(
            workingDirectory.resolve("path/to/lib1.jar"),
            workingDirectory.resolve("path/to/lib2.jar"),
            workingDirectory.resolve("path/to/classes")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[CLASSPATH] = classpathPaths
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(classpathPaths), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-classpath' has the default value when Classpath is not set")
    @BtaVersionsOnlyCompilationTest
    fun testClasspathNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("Classpath can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testClasspathGetWhenSet(toolchain: KotlinToolchains) {
        val expectedClasspath = listOf(
            workingDirectory.resolve("path/to/lib1.jar"),
            workingDirectory.resolve("path/to/lib2.jar"),
            workingDirectory.resolve("path/to/classes")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[CLASSPATH] = expectedClasspath
        }.build()

        val actualClasspath = jvmOperation.compilerArguments[CLASSPATH]

        assertEquals(expectedClasspath, actualClasspath)
    }

    @DisplayName("Classpath has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testClasspathGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val classpath = jvmOperation.compilerArguments[CLASSPATH]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(classpath)
        )
    }

    @DisplayName("Raw argument strings '-classpath <paths>' are converted to Classpath")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsClasspathConversion(toolchain: KotlinToolchains) {
        val expectedClasspathPaths = listOf(
            workingDirectory.resolve("path/to/lib1.jar"),
            workingDirectory.resolve("path/to/lib2.jar"),
            workingDirectory.resolve("path/to/classes")
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedClasspathPaths),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedClasspathPaths,
            operation.compilerArguments[CLASSPATH]
        )
    }

    @DisplayName("Classpath has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsClasspath(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[CLASSPATH])
        )
    }

    @DisplayName("Classpath of null value is converted to '-classpath' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullClasspath(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[CLASSPATH] = null
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: List<Path>?): String? =
        argument?.joinToString(File.pathSeparator) { it.toFile().absolutePath }
}
