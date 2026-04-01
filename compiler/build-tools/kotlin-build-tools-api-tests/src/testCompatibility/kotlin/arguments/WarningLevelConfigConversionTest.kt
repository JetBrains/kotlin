/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class WarningLevelConversionTest : BaseArgumentTest<List<WarningLevel>>("Xwarning-level") {

    private lateinit var toolchain: KotlinToolchains

    @BeforeEach
    fun setup() {
        toolchain = KotlinToolchains.loadImplementation(btaClassloader)
    }

    @DisplayName("WarningLevel is converted to '-Xwarning-level' argument")
    @Test
    fun testWarningLevelToArgumentString() {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val configs = listOf(
            WarningLevel("DEPRECATION", WarningLevel.Severity.ERROR),
            WarningLevel("UNUSED_VARIABLE", WarningLevel.Severity.DISABLED),
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WARNING_LEVEL] = configs
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(configs), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xwarning-level' has the default value when WarningLevel is not set")
    @Test
    fun testWarningLevelNotSetByDefault() {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("WarningLevel can be set and retrieved")
    @Test
    fun testWarningLevelGetWhenSet() {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val expectedConfigs = listOf(
            WarningLevel("DEPRECATION", WarningLevel.Severity.ERROR),
            WarningLevel("UNUSED_VARIABLE", WarningLevel.Severity.DISABLED),
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WARNING_LEVEL] = expectedConfigs
        }.build()

        val actualConfigs = jvmOperation.compilerArguments[X_WARNING_LEVEL]

        assertEquals(expectedConfigs, actualConfigs)
    }

    @DisplayName("WarningLevel has the default value when not set")
    @Test
    fun testWarningLevelGetWhenNull() {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val configs = jvmOperation.compilerArguments[X_WARNING_LEVEL]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(configs)
        )
    }

    @DisplayName("Raw argument strings '-Xwarning-level=<value>' are converted to WarningLevel")
    @Test
    fun testRawArgumentsWarningLevelConversion() {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val expectedConfigs = listOf(
            WarningLevel("DEPRECATION", WarningLevel.Severity.ERROR),
            WarningLevel("UNUSED_VARIABLE", WarningLevel.Severity.DISABLED),
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedConfigs),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedConfigs,
            operation.compilerArguments[X_WARNING_LEVEL]
        )
    }

    @DisplayName("WarningLevel has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsWarningLevel() {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_WARNING_LEVEL])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<WarningLevel>?): String? =
        argument?.joinToString(",") { "${it.warningName}:${it.severity.stringValue}" }

    private fun assumeWarningLevelSupported(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_WARNING_LEVEL.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_WARNING_LEVEL.availableSinceVersion}"
        )
    }
}
