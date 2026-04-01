/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class PhasesToDumpConversionTest : BaseArgumentTest<List<String>>("Xphases-to-dump") {

    @DisplayName("PhasesToDump is converted to '-Xphases-to-dump' argument")
    @BtaVersionsOnlyCompilationTest
    fun testPhasesToDumpToArgumentString(toolchain: KotlinToolchains) {
        val phases = listOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PHASES_TO_DUMP] = phases
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(phases), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xphases-to-dump' has the default value when PhasesToDump is not set")
    @BtaVersionsOnlyCompilationTest
    fun testPhasesToDumpNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("PhasesToDump can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testPhasesToDumpGetWhenSet(toolchain: KotlinToolchains) {
        val expectedPhases = listOf("phase1", "phase2", "phase3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_PHASES_TO_DUMP] = expectedPhases
        }.build()

        val actualPhases = jvmOperation.compilerArguments[X_PHASES_TO_DUMP]

        assertEquals(expectedPhases, actualPhases)
    }

    @DisplayName("PhasesToDump has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testPhasesToDumpGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val phases = jvmOperation.compilerArguments[X_PHASES_TO_DUMP]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(phases)
        )
    }

    @DisplayName("Raw argument strings '-Xphases-to-dump=<value>' are converted to PhasesToDump")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsPhasesToDumpConversion(toolchain: KotlinToolchains) {
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
            operation.compilerArguments[X_PHASES_TO_DUMP]
        )
    }

    @DisplayName("PhasesToDump has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsPhasesToDump(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_PHASES_TO_DUMP])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<String>?): String? = argument?.joinToString(",")
}
