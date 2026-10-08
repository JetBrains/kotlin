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
import kotlin.io.path.Path

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
            assertLogContainsPatterns(LogLevel.ERROR, ".*/a.kt:5:30 Unresolved reference 'SQLException'.".toRegex())
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
    @DisplayName("Removing an 'exports' from a dependency's module-info.java recompiles and fails the consumer (externally tracked)")
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
            dependency.removeExportsAndAssertConsumerFails(consumer, strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing an 'exports' from a dependency's module-info.java recompiles and fails the consumer (internally tracked)")
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
            dependency.removeExportsAndAssertConsumerFails(consumer, strategyConfig)
        }
    }

    private fun ScenarioModule.removeExportsAndAssertConsumerFails(
        consumer: ScenarioModule,
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        replaceFileWithVersion("module-info.java", "remove-exports")
        compile()
        consumer.compile {
            expectFail()
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.DEPENDENCY_MODULE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("bpkg/UseA.kt", "bpkg/Other.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A dependency's module-info.java change unrelated to the consumer still recompiles it fully (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-unrelated-change")
    fun testUnrelatedDependencyModuleInfoChangeRecompilesNonModularConsumerExternallyTracked(
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
            dependency.addUnrelatedRequiresAndAssertConsumerIsFullyRecompiled(consumer, strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A dependency's module-info.java change unrelated to the consumer still recompiles it fully (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-module-info-unrelated-change")
    fun testUnrelatedDependencyModuleInfoChangeRecompilesNonModularConsumerInternallyTracked(
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
            dependency.addUnrelatedRequiresAndAssertConsumerIsFullyRecompiled(consumer, strategyConfig)
        }
    }

    private fun ScenarioModule.addUnrelatedRequiresAndAssertConsumerIsFullyRecompiled(
        consumer: ScenarioModule,
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        replaceFileWithVersion("module-info.java", "add-requires")
        compile()
        consumer.compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.DEPENDENCY_MODULE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("bpkg/UseA.kt", "bpkg/Unrelated.kt")
        }
    }

    companion object {
        private val jpmsSupportConfigAction: (JvmCompilationOperation.Builder) -> Unit = {
            it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_9
            it.compilerArguments[JvmCompilerArguments.JDK_HOME] = Path(System.getenv("JDK_11_0"))
        }
    }
}
