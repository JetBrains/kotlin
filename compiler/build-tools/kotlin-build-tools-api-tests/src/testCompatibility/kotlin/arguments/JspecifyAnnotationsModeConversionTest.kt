/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSPECIFY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JspecifyAnnotationsMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class JspecifyAnnotationsModeConversionTest : BaseArgumentTest<JspecifyAnnotationsMode>("Xjspecify-annotations") {

    @DisplayName("JspecifyAnnotationsMode is converted to '-Xjspecify-annotations' argument")
    @BtaVersionsOnlyCompilationTest
    fun testJspecifyAnnotationsModeToArgumentString(toolchain: KotlinToolchains) {
        for (jspecifyAnnotationsMode in JspecifyAnnotationsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JSPECIFY_ANNOTATIONS] = jspecifyAnnotationsMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(jspecifyAnnotationsMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xjspecify-annotations' has the default value when JspecifyAnnotationsMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testJspecifyAnnotationsModeNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JspecifyAnnotationsMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testJspecifyAnnotationsModeGetWhenSet(toolchain: KotlinToolchains) {
        for (jspecifyAnnotationsMode in JspecifyAnnotationsMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JSPECIFY_ANNOTATIONS] = jspecifyAnnotationsMode
            }.build()

            val actualJspecifyAnnotationsMode = jvmOperation.compilerArguments[X_JSPECIFY_ANNOTATIONS]

            assertEquals(jspecifyAnnotationsMode, actualJspecifyAnnotationsMode)
        }
    }

    @DisplayName("JspecifyAnnotationsMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testJspecifyAnnotationsModeGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val jspecifyAnnotationsMode = jvmOperation.compilerArguments[X_JSPECIFY_ANNOTATIONS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(jspecifyAnnotationsMode)
        )
    }

    @DisplayName("Raw argument strings '-Xjspecify-annotations <value>' are converted to JspecifyAnnotationsMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsJspecifyAnnotationsModeConversion(toolchain: KotlinToolchains) {
        for (jspecifyAnnotationsMode in JspecifyAnnotationsMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(jspecifyAnnotationsMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                jspecifyAnnotationsMode,
                operation.compilerArguments[X_JSPECIFY_ANNOTATIONS]
            )
        }
    }

    @DisplayName("JspecifyAnnotationsMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsJspecifyAnnotationsMode(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_JSPECIFY_ANNOTATIONS])
        )
    }

    @DisplayName("Raw argument with non-existent JspecifyAnnotationsMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidJspecifyAnnotationsModeConversionFails(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_JSPECIFY_ANNOTATIONS]
        }

        assertEquals("Unknown -Xjspecify-annotations value: non-existent-value", exception.message)
    }

    @DisplayName("JspecifyAnnotationsMode of null value is converted to '-Xjspecify-annotations' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullJspecifyAnnotationsMode(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JSPECIFY_ANNOTATIONS] = null
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

    override fun getValueString(argument: JspecifyAnnotationsMode?): String? = argument?.stringValue
}
