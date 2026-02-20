/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JDK_HOME
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class JdkHomeConversionTest() : BaseArgumentTest<Path>("jdk-home") {

    @DisplayName("JdkHome is converted to '-jdk-home' argument")
    @BtaVersionsOnlyCompilationTest
    fun testJdkHomeToArgumentString(toolchain: KotlinToolchains) {
        val jdkHomePath = workingDirectory.resolve("path/to/jdk")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[JDK_HOME] = jdkHomePath
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(jdkHomePath), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-jdk-home' has the default value when JdkHome is not set")
    @BtaVersionsOnlyCompilationTest
    fun testJdkHomeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JdkHome can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testJdkHomeGetWhenSet(toolchain: KotlinToolchains) {
        val expectedJdkHome = workingDirectory.resolve("path/to/jdk")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[JDK_HOME] = expectedJdkHome
        }.build()

        val actualJdkHome = jvmOperation.compilerArguments[JDK_HOME]

        assertEquals(expectedJdkHome, actualJdkHome)
    }

    @DisplayName("JdkHome has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testJdkHomeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val jdkHome = jvmOperation.compilerArguments[JDK_HOME]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(jdkHome)
        )
    }

    @DisplayName("Raw argument strings '-jdk-home <path>' are converted to JdkHome")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsJdkHomeConversion(toolchain: KotlinToolchains) {
        val expectedJdkHomePath = workingDirectory.resolve("path/to/jdk")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedJdkHomePath),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedJdkHomePath,
            operation.compilerArguments[JDK_HOME]
        )
    }

    @DisplayName("JdkHome has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsJdkHome(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[JDK_HOME])
        )
    }

    override fun expectedArgumentStringsFor(value: String?, compilerVersion: String): List<String> {
        if (value == null || value == getDefaultValueString(compilerVersion)) {
            return emptyList()
        }

        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: Path?): String? = argument?.toFile()?.absolutePath
}