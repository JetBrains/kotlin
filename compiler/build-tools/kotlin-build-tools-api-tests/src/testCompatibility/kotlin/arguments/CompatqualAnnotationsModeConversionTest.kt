/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.CompatqualAnnotationsMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class CompatqualAnnotationsModeConversionTest :
    BaseArgumentTest<CompatqualAnnotationsMode>("Xsupport-compatqual-checker-framework-annotations") {

    @DisplayName("CompatqualAnnotationsMode is converted to '-Xsupport-compatqual-checker-framework-annotations' argument")
    @BtaVersionsOnlyCompilationTest
    fun testCompatqualAnnotationsModeToArgumentString(toolchain: KotlinToolchains) {
        for (compatqualAnnotationsMode in CompatqualAnnotationsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = compatqualAnnotationsMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(compatqualAnnotationsMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xsupport-compatqual-checker-framework-annotations' has the default value when CompatqualAnnotationsMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testCompatqualAnnotationsModeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("CompatqualAnnotationsMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testCompatqualAnnotationsModeGetWhenSet(toolchain: KotlinToolchains) {
        for (expectedCompatqualAnnotationsMode in CompatqualAnnotationsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = expectedCompatqualAnnotationsMode
            }.build()

            val actualCompatqualAnnotationsMode = jvmOperation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS]

            assertEquals(expectedCompatqualAnnotationsMode, actualCompatqualAnnotationsMode)
        }
    }

    @DisplayName("CompatqualAnnotationsMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testCompatqualAnnotationsModeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val compatqualAnnotationsMode = jvmOperation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(compatqualAnnotationsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xsupport-compatqual-checker-framework-annotations=<mode>' are converted to CompatqualAnnotationsMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsCompatqualAnnotationsModeConversion(toolchain: KotlinToolchains) {
        for (expectedCompatqualAnnotationsMode in CompatqualAnnotationsMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedCompatqualAnnotationsMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                expectedCompatqualAnnotationsMode,
                operation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS]
            )
        }
    }

    @DisplayName("CompatqualAnnotationsMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsCompatqualAnnotationsMode(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS])
        )
    }

    @DisplayName("Raw argument with non-existent CompatqualAnnotationsMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidCompatqualAnnotationsModeConversionFails(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS]
        }

        assertEquals("Unknown -Xsupport-compatqual-checker-framework-annotations value: non-existent-value", exception.message)
    }

    @DisplayName("CompatqualAnnotationsMode of null value is converted to '-Xsupport-compatqual-checker-framework-annotations' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullCompatqualAnnotationsMode(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = null
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

    override fun getValueString(argument: CompatqualAnnotationsMode?): String? = argument?.stringValue
}
