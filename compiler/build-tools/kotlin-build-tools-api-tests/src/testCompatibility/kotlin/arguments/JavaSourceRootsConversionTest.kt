/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JAVA_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class JavaSourceRootsConversionTest : BaseArgumentTest<List<Path>>("Xjava-source-roots") {

    @DisplayName("JavaSourceRoots is converted to '-Xjava-source-roots' argument")
    @BtaVersionsOnlyCompilationTest
    fun testJavaSourceRootsToArgumentString(toolchain: KotlinToolchains) {
        val javaSourceRoots = listOf(
            workingDirectory.resolve("path/to/java/src1"),
            workingDirectory.resolve("path/to/java/src2"),
            workingDirectory.resolve("path/to/java/src3")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JAVA_SOURCE_ROOTS] = javaSourceRoots
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(javaSourceRoots), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xjava-source-roots' has the default value when JavaSourceRoots is not set")
    @BtaVersionsOnlyCompilationTest
    fun testJavaSourceRootsNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JavaSourceRoots can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testJavaSourceRootsGetWhenSet(toolchain: KotlinToolchains) {
        val expectedJavaSourceRoots = listOf(
            workingDirectory.resolve("path/to/java/src1"),
            workingDirectory.resolve("path/to/java/src2"),
            workingDirectory.resolve("path/to/java/src3")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JAVA_SOURCE_ROOTS] = expectedJavaSourceRoots
        }.build()

        val actualJavaSourceRoots = jvmOperation.compilerArguments[X_JAVA_SOURCE_ROOTS]

        assertEquals(expectedJavaSourceRoots, actualJavaSourceRoots)
    }

    @DisplayName("JavaSourceRoots has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testJavaSourceRootsGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val javaSourceRoots = jvmOperation.compilerArguments[X_JAVA_SOURCE_ROOTS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(javaSourceRoots)
        )
    }

    @DisplayName("Raw argument strings '-Xjava-source-roots=<paths>' are converted to JavaSourceRoots")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsJavaSourceRootsConversion(toolchain: KotlinToolchains) {
        val expectedJavaSourceRoots = listOf(
            workingDirectory.resolve("path/to/java/src1"),
            workingDirectory.resolve("path/to/java/src2"),
            workingDirectory.resolve("path/to/java/src3")
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedJavaSourceRoots),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedJavaSourceRoots,
            operation.compilerArguments[X_JAVA_SOURCE_ROOTS]
        )
    }

    @DisplayName("JavaSourceRoots has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsJavaSourceRoots(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_JAVA_SOURCE_ROOTS])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<Path>?): String? =
        argument?.joinToString(",") { it.toFile().absolutePath }
}
