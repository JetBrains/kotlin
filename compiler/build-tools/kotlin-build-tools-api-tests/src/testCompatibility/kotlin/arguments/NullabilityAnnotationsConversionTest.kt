/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NullabilityAnnotationMode
import org.jetbrains.kotlin.buildtools.api.arguments.types.NullabilityAnnotation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class NullabilityAnnotationsConversionTest : BaseArgumentTest<List<NullabilityAnnotation>>("Xnullability-annotations") {

    @DisplayName("NullabilityAnnotations is converted to '-Xnullability-annotations' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullabilityAnnotationsToArgumentString(toolchain: KotlinToolchains) {
        val annotations = listOf(
            NullabilityAnnotation("javax.annotation.Nullable", NullabilityAnnotationMode.STRICT),
            NullabilityAnnotation("javax.annotation.Nonnull", NullabilityAnnotationMode.WARN),
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_NULLABILITY_ANNOTATIONS] = annotations
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(annotations), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xnullability-annotations' has the default value when NullabilityAnnotations is not set")
    @BtaVersionsOnlyCompilationTest
    fun testNullabilityAnnotationsNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("NullabilityAnnotations can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testNullabilityAnnotationsGetWhenSet(toolchain: KotlinToolchains) {
        val expectedAnnotations = listOf(
            NullabilityAnnotation("javax.annotation.Nullable", NullabilityAnnotationMode.STRICT),
            NullabilityAnnotation("javax.annotation.Nonnull", NullabilityAnnotationMode.WARN),
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_NULLABILITY_ANNOTATIONS] = expectedAnnotations
        }.build()

        val actualAnnotations = jvmOperation.compilerArguments[X_NULLABILITY_ANNOTATIONS]

        assertListEquals(expectedAnnotations, actualAnnotations)
    }

    @DisplayName("NullabilityAnnotations has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testNullabilityAnnotationsGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val annotations = jvmOperation.compilerArguments[X_NULLABILITY_ANNOTATIONS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(annotations)
        )
    }

    @DisplayName("Raw argument strings '-Xnullability-annotations=<value>' are converted to NullabilityAnnotations")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsNullabilityAnnotationsConversion(toolchain: KotlinToolchains) {
        val expectedAnnotations = listOf(
            NullabilityAnnotation("javax.annotation.Nullable", NullabilityAnnotationMode.STRICT),
            NullabilityAnnotation("javax.annotation.Nonnull", NullabilityAnnotationMode.WARN),
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedAnnotations),
                toolchain.getCompilerVersion()
            )
        )

        assertListEquals(
            expectedAnnotations,
            operation.compilerArguments[X_NULLABILITY_ANNOTATIONS]
        )
    }

    @DisplayName("NullabilityAnnotations has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsNullabilityAnnotations(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_NULLABILITY_ANNOTATIONS])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<NullabilityAnnotation>?): String? =
        argument?.joinToString(",") { "@${it.annotationFqName}:${it.mode.stringValue}" }

    private fun assertListEquals(expectedList: List<NullabilityAnnotation>, actualList: List<NullabilityAnnotation>) {
        assertEquals(expectedList.size, actualList.size)

        expectedList.forEachIndexed { index, expected ->
            val actual = actualList[index]

            assertEquals(expected.annotationFqName, actual.annotationFqName)
            assertEquals(expected.mode.stringValue, actual.mode.stringValue)
        }
    }
}
