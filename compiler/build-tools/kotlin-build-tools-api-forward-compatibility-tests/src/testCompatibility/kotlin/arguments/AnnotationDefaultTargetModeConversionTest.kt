/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AnnotationDefaultTargetModeConversionTest : BaseArgumentTest<String>("Xannotation-default-target") {

    @DisplayName("AnnotationDefaultTargetMode is converted to '-Xannotation-default-target' argument")
    @Test
    fun testAnnotationDefaultTargetModeToArgumentString() {
        for (annotationDefaultTargetMode in listOf("first-only", "first-only-warn", "param-property")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ANNOTATION_DEFAULT_TARGET] = annotationDefaultTargetMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(annotationDefaultTargetMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xannotation-default-target' has the default value when AnnotationDefaultTargetMode is not set")
    @Test
    fun testAnnotationDefaultTargetModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AnnotationDefaultTargetMode can be set and retrieved")
    @Test
    fun testAnnotationDefaultTargetModeGetWhenSet() {
        for (annotationDefaultTargetMode in listOf("first-only", "first-only-warn", "param-property")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ANNOTATION_DEFAULT_TARGET] = annotationDefaultTargetMode
            }

            val actualAnnotationDefaultTargetMode = jvmOperation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET]

            assertEquals(annotationDefaultTargetMode, actualAnnotationDefaultTargetMode)
        }
    }

    @DisplayName("AnnotationDefaultTargetMode has the default value when not set")
    @Test
    fun testAnnotationDefaultTargetModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val annotationDefaultTargetMode = jvmOperation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET]

        assertEquals(
            getDefaultValueString(),
            getValueString(annotationDefaultTargetMode)
        )
    }

    @DisplayName("Raw argument strings '-Xannotation-default-target=<value>' are converted to AnnotationDefaultTargetMode")
    @Test
    fun testRawArgumentsAbiStabilityConversion() {
        for (annotationDefaultTargetMode in listOf("first-only", "first-only-warn", "param-property")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(annotationDefaultTargetMode),
                )
            )

            assertEquals(
                annotationDefaultTargetMode,
                operation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET]
            )
        }
    }

    @DisplayName("AnnotationDefaultTargetMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsAbiStability() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET])
        )
    }

    @DisplayName("Raw argument with non-existent AnnotationDefaultTargetMode value fails conversion")
    @Test
    fun testInvalidAnnotationDefaultTargetModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals(
            "Incorrect value for argument '-Xannotation-default-target'. " +
                    "Actual value: 'non-existent-value', but allowed values: 'first-only-warn', 'param-property', 'first-only'.",
            exception.message
        )
    }

    @DisplayName("Setting non-existent AnnotationDefaultTargetMode value directly fails")
    @Test
    fun testInvalidAnnotationDefaultTargetModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET] = "non-existent-value"
        }

        assertEquals("Unknown -Xannotation-default-target value: non-existent-value", exception.message)
    }

    @DisplayName("AnnotationDefaultTargetMode of null value is converted to '-Xannotation-default-target' argument")
    @Test
    fun testNullAnnotationDefaultTargetMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ANNOTATION_DEFAULT_TARGET] = null
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
