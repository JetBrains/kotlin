/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows

class ApplyArgumentStringsValidationTest : BaseCompilationTest() {
    // Old BTA versions throw from applyCommandLineArguments; new versions store the error and report it during executeOperation
    fun KotlinToolchains.isArgumentExceptionDelayedUntilExecution() =
        KotlinToolingVersion(getCompilerVersion()) >= KotlinToolingVersion(2, 4, 20, "snapshot")

    @BtaV2StrategyAgnosticCompilationTest
    fun `applyCompilerArguments with all valid values produces no validation errors`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(
                compilationConfigAction = {
                    it.compilerArguments.applyCommandLineArguments(listOf("-jvm-target", "21", "-jvm-default", "enable"))
                },
                assertions = { assertOutputs("FooKt.class", "Bar.class", "BazKt.class") })
        }
    }


    @BtaV2StrategyAgnosticCompilationTest
    fun `applyCompilerArguments with single invalid enum collects one error`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val [kotlinToolchains, _] = strategyConfig
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(
                compilationConfigAction = {
                    if (kotlinToolchains.isArgumentExceptionDelayedUntilExecution()) {
                        it.compilerArguments.applyCommandLineArguments(listOf("-jvm-target", "15", "-api-version", "bogus"))
                    } else {
                        val exception = assertThrows<CompilerArgumentsParseException> {
                            it.compilerArguments.applyCommandLineArguments(listOf("-jvm-target", "15", "-api-version", "bogus"))
                        }
                        assertTrue(exception.message!!.contains("Unknown -api-version value: bogus"))
                    }
                },
                assertions = {
                    if (kotlinToolchains.isArgumentExceptionDelayedUntilExecution()) {
                        expectFail()
                        assertLogContainsLines(LogLevel.ERROR, "Unknown -api-version value: bogus")
                    }
                }
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    fun `applyCompilerArguments with inexistent argument name collects one error`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val [kotlinToolchains, _] = strategyConfig
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(
                compilationConfigAction = {
                    if (kotlinToolchains.isArgumentExceptionDelayedUntilExecution()) {
                        it.compilerArguments.applyCommandLineArguments(listOf("-inexistent-argument", "15"))
                    } else {
                        val exception = assertThrows<CompilerArgumentsParseException> {
                            it.compilerArguments.applyCommandLineArguments(listOf("-inexistent-argument", "15"))
                        }
                        assertTrue(exception.message!!.contains("Invalid argument: -inexistent-argument"))
                    }
                },
                assertions = {
                    if (kotlinToolchains.isArgumentExceptionDelayedUntilExecution()) {
                        expectFail()
                        assertLogContainsLines(LogLevel.ERROR, "Invalid argument: -inexistent-argument")
                    }
                }
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    fun `applyCompilerArguments collects errors for every invalid enum, not just the first`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val [kotlinToolchains, _] = strategyConfig
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(
                compilationConfigAction = {
                    if (kotlinToolchains.isArgumentExceptionDelayedUntilExecution()) {
                        it.compilerArguments.applyCommandLineArguments(listOf("-jvm-target", "target", "-api-version", "bogus"))
                    } else {
                        val exception = assertThrows<CompilerArgumentsParseException> {
                            it.compilerArguments.applyCommandLineArguments(listOf("-jvm-target", "target", "-api-version", "bogus"))
                        }
                        assertTrue(exception.message!!.contains("Unknown -api-version value: bogus"))
                    }
                },
                assertions = {
                    if (kotlinToolchains.isArgumentExceptionDelayedUntilExecution()) {
                        expectFail()
                        assertLogContainsLines(LogLevel.ERROR, "Unknown -api-version value: bogus")
                        assertLogContainsLines(LogLevel.ERROR, "Unknown -jvm-target value: target")
                    }
                }
            )
        }
    }
}
