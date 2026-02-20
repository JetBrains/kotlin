/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class JdkHomeConversionTest : BaseCompilationTest() {

    @DisplayName("Test jdk-home is converted to a compiler argument correctly")
    @DefaultStrategyAgnosticCompilationTest
    fun testJdkHomeToArgumentString(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jdkHomePath = workingDirectory.resolve("path/to/jdk")

        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[JvmCompilerArguments.Companion.JDK_HOME] = jdkHomePath
        }.build()

        val argumentStrings = jvmOperation.compilerArguments.toArgumentStrings()
        val valueString = argumentStrings.indexOf("-jdk-home")
            .takeIf { it != -1 }
            ?.let { argumentStrings.getOrNull(it + 1) }

        Assertions.assertEquals(jdkHomePath.absolutePathString(), valueString)
    }

    @DisplayName("Test that jdk-home is not set by default")
    @DefaultStrategyAgnosticCompilationTest
    fun testJdkHomeNotSetByDefault(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val argumentStrings = jvmOperation.compilerArguments.toArgumentStrings()
        val valueString = argumentStrings.indexOf("-jdk-home")
            .takeIf { it != -1 }
            ?.let { argumentStrings.getOrNull(it + 1) }

        Assertions.assertEquals(null, valueString)
    }

    @DisplayName("Test jdk-home is set and retrieved correctly")
    @DefaultStrategyAgnosticCompilationTest
    fun testJdkHomeGetWhenSet(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jdkHomePath = workingDirectory.resolve("path/to/jdk")

        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[JvmCompilerArguments.Companion.JDK_HOME] = jdkHomePath
        }.build()

        val jdkHome = jvmOperation.compilerArguments[JvmCompilerArguments.Companion.JDK_HOME]

        Assertions.assertEquals(jdkHomePath, jdkHome)
    }

    @DisplayName("Test jdk-home is retrieved correctly when it is not set")
    @DefaultStrategyAgnosticCompilationTest
    fun testJdkHomeGetWhenNull(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val jdkHome = jvmOperation.compilerArguments[JvmCompilerArguments.Companion.JDK_HOME]

        Assertions.assertEquals(null, jdkHome)
    }
}