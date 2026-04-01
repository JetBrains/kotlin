/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305.Mode
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSR305
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class Jsr305ConversionTest : BaseArgumentTest<List<Jsr305>>("Xjsr305") {

    @DisplayName("Jsr305 is converted to '-Xjsr305' argument")
    @BtaVersionsOnlyCompilationTest
    fun testJsr305ToArgumentString(toolchain: KotlinToolchains) {
        val jsr305 = listOf(
            Jsr305.Global(Mode.STRICT),
            Jsr305.UnderMigration(Mode.WARN),
            Jsr305.SpecificAnnotation("com.example.Nullable", Mode.IGNORE),
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JSR305] = jsr305
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(jsr305), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xjsr305' has the default value when Jsr305 is not set")
    @BtaVersionsOnlyCompilationTest
    fun testJsr305NotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("Jsr305 can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testJsr305GetWhenSet(toolchain: KotlinToolchains) {
        val expectedJsr305 = listOf(
            Jsr305.Global(Mode.STRICT),
            Jsr305.UnderMigration(Mode.WARN),
            Jsr305.SpecificAnnotation("com.example.Nullable", Mode.IGNORE),
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JSR305] = expectedJsr305
        }.build()

        val actualJsr305 = jvmOperation.compilerArguments[X_JSR305]

        assertEquals(expectedJsr305, actualJsr305)
    }

    @DisplayName("Jsr305 has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testJsr305GetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val jsr305 = jvmOperation.compilerArguments[X_JSR305]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(jsr305)
        )
    }

    @DisplayName("Raw argument strings '-Xjsr305=<value>' are converted to Jsr305")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsJsr305Conversion(toolchain: KotlinToolchains) {
        val expectedJsr305 = listOf(
            Jsr305.Global(Mode.STRICT),
            Jsr305.UnderMigration(Mode.WARN),
            Jsr305.SpecificAnnotation("com.example.Nullable", Mode.IGNORE),
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedJsr305),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(expectedJsr305, operation.compilerArguments[X_JSR305])
    }

    @DisplayName("Jsr305 has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsJsr305(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_JSR305])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<Jsr305>?): String? =
        argument?.joinToString(",") { item ->
            when (item) {
                is Jsr305.Global -> item.mode.stringValue
                is Jsr305.UnderMigration -> "under-migration:${item.mode.stringValue}"
                is Jsr305.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
            }
        }
}
