/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ADD_MODULES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AddModulesConversionTest : BaseArgumentTest<List<String>>("Xadd-modules") {

    @DisplayName("AddModules is converted to '-Xadd-modules' argument")
    @BtaVersionsOnlyCompilationTest
    fun testAddModulesToArgumentString(toolchain: KotlinToolchains) {
        val modules = listOf("module1", "module2", "module3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = modules
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(modules), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xadd-modules' has the default value when AddModules is not set")
    @BtaVersionsOnlyCompilationTest
    fun testAddModulesNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AddModules can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testAddModulesGetWhenSet(toolchain: KotlinToolchains) {
        val expectedModules = listOf("module1", "module2", "module3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = expectedModules
        }.build()

        val actualModules = jvmOperation.compilerArguments[X_ADD_MODULES]

        assertEquals(expectedModules, actualModules)
    }

    @DisplayName("AddModules has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testAddModulesGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val modules = jvmOperation.compilerArguments[X_ADD_MODULES]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(modules)
        )
    }

    @DisplayName("Raw argument strings '-Xadd-modules=<value>' are converted to AddModules")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsAddModulesConversion(toolchain: KotlinToolchains) {
        val expectedModules = listOf("module1", "module2", "module3")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedModules),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedModules,
            operation.compilerArguments[X_ADD_MODULES]
        )
    }

    @DisplayName("AddModules has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsAddModules(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_ADD_MODULES])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<String>?): String? = argument?.joinToString(",")
}