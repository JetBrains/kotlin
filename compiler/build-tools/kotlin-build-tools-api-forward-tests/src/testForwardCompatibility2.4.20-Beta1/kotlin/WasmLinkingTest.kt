/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.MAIN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.SOURCE_MAP
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.X_GENERATE_DTS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.X_IR_DCE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments.Companion.X_PARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments.Companion.X_WASM_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments.Companion.X_WASM_DEBUGGER_CUSTOM_FORMATTERS
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments.Companion.X_WASM_GENERATE_DWARF
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments.Companion.X_WASM_GENERATE_WAT
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.wasmProject
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.currentKotlinWasmStdlibKlibLocation
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.currentKotlinWasmWasiStdlibKlibLocation
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
                // X_WASM_TARGET is not available on the linking arguments, so the wasm-wasi target cannot be set on the
                // linking operation through the public typed API; we set it via a cast to the shared underlying builder.
                // Once KT-88684 is fixed, drop the cast and set the target through the typed linking arguments.
                (it.compilerArguments as WasmCompilerKlibArguments.Builder)[X_WASM_TARGET] = WasmTarget.WASM_WASI
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

    @DisplayName("X_WASM_GENERATE_WAT emits a .wat text module")
    @BtaV2StrategyAgnosticCompilationTest
    fun watFileIsGeneratedWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[X_WASM_GENERATE_WAT] = true }) {
                val watFile = "${module.moduleName}.wat"
                assertOutputFileCountWithExtension(".wat", 1)
                assertOutputsContains(module.expectedOutputFileName, watFile)
            }
        }
    }

    @DisplayName("X_GENERATE_DTS emits a TypeScript declaration file")
    @BtaV2StrategyAgnosticCompilationTest
    fun dtsFileIsGeneratedWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[X_GENERATE_DTS] = true }) {
                assertOutputFileCountWithExtension(".d.mts", 1)
                assertOutputsContains(module.expectedOutputFileName, "${module.moduleName}.d.mts")
            }
        }
    }

    @DisplayName("X_WASM_GENERATE_DWARF adds DWARF debug sections to the generated Wasm")
    @BtaV2StrategyAgnosticCompilationTest
    fun dwarfIsGeneratedWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-with-main")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[X_WASM_GENERATE_DWARF] = true }) {
                assertOutputsContains(module.expectedOutputFileName)
                assertOutputFileBytesContain(module.expectedOutputFileName, ".debug_info")
            }
        }
    }

    @DisplayName("X_WASM_DEBUGGER_CUSTOM_FORMATTERS emits a custom-formatters.js file")
    @BtaV2StrategyAgnosticCompilationTest
    fun customFormattersFileIsGeneratedWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[X_WASM_DEBUGGER_CUSTOM_FORMATTERS] = true }) {
                assertOutputsContains(module.expectedOutputFileName, "custom-formatters.js")
            }
        }
    }

    @DisplayName("SOURCE_MAP produces a source map next to the generated Wasm artifact")
    @BtaV2StrategyAgnosticCompilationTest
    fun sourceMapIsGeneratedWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            val sourceMapFileName = "${module.expectedOutputFileName}.map"

            module.link(compilationConfigAction = { it.compilerArguments[SOURCE_MAP] = true }) {
                assertOutputsContains(module.expectedOutputFileName, sourceMapFileName)
                assertOutputFileBytesContain(module.expectedOutputFileName, sourceMapFileName)
                assertOutputFileContains(sourceMapFileName, "useAInLibMain.kt")
            }
        }
    }

    @DisplayName("Without X_IR_DCE the unreachable declaration is kept in the generated Wasm")
    @BtaV2StrategyAgnosticCompilationTest
    fun unreachableDeclarationsAreKeptWithoutDce(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-with-main")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[MAIN] = JsMainCallMode.CALL }) {
                assertOutputFileBytesContain(module.expectedOutputFileName, "usedByMainMarker")
                assertOutputFileBytesContain(module.expectedOutputFileName, "deadUnusedFunctionMarker")
            }
        }
    }

    @DisplayName("X_IR_DCE eliminates unreachable declarations from the generated Wasm")
    @BtaV2StrategyAgnosticCompilationTest
    fun dceEliminatesUnreachableDeclarations(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-with-main")
            module.compile()

            module.link(compilationConfigAction = {
                it.compilerArguments[MAIN] = JsMainCallMode.CALL
                it.compilerArguments[X_IR_DCE] = true
            }) {
                assertOutputFileBytesContain(module.expectedOutputFileName, "usedByMainMarker")
                assertOutputFileBytesDoNotContain(module.expectedOutputFileName, "deadUnusedFunctionMarker")
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

    @DisplayName("An internal declaration of a friend module is resolved across modules and linked into the app Wasm")
    @BtaV2StrategyAgnosticCompilationTest
    fun friendModuleInternalDeclarationIsLinked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val library = module("friend-modules/lib")
            val app = module("friend-modules/app", dependencies = listOf(library))

            library.compile()
            app.compile(compilationConfigAction = {
                it.compilerArguments[X_FRIEND_MODULES] = listOf(library.outputDirectory)
            })

            app.link(compilationConfigAction = {
                it.compilerArguments[X_PARTIAL_LINKAGE_LOGLEVEL] = PartialLinkageLogLevel.ERROR
            }) {
                assertOutputsContains(app.expectedOutputFileName)
                assertOutputFileBytesContain(app.expectedOutputFileName, "hello from the internal API")
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

    @DisplayName("A missing dependency only warns when partial linkage is at WARNING")
    @BtaV2StrategyAgnosticCompilationTest
    fun missingDependencyOnlyWarnsWhenPartialLinkageIsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val library = module("missing-dep/lib")
            val app = module("missing-dep/app", dependencies = listOf(library))

            library.compile()
            app.compile()

            app.link(compilationConfigAction = {
                // drop the library from the linker classpath so the dependency is genuinely missing at link time
                it.compilerArguments[LIBRARIES] = listOf(currentKotlinWasmStdlibKlibLocation)
                it.compilerArguments[X_PARTIAL_LINKAGE_LOGLEVEL] = PartialLinkageLogLevel.WARNING
            }) {
                assertOutputsContains(app.expectedOutputFileName)
                assertLogContainsPatterns(
                    LogLevel.WARN,
                    ".*Function 'formatGreeting' can not be called: No function found for symbol.*"
                        .toRegex(RegexOption.DOT_MATCHES_ALL),
                )
            }
        }
    }
}
