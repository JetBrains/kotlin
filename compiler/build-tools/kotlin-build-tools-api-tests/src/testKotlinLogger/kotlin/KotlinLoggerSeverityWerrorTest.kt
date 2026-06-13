/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.WERROR
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.junit.jupiter.api.DisplayName

@DisplayName("Warnings-as-errors: -Werror promotes warnings to error log level")
class KotlinLoggerSeverityWerrorTest : BaseCompilationTest() {

    @DisplayName("Without -Werror, deprecation warning stays at WARN level")
    @BtaV2StrategyAgnosticCompilationTest
    fun warningIsLoggedAtWarnLevelWithoutWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile {
                assertLogContainsPatterns(LogLevel.WARN, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
                assertLogDoesNotContainPatterns(LogLevel.ERROR, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
            }
        }
    }

    @DisplayName("With -Werror, deprecation warning is promoted to ERROR level")
    @BtaV2StrategyAgnosticCompilationTest
    fun warningIsLoggedAtErrorLevelWithWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                it.compilerArguments[WERROR] = true
            }) {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
                assertLogDoesNotContainPatterns(LogLevel.WARN, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
            }
        }
    }

    @DisplayName("With -Xwarning-level=DEPRECATION:warning and -Werror, deprecation warning stays at WARN level (not escalated)")
    @BtaV2StrategyAgnosticCompilationTest
    fun fixedWarningIsNotEscalatedToErrorWithWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                it.compilerArguments[WERROR] = true
                @OptIn(ExperimentalCompilerArgument::class)
                it.compilerArguments[X_WARNING_LEVEL] = listOf(WarningLevel("DEPRECATION", WarningLevel.Severity.WARNING))
            }) {
                assertLogContainsPatterns(LogLevel.WARN, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
                assertLogDoesNotContainPatterns(LogLevel.ERROR, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
            }
        }
    }

    @DisplayName("With -Xwarning-level=DEPRECATION:warning but no -Werror, deprecation warning still stays at WARN level")
    @BtaV2StrategyAgnosticCompilationTest
    fun fixedWarningStaysAtWarnLevelWithoutWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                @OptIn(ExperimentalCompilerArgument::class)
                it.compilerArguments[X_WARNING_LEVEL] = listOf(WarningLevel("DEPRECATION", WarningLevel.Severity.WARNING))
            }) {
                assertLogContainsPatterns(LogLevel.WARN, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
                assertLogDoesNotContainPatterns(LogLevel.ERROR, Regex(".*oldFun.*"), Regex(".*deprecated.*", RegexOption.IGNORE_CASE))
            }
        }
    }

    // KT-85813: in-process compilation skips checkRedundantArguments because explicitArguments
    // is not carried over when BTA converts applyArgumentStrings → toCompilerArguments().
    // Daemon mode round-trips via toArgumentStrings() so the daemon re-parses and sets explicitArguments.
    @DisplayName("KT-85813: With -Xcontext-parameters (redundant in LV 2.4+) and -Werror, warning is promoted to compilation error")
    @BtaV2StrategyAgnosticCompilationTest
    fun missingWarningWithInProcessMode(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xcontext-parameters"))
                it.compilerArguments[WERROR] = true
            }) {
                expectFail()
            }
        }
    }
}
