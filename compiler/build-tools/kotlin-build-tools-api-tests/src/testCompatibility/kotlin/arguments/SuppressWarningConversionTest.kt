/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class SuppressWarningConversionTest : BaseArgumentTest<List<String>>("Xsuppress-warning") {

    @DisplayName("SuppressWarning is converted to '-Xsuppress-warning' argument")
    @BtaVersionsOnlyCompilationTest
    fun testSuppressWarningToArgumentString(toolchain: KotlinToolchains) {
        assumeSuppressWarningSupported(toolchain.getCompilerVersion())
        val phases = listOf("warning1", "warning2", "warning3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SUPPRESS_WARNING] = phases
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xsuppress-warning' has the default value when SuppressWarning is not set")
    @BtaVersionsOnlyCompilationTest
    fun testSuppressWarningNotSetByDefault(toolchain: KotlinToolchains) {
        assumeSuppressWarningSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("SuppressWarning can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testSuppressWarningGetWhenSet(toolchain: KotlinToolchains) {
        assumeSuppressWarningSupported(toolchain.getCompilerVersion())
        val expectedPhases = listOf("warning1", "warning2", "warning3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SUPPRESS_WARNING] = expectedPhases
        }.build()

        val actualPhases = jvmOperation.compilerArguments[X_SUPPRESS_WARNING]

        assertEquals(expectedPhases, actualPhases)
    }

    @DisplayName("SuppressWarning has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testSuppressWarningGetWhenNull(toolchain: KotlinToolchains) {
        assumeSuppressWarningSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val phases = jvmOperation.compilerArguments[X_SUPPRESS_WARNING]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-Xsuppress-warning=<value>' are converted to SuppressWarning")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsSuppressWarningConversion(toolchain: KotlinToolchains) {
        assumeSuppressWarningSupported(toolchain.getCompilerVersion())
        val expectedPhases = listOf("warning1", "warning2", "warning3")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedPhases),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedPhases,
            operation.compilerArguments[X_SUPPRESS_WARNING]
        )
    }

    @DisplayName("SuppressWarning has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsSuppressWarning(toolchain: KotlinToolchains) {
        assumeSuppressWarningSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_SUPPRESS_WARNING])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<String>?): String? = argument?.joinToString(",")

    private fun assumeSuppressWarningSupported(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_SUPPRESS_WARNING.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_SUPPRESS_WARNING.availableSinceVersion}"
        )
    }
}
