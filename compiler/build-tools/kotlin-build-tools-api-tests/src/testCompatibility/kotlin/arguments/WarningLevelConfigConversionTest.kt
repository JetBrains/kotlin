/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WarningLevel
import org.jetbrains.kotlin.buildtools.api.arguments.types.WarningLevelConfig
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class WarningLevelConfigConversionTest : BaseArgumentTest<List<WarningLevelConfig>>("Xwarning-level") {

    @DisplayName("WarningLevelConfig is converted to '-Xwarning-level' argument")
    @BtaVersionsOnlyCompilationTest
    fun testWarningLevelConfigToArgumentString(toolchain: KotlinToolchains) {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val configs = listOf(
            WarningLevelConfig("DEPRECATION", WarningLevel.ERROR),
            WarningLevelConfig("UNUSED_VARIABLE", WarningLevel.DISABLED),
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

    @DisplayName("'-Xwarning-level' has the default value when WarningLevelConfig is not set")
    @BtaVersionsOnlyCompilationTest
    fun testWarningLevelConfigNotSetByDefault(toolchain: KotlinToolchains) {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("WarningLevelConfig can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testWarningLevelConfigGetWhenSet(toolchain: KotlinToolchains) {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val expectedConfigs = listOf(
            WarningLevelConfig("DEPRECATION", WarningLevel.ERROR),
            WarningLevelConfig("UNUSED_VARIABLE", WarningLevel.DISABLED),
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_WARNING_LEVEL] = expectedConfigs
        }.build()

        val actualConfigs = jvmOperation.compilerArguments[X_WARNING_LEVEL]

        assertListEquals(expectedConfigs, actualConfigs)
    }

    @DisplayName("WarningLevelConfig has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testWarningLevelConfigGetWhenNull(toolchain: KotlinToolchains) {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val configs = jvmOperation.compilerArguments[X_WARNING_LEVEL]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(configs)
        )
    }

    @DisplayName("Raw argument strings '-Xwarning-level=<value>' are converted to WarningLevelConfig")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsWarningLevelConfigConversion(toolchain: KotlinToolchains) {
        assumeWarningLevelSupported(toolchain.getCompilerVersion())
        val expectedConfigs = listOf(
            WarningLevelConfig("DEPRECATION", WarningLevel.ERROR),
            WarningLevelConfig("UNUSED_VARIABLE", WarningLevel.DISABLED),
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedConfigs),
                toolchain.getCompilerVersion()
            )
        )

        assertListEquals(
            expectedConfigs,
            operation.compilerArguments[X_WARNING_LEVEL]
        )
    }

    @DisplayName("WarningLevelConfig has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsWarningLevelConfig(toolchain: KotlinToolchains) {
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

    override fun getValueString(argument: List<WarningLevelConfig>?): String? =
        argument?.joinToString(",") { "${it.warningName}:${it.level.stringValue}" }

    private fun assertListEquals(expectedList: List<WarningLevelConfig>, actualList: List<WarningLevelConfig>) {
        assertEquals(expectedList.size, actualList.size)

        expectedList.forEachIndexed { index, expected ->
            val actual = actualList[index]

            assertEquals(expected.warningName, actual.warningName)
            assertEquals(expected.level.stringValue, actual.level.stringValue)
        }
    }

    private fun assumeWarningLevelSupported(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_WARNING_LEVEL.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_WARNING_LEVEL.availableSinceVersion}"
        )
    }
}
