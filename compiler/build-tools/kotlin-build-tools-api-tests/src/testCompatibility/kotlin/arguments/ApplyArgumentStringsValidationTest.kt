/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assumptions.assumeTrue

class ApplyArgumentStringsValidationTest : BaseCompilationTest() {

    @BtaV2StrategyAgnosticCompilationTest
    fun `applyCompilerArguments with all valid values produces no validation errors`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val [kotlinToolchains, _] = strategyConfig
        // Old BTA versions throw from applyArgumentStrings; new versions store the error and report it during executeOperation
        val kotlinToolingVersion = KotlinToolingVersion(kotlinToolchains.getCompilerVersion())
        assumeTrue { kotlinToolingVersion >= KotlinToolingVersion(2, 4, 20, "snapshot") }

        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile(
                compilationConfigAction = {
                    it.compilerArguments.applyArgumentStrings(listOf("-jvm-target", "21", "-jvm-default", "enable"))
                },
                assertions = { assertOutputs("FooKt.class", "Bar.class", "BazKt.class") })
        }
    }


    @BtaV2StrategyAgnosticCompilationTest
    fun `applyCompilerArguments with single invalid enum collects one error`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val [kotlinToolchains, _] = strategyConfig
        // Old BTA versions throw from applyArgumentStrings; new versions store the error and report it during executeOperation
        val kotlinToolingVersion = KotlinToolingVersion(kotlinToolchains.getCompilerVersion())
        assumeTrue { kotlinToolingVersion >= KotlinToolingVersion(2, 4, 20, "snapshot") }

        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile(
                compilationConfigAction = {
                    it.compilerArguments.applyArgumentStrings(listOf("-jvm-target", "21", "-jvm-default", "bogus"))
                },
                assertions = {
                    expectFail()
                    assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape("bogus")}.*"))
                }
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    fun `applyCompilerArguments collects errors for every invalid enum, not just the first`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val [kotlinToolchains, _] = strategyConfig
        // Old BTA versions throw from applyArgumentStrings; new versions store the error and report it during executeOperation
        val kotlinToolingVersion = KotlinToolingVersion(kotlinToolchains.getCompilerVersion())
        assumeTrue { kotlinToolingVersion >= KotlinToolingVersion(2, 4, 20, "snapshot") }

        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile(
                compilationConfigAction = {
                    it.compilerArguments.applyArgumentStrings(listOf("-jvm-target", "target", "-jvm-default", "bogus"))
                },
                assertions = {
                    expectFail()
                    assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape("bogus")}.*"))
                    assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape("target")}.*"))
                }
            )
        }
    }
}
