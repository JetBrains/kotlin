/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

class BuildersCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Test builders produce independent operations")
    @DefaultStrategyAgnosticCompilationTest
    fun myTest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        println("Compiler version: ${toolchain.getCompilerVersion()}")
        val sources = listOf("a.kt", "b.kt", "c.kt").map { Paths.get(it) }
        val destination = Paths.get("classes")
        val operationBuilder = toolchain.jvm.jvmCompilationOperationBuilder(sources, destination).apply {
            compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM1_6
            compilerArguments[JvmCompilerArguments.MODULE_NAME] = "abc"
        }

        val operation1 = operationBuilder.build()
        val operation2 = operationBuilder.build()
        assertNotEquals(operation1, operation2)

        operationBuilder.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17
        val operation3 = operationBuilder.build()
        assertEquals(JvmTarget.JVM1_6, operation1.compilerArguments[JvmCompilerArguments.JVM_TARGET])
        assertEquals(JvmTarget.JVM_17, operation3.compilerArguments[JvmCompilerArguments.JVM_TARGET])
    }
}
