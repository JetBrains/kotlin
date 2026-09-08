/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.TRACK_CONFIGURATION_INPUTS
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.AbstractProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAndPlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.MetadataProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.DisplayName

class ConfigurationInputsTrackingTest : BaseCompilationTest() {

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("First build forces non-incremental rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testFirstBuildForcesNonIncrementalRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS.readableString}".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Subsequent build with unchanged config stays incremental")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testSubsequentBuildWithUnchangedConfigStaysIncremental(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            )
            module.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Adding a tracked compiler arg triggers rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testCompilerArgsAdditionTriggersNonIncrementalRebuild(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_11 },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.COMPILER_ARGS_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Changing a tracked compiler arg value triggers rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testCompilerArgsChangeTriggersNonIncrementalRebuild(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_11 },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17 },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.COMPILER_ARGS_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Removing a tracked compiler arg triggers rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testCompilerArgsRemovalTriggersNonIncrementalRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[CommonCompilerArguments.PROGRESSIVE] = true },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.COMPILER_ARGS_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Adding a non-tracked compiler arg does not trigger rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNonTrackedCompilerArgAdditionDoesNotTriggerRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[CommonToolArguments.VERSION] = true },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Changing a non-tracked compiler arg does not trigger rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNonTrackedCompilerArgChangeDoesNotTriggerRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[CommonToolArguments.VERSION] = false },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[CommonToolArguments.VERSION] = true },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Removing a non-tracked compiler arg does not trigger rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNonTrackedCompilerArgRemovalDoesNotTriggerRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[CommonToolArguments.VERSION] = true },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Adding a tracked IC config option triggers rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testIcConfigAdditionTriggersNonIncrementalRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
                },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.INCREMENTAL_COMPILATION_CONFIGURATION_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Changing a tracked IC config value triggers rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testIcConfigChangeTriggersNonIncrementalRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
                },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = false
                },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.INCREMENTAL_COMPILATION_CONFIGURATION_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Removing a tracked IC config option triggers rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testIcConfigRemovalTriggersNonIncrementalRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
                },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.INCREMENTAL_COMPILATION_CONFIGURATION_CHANGED.readableString}".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Adding a non-tracked IC config option does not trigger rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNonTrackedIcConfigAdditionDoesNotTriggerRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[KEEP_IC_CACHES_IN_MEMORY] = true
                },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Changing a non-tracked IC config option does not trigger rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNonTrackedIcConfigChangeDoesNotTriggerRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[KEEP_IC_CACHES_IN_MEMORY] = true
                },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[KEEP_IC_CACHES_IN_MEMORY] = false
                },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Removing a non-tracked IC config option does not trigger rebuild")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNonTrackedIcConfigRemovalDoesNotTriggerRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[KEEP_IC_CACHES_IN_MEMORY] = true
                },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Changing both IC config and compiler args reports the 1st found rebuild reason")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testBothIcConfigAndCompilerArgChangeReportsOneReason(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
                },
                compilationConfigAction = { it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_11 },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = false
                },
                compilationConfigAction = { it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17 },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.INCREMENTAL_COMPILATION_CONFIGURATION_CHANGED.readableString}".toRegex()
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("IC config change is ignored when tracking is disabled")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testIcConfigChangeDoesNotTriggerRebuildWhenTrackingDisabled(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = false
                    it[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION] = true
                },
            )
            module.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = false
                    it[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION] = false
                },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Compiler arg change is ignored when tracking is disabled")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testCompilerArgChangeDoesNotTriggerRebuildWhenTrackingDisabled(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = false },
                compilationConfigAction = { it.compilerArguments[CommonCompilerArguments.PROGRESSIVE] = false },
            )
            module.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = false },
                compilationConfigAction = { it.compilerArguments[CommonCompilerArguments.PROGRESSIVE] = true },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Compiler arg change is ignored when tracking is disabled in subsequent build")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testCompilerArgChangeDoesNotTriggerRebuildAfterTrackingDisabled(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = true },
                compilationConfigAction = { it.compilerArguments[CommonCompilerArguments.PROGRESSIVE] = false },
            )
            module.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
                icOptionsConfigAction = { it[TRACK_CONFIGURATION_INPUTS] = false },
                compilationConfigAction = { it.compilerArguments[CommonCompilerArguments.PROGRESSIVE] = true },
            ) {
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed.*".toRegex(),
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Enabling tracking in subsequent build triggers rebuild due to missing snapshot")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testEnablingTrackingInSubsequentBuildTriggersRebuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = false
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
                },
            )
            module.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                icOptionsConfigAction = {
                    it[TRACK_CONFIGURATION_INPUTS] = true
                    it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = false
                },
            ) {
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: ${BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS.readableString}".toRegex(),
                )
            }
        }
    }

    private fun AbstractProject<*, *, *>.expectedLogLevel(): LogLevel =
        if (defaultStrategyConfig is ExecutionPolicy.WithDaemon) LogLevel.DEBUG else LogLevel.INFO // TODO: KT-85024

    private fun AbstractProject<*, *, *>.assumeNotMetadata() {
        assumeFalse(this is MetadataProject)
    }
}
