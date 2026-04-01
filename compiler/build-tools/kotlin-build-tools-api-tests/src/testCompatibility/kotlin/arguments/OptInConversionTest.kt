/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class OptInConversionTest : BaseArgumentTest<List<String>>("opt-in") {

    @DisplayName("OptIn is converted to '-opt-in' argument")
    @BtaVersionsOnlyCompilationTest
    fun testOptInToArgumentString(toolchain: KotlinToolchains) {
        val optIns = listOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[OPT_IN] = optIns
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(optIns), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-opt-in' has the default value when OptIn is not set")
    @BtaVersionsOnlyCompilationTest
    fun testOptInNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("OptIn can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testOptInGetWhenSet(toolchain: KotlinToolchains) {
        val expectedOptIns = listOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[OPT_IN] = expectedOptIns
        }.build()

        val actualOptIns = jvmOperation.compilerArguments[OPT_IN]

        assertEquals(expectedOptIns, actualOptIns)
    }

    @DisplayName("OptIn has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testOptInGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val optIns = jvmOperation.compilerArguments[OPT_IN]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(optIns)
        )
    }

    @DisplayName("Raw argument strings '-opt-in=<value>' are converted to OptIn")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsOptInConversion(toolchain: KotlinToolchains) {
        val expectedOptIns = listOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedOptIns),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedOptIns,
            operation.compilerArguments[OPT_IN]
        )
    }

    @DisplayName("OptIn has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsOptIn(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[OPT_IN])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: List<String>?): String? = argument?.joinToString(",")
}
