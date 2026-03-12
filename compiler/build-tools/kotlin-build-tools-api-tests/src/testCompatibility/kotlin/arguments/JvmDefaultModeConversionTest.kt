/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class JvmDefaultModeConversionTest : BaseArgumentTest<JvmDefaultMode>("jvm-default") {

    @DisplayName("JvmDefaultMode is converted to '-jvm-default' argument")
    @BtaVersionsOnlyCompilationTest
    fun testJvmDefaultModeToArgumentString(toolchain: KotlinToolchains) {
        assumeJvmDefaultSupported(toolchain.getCompilerVersion())

        for (jvmDefaultMode in JvmDefaultMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[JVM_DEFAULT] = jvmDefaultMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(jvmDefaultMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
                "Failed for JvmDefaultMode.${jvmDefaultMode.name}"
            )
        }
    }

    @DisplayName("'-jvm-default' has the default value when JvmDefaultMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testJvmDefaultModeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JvmDefaultMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testJvmDefaultModeGetWhenSet(toolchain: KotlinToolchains) {
        assumeJvmDefaultSupported(toolchain.getCompilerVersion())

        for (jvmDefaultMode in JvmDefaultMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[JVM_DEFAULT] = jvmDefaultMode
            }.build()

            val actualJvmDefaultMode = jvmOperation.compilerArguments[JVM_DEFAULT]

            assertEquals(jvmDefaultMode, actualJvmDefaultMode, "Failed for JvmDefaultMode.${jvmDefaultMode.name}")
        }
    }

    @DisplayName("JvmDefaultMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testJvmDefaultModeGetWhenNull(toolchain: KotlinToolchains) {
        assumeJvmDefaultSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val jvmDefaultMode = jvmOperation.compilerArguments[JVM_DEFAULT]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(jvmDefaultMode)
        )
    }

    @DisplayName("Raw argument strings '-jvm-default <value>' are converted to JvmDefaultMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsJvmDefaultConversion(toolchain: KotlinToolchains) {
        assumeJvmDefaultSupported(toolchain.getCompilerVersion())

        for (jvmDefaultMode in JvmDefaultMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(jvmDefaultMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                jvmDefaultMode,
                operation.compilerArguments[JVM_DEFAULT],
                "Failed to convert '-jvm-default ${jvmDefaultMode.stringValue}' to JvmDefaultMode.${jvmDefaultMode.name}"
            )
        }
    }

    @DisplayName("JvmDefaultMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsJvmDefault(toolchain: KotlinToolchains) {
        assumeJvmDefaultSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[JVM_DEFAULT])
        )
    }

    @DisplayName("Raw argument with non-existent JvmDefaultMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidJvmDefaultModeConversionFails(toolchain: KotlinToolchains) {
        assumeJvmDefaultSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[JVM_DEFAULT]
        }

        assertEquals("Unknown -jvm-default value: non-existent-value", exception.message)
    }

    @DisplayName("JvmDefaultMode of null value is converted to '-jvm-default' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullJvmDefaultMode(toolchain: KotlinToolchains) {
        assumeJvmDefaultSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[JVM_DEFAULT] = null
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

    override fun getValueString(argument: JvmDefaultMode?): String? = argument?.stringValue

    private fun assumeJvmDefaultSupported(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= JVM_DEFAULT.availableSinceVersion.toString(),
            "Test requires compiler version >= ${JVM_DEFAULT.availableSinceVersion}"
        )
    }
}