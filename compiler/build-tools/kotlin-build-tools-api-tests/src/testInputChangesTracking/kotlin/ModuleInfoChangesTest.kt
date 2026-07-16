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

    companion object {
        private val jpmsSupportConfigAction: (JvmCompilationOperation.Builder) -> Unit = {
            it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_9
            it.compilerArguments[JvmCompilerArguments.JDK_HOME] = Path(System.getenv("JDK_11_0"))
        }
    }
}
