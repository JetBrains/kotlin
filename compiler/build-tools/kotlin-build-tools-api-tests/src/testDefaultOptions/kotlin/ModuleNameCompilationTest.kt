/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.EXPLICIT_NULL_MODULE_NAME_MARKER
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class ModuleNameCompilationTest : BaseCompilationTest() {
    @DisplayName("Non-incremental compilation without specified -module-name")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun nonIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module1 = module("jvm-module-1", moduleCompilationConfigAction = {
                it.compilerArguments[MODULE_NAME] = EXPLICIT_NULL_MODULE_NAME_MARKER
            })

            module1.compile {
                assertOutputs("META-INF/main.kotlin_module", "FooKt.class", "Bar.class", "BazKt.class")
                assertModuleNameIsNotSet()
            }
        }
    }

    @DisplayName("Incremental compilation without specified -module-name")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun incremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module1 = module("jvm-module-1", compilationConfigAction = {
                it.compilerArguments[MODULE_NAME] = EXPLICIT_NULL_MODULE_NAME_MARKER
            })

            module1.createPredefinedFile("secret.kt", "new-file")
            module1.compile {
                assertCompiledSources("secret.kt")
                // SecretKt is added, BazKt is removed
                assertOutputs("META-INF/main.kotlin_module", "SecretKt.class", "Bar.class", "BazKt.class", "FooKt.class")
                assertModuleNameIsNotSet()
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @DisplayName("FIR Incremental compilation without specified -module-name")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun incrementalWithFirRunner(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module1 = module("jvm-module-1", icOptionsConfigAction = {
                it[USE_FIR_RUNNER] = true
            }, compilationConfigAction = {
                it.compilerArguments[CommonCompilerArguments.X_USE_FIR_IC] = true
                it.compilerArguments[MODULE_NAME] = EXPLICIT_NULL_MODULE_NAME_MARKER
            })

            module1.createPredefinedFile("secret.kt", "new-file")
            module1.compile {
                assertCompiledSources("secret.kt")
                // SecretKt is added, BazKt is removed
                assertOutputs("META-INF/main.kotlin_module", "SecretKt.class", "Bar.class", "BazKt.class", "FooKt.class")
                assertModuleNameIsNotSet()
            }
        }
    }

    private fun CompilationOutcome.assertModuleNameIsNotSet() {
        // verify compiler arguments are included in logs
        assertLogContainsPatterns(LogLevel.DEBUG, "Kotlin compiler args:.* -classpath .*".toRegex())
        assertLogDoesNotContainPatterns(LogLevel.DEBUG, ".* -module-name .*".toRegex())
    }
}
