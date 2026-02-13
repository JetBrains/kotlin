/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ADD_MODULES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AddModulesConversionTest : BaseCompilationTest() {

    @DisplayName("Test Xadd-modules is converted to a compiler argument correctly")
    @DefaultStrategyAgnosticCompilationTest
    fun testXaddModulesToArgumentArray(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val expectedModules = listOf("module1", "module2", "module3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = expectedModules
        }.build()

        jvmOperation.compilerArguments.toArgumentStrings()
        val valueString =
            jvmOperation.compilerArguments.toArgumentStrings().first { it.startsWith("-Xadd-modules=") }.removePrefix("-Xadd-modules=")
        val actualModules = valueString.split(",")

        assertEquals(expectedModules.size, actualModules.size)
        for (i in expectedModules.indices) {
            assertEquals(expectedModules[i], actualModules[i])
        }
    }

    @DisplayName("Test empty Xadd-modules is converted to a compiler argument correctly")
    @DefaultStrategyAgnosticCompilationTest
    fun testEmptyXaddModulesToArgumentArray(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = emptyList()
        }.build()

        val valueString =
            jvmOperation.compilerArguments.toArgumentStrings().firstOrNull { it.startsWith("-Xadd-modules=") }
                ?.removePrefix("-Xadd-modules=")

        assertEquals(null, valueString)
    }

    @DisplayName("Test that Xadd-modules is not set by default")
    @DefaultStrategyAgnosticCompilationTest
    fun testXaddModulesANotSetByDefault(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val valueString =
            jvmOperation.compilerArguments.toArgumentStrings().firstOrNull { it.startsWith("-Xadd-modules=") }

        assertEquals(null, valueString)
    }

    @DisplayName("Test Xadd-modules is set and retrieved correctly")
    @DefaultStrategyAgnosticCompilationTest
    fun testXaddModulesAGetWhenSet(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val expectedModules = listOf("module1", "module2", "module3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = expectedModules
        }.build()

        val actualModules = jvmOperation.compilerArguments[X_ADD_MODULES]

        assertEquals(expectedModules.size, actualModules.size)
        for (i in expectedModules.indices) {
            assertEquals(expectedModules[i], actualModules[i])
        }
    }

    @DisplayName("Test Xadd-modules is retrieved correctly when it is not set")
    @DefaultStrategyAgnosticCompilationTest
    fun testXaddModulesGetWhenNull(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualModules = jvmOperation.compilerArguments[X_ADD_MODULES]

        assertEquals(emptyList<String>(), actualModules)
    }
}