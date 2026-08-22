/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments.Companion.X_PARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.MODULE_KIND
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_IR_PER_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_IR_PER_MODULE
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinJsStdlibKlibLocation
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalCompilerArgument::class)
@DisplayName("Functional tests for the JS linking operation of the BTA")
class JsLinkingTest : BaseCompilationTest() {

    @DisplayName("An unpacked klib is accepted as the linking input")
    @BtaV2StrategyAgnosticCompilationTest
    fun linksAnUnpackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            module.link {
                assertOutputFileCountWithExtension(".js", 1)
                assertOutputsContains(module.expectedOutputFileName)
                assertOutputFileContains(module.expectedOutputFileName, "useAInLibMain")
            }
        }
    }

    @DisplayName("A packed klib (NOPACK=false) is accepted as the linking input")
    @BtaV2StrategyAgnosticCompilationTest
    fun linksAPackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile(compilationConfigAction = { it.compilerArguments[NOPACK] = false })
            module.link {
                assertOutputFileCountWithExtension(".js", 1)
                assertOutputsContains(module.expectedOutputFileName)
                assertOutputFileContains(module.expectedOutputFileName, "useAInLibMain")
            }
        }
    }

    @DisplayName("The linked JS file name comes from the linking IR_OUTPUT_NAME, independent of the compiled klib name")
    @BtaV2StrategyAgnosticCompilationTest
    fun linkedJsFileNameComesFromTheLinkingIrOutputName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile(compilationConfigAction = {
                it.compilerArguments[NOPACK] = false
            }) {
                assertIsPackedKlib("js-ic-basic-lib")
            }
            module.link(compilationConfigAction = {
                it.compilerArguments[IR_OUTPUT_NAME] = "linked-js-name"
            }) {
                assertOutputsContains("linked-js-name.js")
                assertOutputFileContains("linked-js-name.js", "useAInLibMain")
            }
        }
    }

    @DisplayName("MODULE_KIND=ES writes the generated JS with the .mjs extension")
    @BtaV2StrategyAgnosticCompilationTest
    fun moduleKindEsShapesTheGeneratedJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            module.link(compilationConfigAction = { it.compilerArguments[MODULE_KIND] = JsModuleKind.ES }) {
                assertOutputsContains("${module.moduleName}.mjs")
            }
        }
    }

    @DisplayName("A lib+app graph links each module to its own JS and the app resolves the library across modules")
    @BtaV2StrategyAgnosticCompilationTest
    fun multiModuleGraphLinksEachModuleToItsOwnJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val library = module("js-ic-basic-lib")
            val app = module("js-ic-basic-app", dependencies = listOf(library))

            library.compile()
            app.compile()

            library.link {
                assertOutputsContains(library.expectedOutputFileName)
                assertOutputFileContains(library.expectedOutputFileName, "useAInLibMain")
            }

            app.link(compilationConfigAction = {
                it.compilerArguments[X_PARTIAL_LINKAGE_LOGLEVEL] = PartialLinkageLogLevel.ERROR
            }) {
                assertOutputsContains(app.expectedOutputFileName)
                assertOutputFileContains(app.expectedOutputFileName, "useAInAppMain")
            }
        }
    }

    @DisplayName("A missing dependency fails linking when partial linkage is escalated to ERROR")
    @BtaV2StrategyAgnosticCompilationTest
    fun missingDependencyFailsLinkingWhenPartialLinkageIsError(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val library = module("missing-dep/lib")
            val app = module("missing-dep/app", dependencies = listOf(library))

            library.compile()
            app.compile()

            app.link(compilationConfigAction = {
                it.compilerArguments[LIBRARIES] = listOf(currentKotlinJsStdlibKlibLocation)
                it.compilerArguments[X_PARTIAL_LINKAGE_LOGLEVEL] = PartialLinkageLogLevel.ERROR
            }) {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    ".*Function 'formatGreeting' can not be called: No function found for symbol.*"
                        .toRegex(RegexOption.DOT_MATCHES_ALL),
                )
            }
        }
    }

    @DisplayName("Per-module granularity produces one JS file per module")
    @BtaV2StrategyAgnosticCompilationTest
    fun perModuleGranularityProducesMultipleJsFiles(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            // per-module granularity emits a separate JS file per module (library and stdlib)
            module.link(compilationConfigAction = { it.compilerArguments[X_IR_PER_MODULE] = true }) {
                assertOutputFileCountWithExtension(".js", 2)
                assertOutputsContains(module.expectedOutputFileName)
                assertOutputsContains("kotlin-kotlin-stdlib.js")
            }
        }
    }

    @DisplayName("Per-file granularity emits a separate JS file per source file")
    @BtaV2StrategyAgnosticCompilationTest
    fun perFileGranularityProducesOneJsFilePerSourceFile(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            // per-file granularity is only supported together with the ES module kind
            module.link(compilationConfigAction = {
                it.compilerArguments[MODULE_KIND] = JsModuleKind.ES
                it.compilerArguments[X_IR_PER_FILE] = true
            }) {
                val perFileDir = "kotlin_${module.moduleName.replace('-', '_')}"
                assertOutputsContains("$perFileDir/A.mjs", "$perFileDir/DummyInLibMain.mjs")
            }
        }
    }
}
