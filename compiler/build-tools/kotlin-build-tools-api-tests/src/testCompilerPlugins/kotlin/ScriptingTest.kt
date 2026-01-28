/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_USE_FIR_LT
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Dependency
import org.jetbrains.kotlin.buildtools.tests.compilation.model.FileDependency
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths
import kotlin.reflect.KClass

class ScriptingTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application (non-incremental)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationNonIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("scripting-kts", dependencies = listOf(dependencyOnThisClasspath))
            module.compile(compilationConfigAction = configureCompilerArgs(GreetScriptTemplate::class)) {
                assertOutputs("Test_greet.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application with custom extension (non-incremental)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationCustomExtensionNonIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("scripting-custom-extension", dependencies = listOf(dependencyOnThisClasspath))
            module.compile(compilationConfigAction = configureCompilerArgs(GreetScriptCustomExtensionTemplate::class, "greet")) {
                requireLogLevel(LogLevel.DEBUG)
                assertOutputs("Test.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application (incremental)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("scripting-kts", dependencies = listOf(dependencyOnThisClasspath))
            module.compileIncrementally(
                SourcesChanges.Unknown,
                compilationConfigAction = configureCompilerArgs(GreetScriptTemplate::class)
            ) {
                assertOutputs("Test_greet.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application with custom extension (incremental)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationCustomExtensionIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("scripting-custom-extension", dependencies = listOf(dependencyOnThisClasspath))
            module.compileIncrementally(
                SourcesChanges.Unknown,
                compilationConfigAction = configureCompilerArgs(GreetScriptCustomExtensionTemplate::class, "greet")
            ) {
                requireLogLevel(LogLevel.DEBUG)
                assertOutputs("Test.class")
            }
        }
    }

    private val dependencyOnThisClasspath: Dependency
        get() = FileDependency(
            Paths.get(
                GreetScriptTemplate::class.java
                    .protectionDomain
                    .codeSource
                    .location
                    .toURI()
            )
        )

    private fun configureCompilerArgs(
        scriptingTemplate: KClass<*>,
        customScriptExtension: String? = null,
    ): (JvmCompilationOperation.Builder) -> Unit = {
        it.compilerArguments[COMPILER_PLUGINS] = listOf(scriptingPlugin(scriptingTemplate))
        @OptIn(ExperimentalCompilerArgument::class)
        it.compilerArguments[X_USE_FIR_LT] = false
        if (customScriptExtension != null) {
            it[KOTLINSCRIPT_EXTENSIONS] = arrayOf(customScriptExtension)
        }
    }
}