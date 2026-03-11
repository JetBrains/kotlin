/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class VerbosePhasesConversionTest : BaseArgumentTest<List<String>>("Xverbose-phases") {

    @DisplayName("VerbosePhases is converted to '-Xverbose-phases' argument")
    @BtaVersionsOnlyCompilationTest
    fun testVerbosePhasesToArgumentString(toolchain: KotlinToolchains) {
        val phases = listOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_VERBOSE_PHASES] = phases
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xverbose-phases' has the default value when VerbosePhases is not set")
    @BtaVersionsOnlyCompilationTest
    fun testVerbosePhasesNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("VerbosePhases can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testVerbosePhasesGetWhenSet(toolchain: KotlinToolchains) {
        val expectedPhases = listOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_VERBOSE_PHASES] = expectedPhases
        }.build()

        val actualPhases = jvmOperation.compilerArguments[X_VERBOSE_PHASES]

        assertEquals(expectedPhases, actualPhases)
    }

    @DisplayName("VerbosePhases has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testVerbosePhasesGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val phases = jvmOperation.compilerArguments[X_VERBOSE_PHASES]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-Xverbose-phases=<value>' are converted to VerbosePhases")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsVerbosePhasesConversion(toolchain: KotlinToolchains) {
        val expectedPhases = listOf("phase1", "phase2", "phase3")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedPhases),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedPhases,
            operation.compilerArguments[X_VERBOSE_PHASES]
        )
    }

    @DisplayName("VerbosePhases has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsVerbosePhases(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_VERBOSE_PHASES])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<String>?): String? = argument?.joinToString(",")
}
