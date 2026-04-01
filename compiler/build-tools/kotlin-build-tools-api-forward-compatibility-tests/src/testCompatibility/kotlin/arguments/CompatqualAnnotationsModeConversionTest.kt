/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class CompatqualAnnotationsModeConversionTest :
    BaseArgumentTest<String>("Xsupport-compatqual-checker-framework-annotations") {

    @DisplayName("CompatqualAnnotationsMode is converted to '-Xsupport-compatqual-checker-framework-annotations' argument")
    @Test
    fun testCompatqualAnnotationsModeToArgumentString() {
        for (compatqualAnnotationsMode in listOf("enable", "disable")) {
            val jvmOperation =
                toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                    compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = compatqualAnnotationsMode
                }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(compatqualAnnotationsMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xsupport-compatqual-checker-framework-annotations' has the default value when CompatqualAnnotationsMode is not set")
    @Test
    fun testCompatqualAnnotationsModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("CompatqualAnnotationsMode can be set and retrieved")
    @Test
    fun testCompatqualAnnotationsModeGetWhenSet() {
        for (expectedCompatqualAnnotationsMode in listOf("enable", "disable")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = expectedCompatqualAnnotationsMode
            }

            val actualCompatqualAnnotationsMode = jvmOperation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS]

            assertEquals(expectedCompatqualAnnotationsMode, actualCompatqualAnnotationsMode)
        }
    }

    @DisplayName("CompatqualAnnotationsMode has the default value when not set")
    @Test
    fun testCompatqualAnnotationsModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val compatqualAnnotationsMode = jvmOperation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS]

        assertEquals(
            getDefaultValueString(),
            getValueString(compatqualAnnotationsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xsupport-compatqual-checker-framework-annotations=<mode>' are converted to CompatqualAnnotationsMode")
    @Test
    fun testRawArgumentsCompatqualAnnotationsModeConversion() {
        for (expectedCompatqualAnnotationsMode in listOf("enable", "disable")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(expectedCompatqualAnnotationsMode),

                    )
            )

            assertEquals(
                expectedCompatqualAnnotationsMode,
                operation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS]
            )
        }
    }

    @DisplayName("CompatqualAnnotationsMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsCompatqualAnnotationsMode() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS])
        )
    }

    @DisplayName("Raw argument with non-existent CompatqualAnnotationsMode value fails conversion")
    @Test
    fun testInvalidCompatqualAnnotationsModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xsupport-compatqual-checker-framework-annotations value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent CompatqualAnnotationsMode value directly fails")
    @Test
    fun testInvalidCompatqualAnnotationsModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = "non-existent-value"
        }

        assertEquals("Unknown -Xsupport-compatqual-checker-framework-annotations value: non-existent-value", exception.message)
    }

    @DisplayName("CompatqualAnnotationsMode of null value is converted to '-Xsupport-compatqual-checker-framework-annotations' argument")
    @Test
    fun testNullCompatqualAnnotationsMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: String?): String? = argument
}
