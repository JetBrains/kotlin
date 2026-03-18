/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName

@DisplayName("Severity routing: compiler messages are forwarded to the correct KotlinLogger level")
class KotlinLoggerSeverityRoutingTest : BaseCompilationTest() {

    @DisplayName("Compilation error is logged at ERROR level")
    @BtaV2StrategyAgnosticCompilationTest
    fun errorSeverityRoutesToErrorLogLevel(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("compilation-error")
            module.compile {
                expectFail()
                val errorLines = logLines[LogLevel.ERROR].orEmpty()
                assertTrue(errorLines.any { "doesNotExist" in it || "unresolved reference" in it.lowercase() }) {
                    "Expected an unresolved reference error at ERROR level, got: $errorLines"
                }
            }
        }
    }

    @DisplayName("Deprecation warning is logged at WARN level")
    @BtaV2StrategyAgnosticCompilationTest
    fun warningSeverityRoutesToWarnLogLevel(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile {
                val warnLines = logLines[LogLevel.WARN].orEmpty()
                assertTrue(warnLines.any { "deprecated" in it.lowercase() }) {
                    "Expected a deprecation warning at WARN level, got: $warnLines"
                }
            }
        }
    }

    @DisplayName("Verbose output is logged at INFO level")
    @BtaV2StrategyAgnosticCompilationTest
    fun infoSeverityRoutesToInfoLogLevel(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments[VERBOSE] = true
            }) {
                val infoLines = logLines[LogLevel.INFO].orEmpty()
                assertTrue(infoLines.isNotEmpty()) {
                    "Expected at least one message at INFO level with -verbose flag"
                }
            }
        }
    }

    @DisplayName("Compiler output is logged at DEBUG level")
    @BtaV2StrategyAgnosticCompilationTest
    fun debugSeverityRoutesToDebugLogLevel(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile {
                val debugLines = logLines[LogLevel.DEBUG].orEmpty()
                assertTrue(debugLines.isNotEmpty()) {
                    "Expected at least one message at DEBUG level during compilation"
                }
            }
        }
    }
}