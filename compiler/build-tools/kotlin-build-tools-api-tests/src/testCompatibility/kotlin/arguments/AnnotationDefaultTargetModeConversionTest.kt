/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AnnotationDefaultTargetModeConversionTest : BaseArgumentTest<AnnotationDefaultTargetMode>("Xannotation-default-target") {

    @DisplayName("AnnotationDefaultTargetMode is converted to '-Xannotation-default-target' argument")
    @BtaVersionsOnlyCompilationTest
    fun testAnnotationDefaultTargetModeToArgumentString(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        for (annotationDefaultTargetMode in AnnotationDefaultTargetMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ANNOTATION_DEFAULT_TARGET] = annotationDefaultTargetMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(annotationDefaultTargetMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xannotation-default-target' has the default value when AnnotationDefaultTargetMode is not set")
    @BtaVersionsOnlyCompilationTest
    fun testAnnotationDefaultTargetModeNotSetByDefault(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AnnotationDefaultTargetMode can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testAnnotationDefaultTargetModeGetWhenSet(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        for (annotationDefaultTargetMode in AnnotationDefaultTargetMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_ANNOTATION_DEFAULT_TARGET] = annotationDefaultTargetMode
            }.build()

            val actualAnnotationDefaultTargetMode = jvmOperation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET]

            assertEquals(annotationDefaultTargetMode, actualAnnotationDefaultTargetMode)
        }
    }

    @DisplayName("AnnotationDefaultTargetMode has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testAnnotationDefaultTargetModeGetWhenNull(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val annotationDefaultTargetMode = jvmOperation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(annotationDefaultTargetMode)
        )
    }

    @DisplayName("Raw argument strings '-Xannotation-default-target=<value>' are converted to AnnotationDefaultTargetMode")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsAbiStabilityConversion(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        for (annotationDefaultTargetMode in AnnotationDefaultTargetMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(annotationDefaultTargetMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                annotationDefaultTargetMode,
                operation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET]
            )
        }
    }

    @DisplayName("AnnotationDefaultTargetMode has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsAbiStability(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET])
        )
    }

    @DisplayName("Raw argument with non-existent AnnotationDefaultTargetMode value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidAnnotationDefaultTargetModeConversionFails(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_ANNOTATION_DEFAULT_TARGET]
        }
    }

    @DisplayName("AnnotationDefaultTargetMode of null value is converted to '-Xannotation-default-target' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullAnnotationDefaultTargetMode(toolchain: KotlinToolchains) {
        assumeAnnotationDefaultTargetMode(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ANNOTATION_DEFAULT_TARGET] = null
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

    override fun getValueString(argument: AnnotationDefaultTargetMode?): String? = argument?.stringValue

    private fun assumeAnnotationDefaultTargetMode(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_ANNOTATION_DEFAULT_TARGET.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_ANNOTATION_DEFAULT_TARGET.availableSinceVersion}"
        )
    }
}