/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class KotlinHomeConversionTest : BaseArgumentTest<Path>("kotlin-home") {

    @DisplayName("KotlinHome is converted to '-kotlin-home' argument")
    @BtaVersionsOnlyCompilationTest
    fun testKotlinHomeToArgumentString(toolchain: KotlinToolchains) {
        val kotlinHomePath = workingDirectory.resolve("path/to/kotlin")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[KOTLIN_HOME] = kotlinHomePath
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(kotlinHomePath), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-kotlin-home' has the default value when KotlinHome is not set")
    @BtaVersionsOnlyCompilationTest
    fun testKotlinHomeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("KotlinHome can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testKotlinHomeGetWhenSet(toolchain: KotlinToolchains) {
        val expectedKotlinHome = workingDirectory.resolve("path/to/kotlin")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[KOTLIN_HOME] = expectedKotlinHome
        }.build()

        val actualKotlinHome = jvmOperation.compilerArguments[KOTLIN_HOME]

        assertEquals(expectedKotlinHome, actualKotlinHome)
    }

    @DisplayName("KotlinHome has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testKotlinHomeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val kotlinHome = jvmOperation.compilerArguments[KOTLIN_HOME]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(kotlinHome)
        )
    }

    @DisplayName("Raw argument strings '-kotlin-home <path>' are converted to KotlinHome")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsKotlinHomeConversion(toolchain: KotlinToolchains) {
        val expectedKotlinHomePath = workingDirectory.resolve("path/to/kotlin")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedKotlinHomePath),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedKotlinHomePath,
            operation.compilerArguments[KOTLIN_HOME]
        )
    }

    @DisplayName("KotlinHome has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsKotlinHome(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[KOTLIN_HOME])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: Path?): String? = argument?.toFile()?.absolutePath
}
