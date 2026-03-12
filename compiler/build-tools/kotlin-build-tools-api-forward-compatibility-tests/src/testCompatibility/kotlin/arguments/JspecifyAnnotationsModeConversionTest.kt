/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSPECIFY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class JspecifyAnnotationsModeConversionTest : BaseArgumentTest<String>("Xjspecify-annotations") {

    @DisplayName("JspecifyAnnotationsMode is converted to '-Xjspecify-annotations' argument")
    @Test
    fun testJspecifyAnnotationsModeToArgumentString() {
        for (jspecifyAnnotationsMode in listOf("ignore", "strict", "warn")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JSPECIFY_ANNOTATIONS] = jspecifyAnnotationsMode
            }

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(jspecifyAnnotationsMode)),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xjspecify-annotations' has the default value when JspecifyAnnotationsMode is not set")
    @Test
    fun testJspecifyAnnotationsModeNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JspecifyAnnotationsMode can be set and retrieved")
    @Test
    fun testJspecifyAnnotationsModeGetWhenSet() {
        for (jspecifyAnnotationsMode in listOf("ignore", "strict", "warn")) {
            val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JSPECIFY_ANNOTATIONS] = jspecifyAnnotationsMode
            }

            val actualJspecifyAnnotationsMode = jvmOperation.compilerArguments[X_JSPECIFY_ANNOTATIONS]

            assertEquals(jspecifyAnnotationsMode, actualJspecifyAnnotationsMode)
        }
    }

    @DisplayName("JspecifyAnnotationsMode has the default value when not set")
    @Test
    fun testJspecifyAnnotationsModeGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val jspecifyAnnotationsMode = jvmOperation.compilerArguments[X_JSPECIFY_ANNOTATIONS]

        assertEquals(
            getDefaultValueString(),
            getValueString(jspecifyAnnotationsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xjspecify-annotations <value>' are converted to JspecifyAnnotationsMode")
    @Test
    fun testRawArgumentsJspecifyAnnotationsModeConversion() {
        for (jspecifyAnnotationsMode in listOf("ignore", "strict", "warn")) {
            val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(jspecifyAnnotationsMode),

                    )
            )

            assertEquals(
                jspecifyAnnotationsMode,
                operation.compilerArguments[X_JSPECIFY_ANNOTATIONS]
            )
        }
    }

    @DisplayName("JspecifyAnnotationsMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsJspecifyAnnotationsMode() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(),
            getValueString(operation.compilerArguments[X_JSPECIFY_ANNOTATIONS])
        )
    }

    @DisplayName("Raw argument with non-existent JspecifyAnnotationsMode value fails conversion")
    @Test
    fun testInvalidJspecifyAnnotationsModeConversionFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
        }

        assertEquals("Unknown -Xjspecify-annotations value: non-existent-value", exception.message)
    }

    @DisplayName("Setting non-existent JspecifyAnnotationsMode value directly fails")
    @Test
    fun testInvalidJspecifyAnnotationsModeDirectAssignmentFails() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments[X_JSPECIFY_ANNOTATIONS] = "non-existent-value"
        }

        assertEquals("Unknown -Xjspecify-annotations value: non-existent-value", exception.message)
    }

    @DisplayName("JspecifyAnnotationsMode of null value is converted to '-Xjspecify-annotations' argument")
    @Test
    fun testNullJspecifyAnnotationsMode() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JSPECIFY_ANNOTATIONS] = null
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
