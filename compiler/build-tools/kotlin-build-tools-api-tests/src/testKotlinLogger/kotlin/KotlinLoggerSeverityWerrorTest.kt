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
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName

@DisplayName("Warnings-as-errors: -Werror promotes warnings to error log level")
class KotlinLoggerSeverityWerrorTest : BaseCompilationTest() {

    @DisplayName("Without -Werror, deprecation warning stays at WARN level")
    @BtaV2StrategyAgnosticCompilationTest
    fun warningIsLoggedAtWarnLevelWithoutWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile {
                val warnLines = logLines[LogLevel.WARN].orEmpty()
                val errorLines = logLines[LogLevel.ERROR].orEmpty()
                assertTrue(warnLines.any { "oldFun" in it && "deprecated" in it.lowercase() }) {
                    "Expected a deprecation warning at WARN level, got: $warnLines"
                }
                assertFalse(errorLines.any { "oldFun" in it || "deprecated" in it.lowercase() }) {
                    "Expected no deprecation message at ERROR level, got: $errorLines"
                }
            }
        }
    }

    @DisplayName("With -Werror, deprecation warning is promoted to ERROR level")
    @BtaV2StrategyAgnosticCompilationTest
    fun warningIsLoggedAtErrorLevelWithWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                it.compilerArguments[WERROR] = true
            }) {
                expectFail()
                val errorLines = logLines[LogLevel.ERROR].orEmpty()
                val warnLines = logLines[LogLevel.WARN].orEmpty()
                assertTrue(errorLines.any { "oldFun" in it && "deprecated" in it.lowercase() }) {
                    "Expected a deprecation warning promoted to ERROR level, got: $errorLines"
                }
                assertFalse(warnLines.any { "oldFun" in it || "deprecated" in it.lowercase() }) {
                    "Expected no deprecation message at WARN level when Werror is set, got: $warnLines"
                }
            }
        }
    }

    @DisplayName("With -Xwarning-level=DEPRECATION:warning and -Werror, deprecation warning stays at WARN level (not escalated)")
    @BtaV2StrategyAgnosticCompilationTest
    fun fixedWarningIsNotEscalatedToErrorWithWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                it.compilerArguments[WERROR] = true
                @OptIn(ExperimentalCompilerArgument::class)
                it.compilerArguments[X_WARNING_LEVEL] = listOf(WarningLevel("DEPRECATION", WarningLevel.Severity.WARNING))
            }) {
                val warnLines = logLines[LogLevel.WARN].orEmpty()
                val errorLines = logLines[LogLevel.ERROR].orEmpty()
                assertTrue(warnLines.any { "oldFun" in it && "deprecated" in it.lowercase() }) {
                    "Expected the DEPRECATION warning pinned by -Xwarning-level to stay at WARN level, got warnLines=$warnLines"
                }
                assertFalse(errorLines.any { "oldFun" in it || "deprecated" in it.lowercase() }) {
                    "Expected no DEPRECATION message at ERROR level when -Xwarning-level=DEPRECATION:warning is set, got errorLines=$errorLines"
                }
            }
        }
    }

    @DisplayName("With -Xwarning-level=DEPRECATION:warning but no -Werror, deprecation warning still stays at WARN level")
    @BtaV2StrategyAgnosticCompilationTest
    fun fixedWarningStaysAtWarnLevelWithoutWerror(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                @OptIn(ExperimentalCompilerArgument::class)
                it.compilerArguments[X_WARNING_LEVEL] = listOf(WarningLevel("DEPRECATION", WarningLevel.Severity.WARNING))
            }) {
                val warnLines = logLines[LogLevel.WARN].orEmpty()
                val errorLines = logLines[LogLevel.ERROR].orEmpty()
                assertTrue(warnLines.any { "oldFun" in it && "deprecated" in it.lowercase() }) {
                    "Expected the DEPRECATION warning at WARN level, got warnLines=$warnLines"
                }
                assertFalse(errorLines.any { "oldFun" in it || "deprecated" in it.lowercase() }) {
                    "Expected no DEPRECATION message at ERROR level, got errorLines=$errorLines"
                }
            }
        }
    }
}
