/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_IGNORED_ANNOTATIONS_FOR_BRIDGES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

// TODO(KT-85093) Resolve Forward Compatibility Test Blocker for X_IGNORED_ANNOTATIONS_FOR_BRIDGES
@OptIn(ExperimentalCompilerArgument::class)
internal class IgnoredAnnotationsForBridgesConversionTest : BaseArgumentTest<List<String>>("Xignored-annotations-for-bridges") {

    private lateinit var toolchain: KotlinToolchains

    @BeforeEach
    fun setup() {
        toolchain = KotlinToolchains.loadImplementation(btaClassloader)
    }

    @DisplayName("IgnoredAnnotationsForBridges is converted to '-Xignored-annotations-for-bridges' argument")
    @Test
    fun testIgnoredAnnotationsForBridgesToArgumentString() {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val annotations = listOf("com.example.MyAnnotation", "*")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_IGNORED_ANNOTATIONS_FOR_BRIDGES] = annotations
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(annotations), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xignored-annotations-for-bridges' has the default value when IgnoredAnnotationsForBridges is not set")
    @Test
    fun testIgnoredAnnotationsForBridgesNotSetByDefault() {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("IgnoredAnnotationsForBridges can be set and retrieved")
    @Test
    fun testIgnoredAnnotationsForBridgesGetWhenSet() {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val expectedAnnotations = listOf("com.example.MyAnnotation", "*")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_IGNORED_ANNOTATIONS_FOR_BRIDGES] = expectedAnnotations
        }.build()

        val actualAnnotations = jvmOperation.compilerArguments[X_IGNORED_ANNOTATIONS_FOR_BRIDGES]

        assertEquals(expectedAnnotations, actualAnnotations)
    }

    @DisplayName("IgnoredAnnotationsForBridges has the default value when not set")
    @Test
    fun testIgnoredAnnotationsForBridgesGetWhenNull() {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val annotations = jvmOperation.compilerArguments[X_IGNORED_ANNOTATIONS_FOR_BRIDGES]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(annotations)
        )
    }

    @DisplayName("Raw argument strings '-Xignored-annotations-for-bridges=<value>' are converted to IgnoredAnnotationsForBridges")
    @Test
    fun testRawArgumentsIgnoredAnnotationsForBridgesConversion() {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val expectedAnnotations = listOf("com.example.MyAnnotation", "*")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedAnnotations),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedAnnotations,
            operation.compilerArguments[X_IGNORED_ANNOTATIONS_FOR_BRIDGES]
        )
    }

    @DisplayName("IgnoredAnnotationsForBridges has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsIgnoredAnnotationsForBridges() {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_IGNORED_ANNOTATIONS_FOR_BRIDGES])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<String>?): String? =
        argument?.joinToString(",")

    private fun assumeArgumentSupported(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_IGNORED_ANNOTATIONS_FOR_BRIDGES.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_IGNORED_ANNOTATIONS_FOR_BRIDGES.availableSinceVersion}"
        )
    }
}
