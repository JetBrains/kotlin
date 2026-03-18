/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class NullabilityAnnotationsConversionTest : BaseArgumentTest<Array<String>>("Xnullability-annotations") {

    @DisplayName("NullabilityAnnotations is converted to '-Xnullability-annotations' argument")
    @Test
    fun testNullabilityAnnotationsToArgumentString() {
        val annotations = arrayOf("@javax.annotation.Nullable:strict", "@javax.annotation.Nonnull:warn")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_NULLABILITY_ANNOTATIONS] = annotations
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(annotations)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xnullability-annotations' has the default value when NullabilityAnnotations is not set")
    @Test
    fun testNullabilityAnnotationsNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("NullabilityAnnotations can be set and retrieved")
    @Test
    fun testNullabilityAnnotationsGetWhenSet() {
        val expectedAnnotations = arrayOf("@javax.annotation.Nullable:strict", "@javax.annotation.Nonnull:warn")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_NULLABILITY_ANNOTATIONS] = expectedAnnotations
        }

        val actualAnnotations = jvmOperation.compilerArguments[X_NULLABILITY_ANNOTATIONS]

        assertArrayEquals(expectedAnnotations, actualAnnotations)
    }

    @DisplayName("NullabilityAnnotations has the default value when not set")
    @Test
    fun testNullabilityAnnotationsGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val annotations = jvmOperation.compilerArguments[X_NULLABILITY_ANNOTATIONS]

        assertEquals(
            getDefaultValueString(), getValueString(annotations)
        )
    }

    @DisplayName("Raw argument strings '-Xnullability-annotations=<value>' are converted to NullabilityAnnotations")
    @Test
    fun testRawArgumentsNullabilityAnnotationsConversion() {
        val expectedAnnotations = arrayOf("@javax.annotation.Nullable:strict", "@javax.annotation.Nonnull:warn")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedAnnotations))
        )

        assertArrayEquals(
            expectedAnnotations, operation.compilerArguments[X_NULLABILITY_ANNOTATIONS]
        )
    }

    @DisplayName("NullabilityAnnotations has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsNullabilityAnnotations() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_NULLABILITY_ANNOTATIONS])
        )
    }

    @DisplayName("NullabilityAnnotations of null value is converted to '-Xnullability-annotations' argument")
    @Test
    fun testNullNullabilityAnnotations() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_NULLABILITY_ANNOTATIONS] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    @DisplayName("Raw argument with non-existent NullabilityAnnotations mode value fails conversion")
    @Test
    fun testInvalidNullabilityAnnotationsModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("@javax.annotation.Nullable:non-existent-mode")
            )
        }

        assertEquals("Unknown -Xnullability-annotations mode: @javax.annotation.Nullable:non-existent-mode", exception.message)
    }

    @DisplayName("Setting non-existent NullabilityAnnotations mode value directly fails")
    @Test
    fun testInvalidNullabilityAnnotationsModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_NULLABILITY_ANNOTATIONS] = arrayOf("@javax.annotation.Nullable:non-existent-mode")
        }

        assertEquals("Unknown -Xnullability-annotations mode: @javax.annotation.Nullable:non-existent-mode", exception.message)
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}