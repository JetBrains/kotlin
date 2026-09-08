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
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerArguments.Companion.X_WASM_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.wasmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinWasmStdlibKlibLocation
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinWasmWasiStdlibKlibLocation
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalCompilerArgument::class)
@DisplayName("Functional tests for the Wasm linking operation of the BTA")
class WasmLinkingTest : BaseCompilationTest() {

    @DisplayName("An unpacked klib is accepted as the Wasm linking input")
    @BtaV2StrategyAgnosticCompilationTest
    fun linksAnUnpackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            module.link {
                assertOutputFileCountWithExtension(".wasm", 1)
                assertOutputFileCountWithExtension(".mjs", 3)
                assertOutputsContains(
                    module.expectedOutputFileName,
                    "${module.moduleName}.mjs",
                    "${module.moduleName}.import-object.mjs",
                    "${module.moduleName}.js-builtins.mjs",
                )
            }
        }
    }

    @DisplayName("A packed klib (NOPACK=false) is accepted as the Wasm linking input")
    @BtaV2StrategyAgnosticCompilationTest
    fun linksAPackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile(compilationConfigAction = { it.compilerArguments[NOPACK] = false })
            module.link {
                assertOutputFileCountWithExtension(".wasm", 1)
                assertOutputFileCountWithExtension(".mjs", 3)
                assertOutputsContains(
                    module.expectedOutputFileName,
                    "${module.moduleName}.mjs",
                    "${module.moduleName}.import-object.mjs",
                    "${module.moduleName}.js-builtins.mjs",
                )
            }
        }
    }

    @DisplayName("A wasm-wasi klib links into a wasm-wasi artifact")
    @BtaV2StrategyAgnosticCompilationTest
    fun wasmWasiTargetLinks(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib", stdlibClasspath = listOf(currentKotlinWasmWasiStdlibKlibLocation))
            module.compile(compilationConfigAction = {
                it.compilerArguments[X_WASM_TARGET] = WasmTarget.WASM_WASI
            })
            module.link(compilationConfigAction = {
                it.compilerArguments[X_WASM_TARGET] = WasmTarget.WASM_WASI
            }) {
                assertOutputFileCountWithExtension(".wasm", 1)
                assertOutputFileCountWithExtension(".mjs", 1)
                assertOutputsContains(module.expectedOutputFileName, "${module.moduleName}.mjs")
                assertOutputFileContains("${module.moduleName}.mjs", "import { WASI } from 'wasi'")
            }
        }
    }

    @DisplayName("The linked Wasm artifact name comes from the linking IR_OUTPUT_NAME, independent of the compiled klib name")
    @BtaV2StrategyAgnosticCompilationTest
    fun linkedWasmArtifactNameComesFromIrOutputName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile(compilationConfigAction = {
                it.compilerArguments[NOPACK] = false
            }) {
                assertIsPackedKlib("js-ic-basic-lib")
            }
            module.link(compilationConfigAction = {
                it.compilerArguments[IR_OUTPUT_NAME] = "wasm-lib"
            }) {
                assertOutputsContains("wasm-lib.wasm")
            }
        }
    }

    @DisplayName("A lib+app graph links each module to its own Wasm and the app resolves the library across modules")
    @BtaV2StrategyAgnosticCompilationTest
    fun multiModuleGraphLinksEachModuleToItsOwnArtifact(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val library = module("js-ic-basic-lib")
            val app = module("js-ic-basic-app", dependencies = listOf(library))

            library.compile()
            app.compile()

            library.link {
                assertOutputsContains(library.expectedOutputFileName)
            }

            app.link(compilationConfigAction = {
                it.compilerArguments[X_PARTIAL_LINKAGE_LOGLEVEL] = PartialLinkageLogLevel.ERROR
            }) {
                assertOutputsContains(app.expectedOutputFileName)
            }
        }
    }

    @DisplayName("A missing dependency fails Wasm linking when partial linkage is escalated to ERROR")
    @BtaV2StrategyAgnosticCompilationTest
    fun missingDependencyFailsLinkingWhenPartialLinkageIsError(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val library = module("missing-dep/lib")
            val app = module("missing-dep/app", dependencies = listOf(library))

            library.compile()
            app.compile()

            app.link(compilationConfigAction = {
                // drop the library from the linker classpath so the dependency is genuinely missing at link time
                it.compilerArguments[LIBRARIES] = listOf(currentKotlinWasmStdlibKlibLocation)
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
}
