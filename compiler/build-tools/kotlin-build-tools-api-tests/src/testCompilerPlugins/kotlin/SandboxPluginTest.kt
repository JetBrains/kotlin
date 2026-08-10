/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.testFederation.AffectedByCompilerPlugins
import org.junit.jupiter.api.DisplayName

@AffectedByCompilerPlugins
class SandboxPluginTest : BaseCompilationTest() {

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-87217 and KT-87370 Generating top-level callables from compiler plugins shouldn't break IC")
    fun testGeneratingTopLevelCallables(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.compile {}
            repeat(3) { i ->
                module.replaceFileWithVersion("main.kt", "step${i + 1}")
                module.compile {
                    assertCompiledSources("main.kt")
                    assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
                }
            }
        }
    }
}
