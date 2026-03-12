/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERIFY_IR
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class VerifyIrModeConversionTest : BaseArgumentTest<VerifyIrMode>("Xverify-ir") {

    @DisplayName("VerifyIrMode is converted to '-Xverify-ir' argument")
    @BtaVersionsOnlyCompilationTest
    fun testVerifyIrModeToArgumentString(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        for (verifyIrMode in VerifyIrMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_VERIFY_IR] = verifyIrMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(verifyIrMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xverify-ir' has the default value when VerifyIrMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testVerifyIrModeNotSetByDefault(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("VerifyIrMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testVerifyIrModeGetWhenSet(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        for (verifyIrMode in VerifyIrMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_VERIFY_IR] = verifyIrMode
            }.build()

            val actualVerifyIrMode = jvmOperation.compilerArguments[X_VERIFY_IR]

            assertEquals(verifyIrMode, actualVerifyIrMode)
        }
    }

    @DisplayName("VerifyIrMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testVerifyIrModeGetWhenNull(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val verifyIrMode = jvmOperation.compilerArguments[X_VERIFY_IR]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(verifyIrMode)
        )
    }

    @DisplayName("Raw argument strings '-Xverify-ir=<value>' are converted to VerifyIrMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsVerifyIrConversion(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        for (verifyIrMode in VerifyIrMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(verifyIrMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                verifyIrMode,
                operation.compilerArguments[X_VERIFY_IR]
            )
        }
    }

    @DisplayName("VerifyIrMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsVerifyIr(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_VERIFY_IR])
        )
    }

    @DisplayName("Raw argument with non-existent VerifyIrMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidVerifyIrModeConversionFails(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_VERIFY_IR]
        }

        assertEquals("Unknown -Xverify-ir value: non-existent-value", exception.message)
    }

    @DisplayName("VerifyIrMode of null value is converted to '-Xverify-ir' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullVerifyIrMode(toolchain: KotlinToolchains) {
        assumeVerifyIr(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_VERIFY_IR] = null
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: VerifyIrMode?): String? = argument?.stringValue

    private fun assumeVerifyIr(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_VERIFY_IR.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_VERIFY_IR.availableSinceVersion}"
        )
    }
}
