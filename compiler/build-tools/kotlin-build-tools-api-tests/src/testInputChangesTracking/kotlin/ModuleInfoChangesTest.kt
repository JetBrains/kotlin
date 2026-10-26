/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectedLogLevelForRebuildReason
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class ModuleInfoChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing a 'requires' from module-info.java makes compilation fail (internally tracked)")
    @TestMetadata("ic-scenarios/module-info-modification")
    fun testModuleInfoRequiresRemovalFailsCompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(
                moduleName = "ic-scenarios/module-info-modification",
                compilationConfigAction = jpmsSupportConfigAction,
            ).removeRequiresAndAssertCompilationFails(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing a 'requires' from module-info.java makes compilation fail (externally tracked)")
    @TestMetadata("ic-scenarios/module-info-modification")
    fun testModuleInfoRequiresRemovalFailsCompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(
                moduleName = "ic-scenarios/module-info-modification",
                compilationConfigAction = jpmsSupportConfigAction,
            ).removeRequiresAndAssertCompilationFails(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing module-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/module-info-modification")
    fun testModuleInfoRemovalCausesRecompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(
                moduleName = "ic-scenarios/module-info-modification",
                compilationConfigAction = jpmsSupportConfigAction,
            ).removeModuleInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing module-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/module-info-modification")
    fun testModuleInfoRemovalCausesRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(
                moduleName = "ic-scenarios/module-info-modification",
                compilationConfigAction = jpmsSupportConfigAction,
            ).removeModuleInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding module-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/module-info-modification")
    fun testModuleInfoAdditionCausesRecompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(
                moduleName = "ic-scenarios/module-info-modification",
                compilationConfigAction = jpmsSupportConfigAction,
            ).addModuleInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding module-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/module-info-modification")
    fun testModuleInfoAdditionCausesRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(
                moduleName = "ic-scenarios/module-info-modification",
                compilationConfigAction = jpmsSupportConfigAction,
            ).addModuleInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    private fun ScenarioModule.removeRequiresAndAssertCompilationFails(strategyConfig: CompilerExecutionStrategyConfiguration) {
        // `a.kt` uses `java.sql.SQLException`, which is only accessible because `module-info.java`
        // has `requires java.sql;`. Removing that requires must recompile `a.kt` and fail with a JPMS error.
        replaceFileWithVersion("module-info.java", "remove-requires")
        compile {
            expectFail()
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.MODULE_INFO_CHANGED.readableString}"
            )
            assertLogContainsPatterns(LogLevel.ERROR, ".*/a.kt:5:39 Unresolved reference 'SQLException'.".toRegex())
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    private fun ScenarioModule.removeModuleInfoAndAssertFullRecompilation(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        deleteFile("module-info.java")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.MODULE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    private fun ScenarioModule.addModuleInfoAndAssertFullRecompilation(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        deleteFile("module-info.java")
        compile()
        createPredefinedFile("module-info.java", "add-module-info")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.MODULE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing a 'requires' from a module-info.java located in the 'java' source directory makes compilation fail (internally tracked)")
    @TestMetadata("ic-scenarios/module-info-in-java-source-directory")
    fun testModuleInfoInJavaSourceDirectoryRequiresRemovalFailsCompilationInternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            trackedModule(
                moduleName = "ic-scenarios/module-info-in-java-source-directory",
                compilationConfigAction = jpmsSupportConfigAction,
            ).removeRequiresInJavaSourceDirectoryAndAssertCompilationFails(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing a 'requires' from a module-info.java located in the 'java' source directory makes compilation fail (externally tracked)")
    @TestMetadata("ic-scenarios/module-info-in-java-source-directory")
    fun testModuleInfoInJavaSourceDirectoryRequiresRemovalFailsCompilationExternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            module(
                moduleName = "ic-scenarios/module-info-in-java-source-directory",
                compilationConfigAction = jpmsSupportConfigAction,
            ).removeRequiresInJavaSourceDirectoryAndAssertCompilationFails(strategyConfig)
        }
    }

    private fun ScenarioModule.removeRequiresInJavaSourceDirectoryAndAssertCompilationFails(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        replaceFileWithVersion("java/module-info.java", "remove-requires")
        compile {
            expectFail()
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.MODULE_INFO_CHANGED.readableString}"
            )
            assertLogContainsPatterns(LogLevel.ERROR, ".*/a\\.kt:\\d+:\\d+ Unresolved reference 'SQLException'.*".toRegex())
            assertCompiledSources("kotlin/a.kt", "kotlin/b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Fixing one module-info.java error keeps reporting the remaining ones (internally tracked)")
    @TestMetadata("ic-scenarios/module-info-partial-error-fix")
    fun testPartialModuleInfoFixKeepsRemainingErrorsInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(
                moduleName = "ic-scenarios/module-info-partial-error-fix",
                compilationConfigAction = jpmsSupportConfigAction,
            ).fixOneRequiresAndAssertRemainingErrorIsReported(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Fixing one module-info.java error keeps reporting the remaining ones (externally tracked)")
    @TestMetadata("ic-scenarios/module-info-partial-error-fix")
    fun testPartialModuleInfoFixKeepsRemainingErrorsExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(
                moduleName = "ic-scenarios/module-info-partial-error-fix",
                compilationConfigAction = jpmsSupportConfigAction,
            ).fixOneRequiresAndAssertRemainingErrorIsReported(strategyConfig)
        }
    }

    private fun ScenarioModule.fixOneRequiresAndAssertRemainingErrorIsReported(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        replaceFileWithVersion("module-info.java", "remove-both-requires")
        compile {
            expectFail()
            assertLogContainsPatterns(
                LogLevel.ERROR,
                ".*/a\\.kt:\\d+:\\d+ Unresolved reference 'SQLException'\\..*".toRegex(),
                ".*/b\\.kt:\\d+:\\d+ Unresolved reference 'ObjectName'\\..*".toRegex(),
            )
        }
        replaceFileWithVersion("module-info.java", "restore-sql-requires")
        compile {
            expectFail()
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.MODULE_INFO_CHANGED.readableString}"
            )
            assertLogContainsPatterns(LogLevel.ERROR, ".*/b\\.kt:\\d+:\\d+ Unresolved reference 'ObjectName'\\..*".toRegex())
            assertLogDoesNotContainPatterns(LogLevel.ERROR, ".*/a\\.kt:\\d+:\\d+ Unresolved reference 'SQLException'\\..*".toRegex())
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing an 'exports' from a dependency's module-info.java recompiles and fails the consumer's user of the package (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-modification")
    fun testDependencyModuleInfoExportsRemovalFailsConsumerExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-module-info-modification/module-a",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-module-info-modification/module-b",
                dependencies = listOf(dependency),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            dependency.removeExportsAndAssertConsumerFails(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing an 'exports' from a dependency's module-info.java recompiles and fails the consumer's user of the package (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-modification")
    fun testDependencyModuleInfoExportsRemovalFailsConsumerInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-module-info-modification/module-a",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-module-info-modification/module-b",
                dependencies = listOf(dependency),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            dependency.removeExportsAndAssertConsumerFails(consumer)
        }
    }

    private fun ScenarioModule.removeExportsAndAssertConsumerFails(consumer: ScenarioModule) {
        replaceFileWithVersion("module-info.java", "remove-exports")
        compile()
        consumer.compile {
            expectFail()
            assertLogContainsPatterns(LogLevel.ERROR, ".*/UseA\\.kt:\\d+:\\d+ .*does not export.*".toRegex())
            assertCompiledSources("bpkg/UseA.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A dependency's module-info.java change recompiles only the consumer's users of the dependency (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-unrelated-change")
    fun testDependencyModuleInfoChangeRecompilesOnlyUsersInNonModularConsumerExternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-module-info-unrelated-change/module-a",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-module-info-unrelated-change/module-b",
                dependencies = listOf(dependency),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            dependency.addRequiresAndAssertOnlyUsersAreRecompiled(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A dependency's module-info.java change recompiles only the consumer's users of the dependency (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-unrelated-change")
    fun testDependencyModuleInfoChangeRecompilesOnlyUsersInNonModularConsumerInternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-module-info-unrelated-change/module-a",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-module-info-unrelated-change/module-b",
                dependencies = listOf(dependency),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            dependency.addRequiresAndAssertOnlyUsersAreRecompiled(consumer)
        }
    }

    private fun ScenarioModule.addRequiresAndAssertOnlyUsersAreRecompiled(consumer: ScenarioModule) {
        replaceFileWithVersion("module-info.java", "add-requires")
        compile()
        consumer.compile {
            assertCompiledSources("bpkg/UseA.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A dependency becoming a JPMS module recompiles only the consumer's users of the dependency (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-addition")
    fun testDependencyModuleInfoAdditionRecompilesOnlyUsersExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-module-info-addition/module-a",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-module-info-addition/module-b",
                dependencies = listOf(dependency),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            dependency.addModuleInfoAndAssertOnlyUsersAreRecompiled(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A dependency becoming a JPMS module recompiles only the consumer's users of the dependency (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-addition")
    fun testDependencyModuleInfoAdditionRecompilesOnlyUsersInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-module-info-addition/module-a",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-module-info-addition/module-b",
                dependencies = listOf(dependency),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            dependency.addModuleInfoAndAssertOnlyUsersAreRecompiled(consumer)
        }
    }

    private fun ScenarioModule.addModuleInfoAndAssertOnlyUsersAreRecompiled(consumer: ScenarioModule) {
        createPredefinedFile("module-info.java", "add-module-info")
        compile()
        consumer.compile {
            assertCompiledSources("bpkg/UseA.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Dropping 'transitive' from a 'requires' in an intermediate module recompiles the consumer's users of both modules (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-requires-transitive")
    fun testDependencyRequiresTransitiveRemovalRecompilesUsersExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val base = module(
                moduleName = "ic-scenarios/dependency-module-info-requires-transitive/module-base",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val intermediate = module(
                moduleName = "ic-scenarios/dependency-module-info-requires-transitive/module-intermediate",
                dependencies = listOf(base),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-module-info-requires-transitive/module-consumer",
                dependencies = listOf(base, intermediate),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            intermediate.dropTransitiveAndAssertUsersAreRecompiled(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Dropping 'transitive' from a 'requires' in an intermediate module recompiles the consumer's users of both modules (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-requires-transitive")
    fun testDependencyRequiresTransitiveRemovalRecompilesUsersInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val base = module(
                moduleName = "ic-scenarios/dependency-module-info-requires-transitive/module-base",
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val intermediate = module(
                moduleName = "ic-scenarios/dependency-module-info-requires-transitive/module-intermediate",
                dependencies = listOf(base),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-module-info-requires-transitive/module-consumer",
                dependencies = listOf(base, intermediate),
                compilationConfigAction = jpmsSupportConfigAction,
            )
            intermediate.dropTransitiveAndAssertUsersAreRecompiled(consumer)
        }
    }

    private fun ScenarioModule.dropTransitiveAndAssertUsersAreRecompiled(consumer: ScenarioModule) {
        replaceFileWithVersion("module-info.java", "drop-transitive")
        compile()
        consumer.compile {
            assertCompiledSources("cpkg/UseBase.kt", "cpkg/UseIntermediate.kt")
        }
    }

    companion object {
        private val jpmsSupportConfigAction: (JvmCompilationOperation.Builder) -> Unit = {
            it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_9
        }
    }
}
