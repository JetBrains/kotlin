/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation

class SandboxPluginTest : BaseCompilationTest() {

//    @DefaultStrategyAndPlatformAgnosticScenarioTest
//    @DisplayName("KT-87217 and KT-87370 Generating top-level callables from compiler plugins shouldn't break IC")
//    fun testGeneratingTopLevelCallables(scenario: ScenarioCreator) {
//        scenario {
//            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
//                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
//            })
//
//            module.compile {}
//            repeat(3) { i ->
//                module.replaceFileWithVersion("main.kt", "step${i + 1}")
//                module.compile {
//                    assertCompiledSources("main.kt")
//                    assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
//                }
//            }
//        }
//    }
}
