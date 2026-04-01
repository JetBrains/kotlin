/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.TRACK_CONFIGURATION_INPUTS
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class CompilerPluginsConfigurationInputsTrackingTest : BaseCompilationTest() {

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Adding a compiler plugin triggers non-incremental rebuild")
    @TestMetadata("jvm-module-1")
    fun testCompilerPluginsChangeTriggersNonIncrementalRebuild(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("jvm-module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = {
                    it.compilerArguments[COMPILER_PLUGINS] = listOf(NOARG_PLUGIN)
                },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(strategyConfig),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.COMPILER_ARGS_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Changing compiler plugin options triggers non-incremental rebuild")
    @TestMetadata("jvm-module-1")
    fun testCompilerPluginOptionsChangeTriggersNonIncrementalRebuild(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("jvm-module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = {
                    it.compilerArguments[COMPILER_PLUGINS] = listOf(NOARG_PLUGIN)
                },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = {
                    it.compilerArguments[COMPILER_PLUGINS] = listOf(NOARG_JPA_PLUGIN)
                },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(strategyConfig),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.COMPILER_ARGS_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Removing a compiler plugin triggers non-incremental rebuild")
    @TestMetadata("jvm-module-1")
    fun testCompilerPluginRemovalTriggersNonIncrementalRebuild(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("jvm-module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = {
                    it.compilerArguments[COMPILER_PLUGINS] = listOf(NOARG_PLUGIN)
                },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(strategyConfig),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.COMPILER_ARGS_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    private fun expectedLogLevel(strategyConfig: CompilerExecutionStrategyConfiguration): LogLevel =
        if (strategyConfig.second is ExecutionPolicy.WithDaemon) LogLevel.DEBUG else LogLevel.INFO // TODO: KT-85024
}