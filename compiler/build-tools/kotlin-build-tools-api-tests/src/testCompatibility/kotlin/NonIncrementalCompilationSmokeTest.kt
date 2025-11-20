/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.RemovedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

class NonIncrementalCompilationSmokeTest : BaseCompilationTest() {
    @DisplayName("Non-incremental compilation produces only expected outputs in multi-module setup")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun multiModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            module1.compile {
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
            module2.compile {
                assertOutputs("AKt.class", "BKt.class")
            }
        }
    }

    @DisplayName("Non-incremental compilation produces only expected outputs with mixed java+kotlin setup")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("kotlin-java-mixed")
    fun mixedJavaKotlin(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("kotlin-java-mixed")

            module1.compile {
                assertOutputs("bpkg/MainKt.class", "bpkg/BClass.class")
                if (strategyConfig.first::class.simpleName != "KotlinToolchainsV1Adapter") { // v1 is not producing some logs and that's expected
                    assertLogContainsSubstringExactlyTimes(LogLevel.DEBUG, "AClass.java", 1) // no duplication of java sources
                }
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class, RemovedCompilerArgument::class)
    @DisplayName("Using removed argument throws on unsupported compiler versions")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun removedArgument(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1") { it: JvmCompilationOperation ->
                it.compilerArguments[JvmCompilerArguments.X_USE_K2_KAPT] = true
            }
            if (kotlinToolchain.getCompilerVersion().startsWith("2.3") || kotlinToolchain.getCompilerVersion().startsWith("2.0")) {
                val exception = assertThrows<IllegalStateException> { module1.compile {} }
                assert(exception.message?.contains("Compiler parameter not recognized: X_USE_K2_KAPT") == true) { "Expected exception message to contain 'Compiler parameter not recognized: X_USE_K2_KAPT'" }
            } else {
                module1.compile {
                    assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
                }
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @DisplayName("Using recently added argument throws on unsupported compiler versions")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun addedArgument(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1") { it: JvmCompilationOperation ->
                it.compilerArguments[JvmCompilerArguments.X_ANNOTATIONS_IN_METADATA] = true
            }
            if (kotlinToolchain.getCompilerVersion().startsWith("2.0") || kotlinToolchain.getCompilerVersion().startsWith("2.1")) {
                val exception = assertThrows<IllegalStateException> { module1.compile {} }
                assert(exception.message?.contains("Compiler parameter not recognized: X_ANNOTATIONS_IN_METADATA") == true) { "Expected exception message to contain 'Compiler parameter not recognized: X_ANNOTATIONS_IN_METADATA'" }
            } else {
                module1.compile {
                    assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
                }
            }
        }
    }
}