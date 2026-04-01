/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DISABLE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class DisablePhasesConversionTest : BaseArgumentTest<List<String>>("Xdisable-phases") {

    @DisplayName("DisablePhases is converted to '-Xdisable-phases' argument")
    @BtaVersionsOnlyCompilationTest
    fun testDisablePhasesToArgumentString(toolchain: KotlinToolchains) {
        val phases = listOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DISABLE_PHASES] = phases
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xdisable-phases' has the default value when DisablePhases is not set")
    @BtaVersionsOnlyCompilationTest
    fun testDisablePhasesNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("DisablePhases can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testDisablePhasesGetWhenSet(toolchain: KotlinToolchains) {
        val expectedPhases = listOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_DISABLE_PHASES] = expectedPhases
        }.build()

        val actualPhases = jvmOperation.compilerArguments[X_DISABLE_PHASES]

        assertEquals(expectedPhases, actualPhases)
    }

    @DisplayName("DisablePhases has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testDisablePhasesGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val phases = jvmOperation.compilerArguments[X_DISABLE_PHASES]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-Xdisable-phases=<value>' are converted to DisablePhases")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsDisablePhasesConversion(toolchain: KotlinToolchains) {
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
            operation.compilerArguments[X_DISABLE_PHASES]
        )
    }

    @DisplayName("DisablePhases has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsDisablePhases(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_DISABLE_PHASES])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<String>?): String? = argument?.joinToString(",")
}
