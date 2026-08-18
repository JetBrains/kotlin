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
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.X_IR_DCE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments.Companion.X_PARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.MODULE_KIND
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_ES_LONG_AS_BIGINT
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_IR_PER_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_IR_PER_MODULE
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.jsProject
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.currentKotlinJsStdlibKlibLocation
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

    @DisplayName("MODULE_KIND=COMMONJS wraps the generated JS as a CommonJS module")
    @BtaV2StrategyAgnosticCompilationTest
    fun moduleKindCommonJsShapesTheGeneratedJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            val jsFileName = module.expectedOutputFileName
            module.link(compilationConfigAction = { it.compilerArguments[MODULE_KIND] = JsModuleKind.COMMONJS }) {
                assertOutputFileContains(jsFileName, "module.exports")
                assertOutputFileDoesNotContain(jsFileName, "define.amd")
            }
        }
    }

    @DisplayName("MODULE_KIND=UMD wraps the generated JS with the AMD/CommonJS detector")
    @BtaV2StrategyAgnosticCompilationTest
    fun moduleKindUmdShapesTheGeneratedJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            val jsFileName = module.expectedOutputFileName
            module.link(compilationConfigAction = { it.compilerArguments[MODULE_KIND] = JsModuleKind.UMD }) {
                assertOutputFileContains(jsFileName, "define.amd")
            }
        }
    }

    @DisplayName("MODULE_KIND=PLAIN wraps the generated JS as a plain global assignment")
    @BtaV2StrategyAgnosticCompilationTest
    fun moduleKindPlainShapesTheGeneratedJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            val jsFileName = module.expectedOutputFileName
            module.link(compilationConfigAction = { it.compilerArguments[MODULE_KIND] = JsModuleKind.PLAIN }) {
                assertOutputFileDoesNotContain(jsFileName, "module.exports")
                assertOutputFileDoesNotContain(jsFileName, "define(")
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

    @DisplayName("MODULE_KIND=AMD wraps the generated JS as an AMD module")
    @BtaV2StrategyAgnosticCompilationTest
    fun moduleKindAmdShapesTheGeneratedJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            val jsFileName = module.expectedOutputFileName
            module.link(compilationConfigAction = { it.compilerArguments[MODULE_KIND] = JsModuleKind.AMD }) {
                assertOutputFileContains(jsFileName, "define(")
                assertOutputFileDoesNotContain(jsFileName, "define.amd")
            }
        }
    }

    @DisplayName("SOURCE_MAP produces a source map next to the generated JS")
    @BtaV2StrategyAgnosticCompilationTest
    fun sourceMapIsGeneratedWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            val sourceMapFileName = "${module.expectedOutputFileName}.map"

            module.link(compilationConfigAction = { it.compilerArguments[SOURCE_MAP] = true }) {
                assertOutputsContains(module.expectedOutputFileName, sourceMapFileName)
                assertOutputFileContains(module.expectedOutputFileName, "//# sourceMappingURL=${module.expectedOutputFileName}.map")
                // a plain source map references the sources but does not embed their contents
                assertOutputFileDoesNotContain(sourceMapFileName, "fun useAInLibMain")
            }
        }
    }

    @DisplayName("SOURCE_MAP_EMBED_SOURCES=ALWAYS embeds Kotlin sources into the generated source map")
    @BtaV2StrategyAgnosticCompilationTest
    fun sourceMapEmbedsSourcesWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile()
            val sourceMapFileName = "${module.expectedOutputFileName}.map"

            // embedding the sources inlines the Kotlin source text into the source map
            module.link(compilationConfigAction = {
                it.compilerArguments[SOURCE_MAP] = true
                it.compilerArguments[SOURCE_MAP_EMBED_SOURCES] = SourceMapEmbedSources.ALWAYS
            }) {
                assertOutputFileContains(sourceMapFileName, "fun useAInLibMain")
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

    @DisplayName("An internal declaration of a friend module is resolved across modules and linked into the app JS")
    @BtaV2StrategyAgnosticCompilationTest
    fun friendModuleInternalDeclarationIsLinkedIntoTheAppJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
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
                assertOutputFileContains(app.expectedOutputFileName, "useInternalGreeting")
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

    @DisplayName("A missing dependency only warns when partial linkage is at WARNING")
    @BtaV2StrategyAgnosticCompilationTest
    fun missingDependencyOnlyWarnsWhenPartialLinkageIsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val library = module("missing-dep/lib")
            val app = module("missing-dep/app", dependencies = listOf(library))

            library.compile()
            app.compile()

            app.link(compilationConfigAction = {
                it.compilerArguments[LIBRARIES] = listOf(currentKotlinJsStdlibKlibLocation)
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

    @DisplayName("X_ES_LONG_AS_BIGINT compiles Long constants as ES bigint literals")
    @BtaV2StrategyAgnosticCompilationTest
    fun longsAreCompiledAsBigIntWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val longDecimalLiteral = "1234567890123456789"
            val bigIntLiteral = "${longDecimalLiteral}n"
            val module = module("js-long-as-bigint")

            module.compile()

            module.link {
                assertOutputFileDoesNotContain(module.expectedOutputFileName, bigIntLiteral)
                assertOutputFileDoesNotContain(module.expectedOutputFileName, longDecimalLiteral)
            }

            module.link(compilationConfigAction = { it.compilerArguments[X_ES_LONG_AS_BIGINT] = true }) {
                assertOutputFileContains(module.expectedOutputFileName, bigIntLiteral)
            }
        }
    }

    @DisplayName("MAIN=CALL invokes main() in the generated JS")
    @BtaV2StrategyAgnosticCompilationTest
    fun mainCallModeCallInvokesMain(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-with-main")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[MAIN] = JsMainCallMode.CALL }) {
                assertOutputFileContains(module.expectedOutputFileName, "mainWrapper();")
            }
        }
    }

    @DisplayName("MAIN=NO_CALL does not invoke main() in the generated JS")
    @BtaV2StrategyAgnosticCompilationTest
    fun mainCallModeNoCallDoesNotInvokeMain(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-with-main")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[MAIN] = JsMainCallMode.NO_CALL }) {
                assertOutputFileDoesNotContain(module.expectedOutputFileName, "mainWrapper();")
            }
        }
    }

    @DisplayName("Without X_IR_DCE the unreachable declaration is kept in the generated JS")
    @BtaV2StrategyAgnosticCompilationTest
    fun unreachableDeclarationsAreKeptWithoutDce(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-with-main")
            module.compile()

            module.link(compilationConfigAction = { it.compilerArguments[MAIN] = JsMainCallMode.CALL }) {
                assertOutputFileContains(module.expectedOutputFileName, "usedByMainMarker")
                assertOutputFileContains(module.expectedOutputFileName, "deadUnusedFunctionMarker")
            }
        }
    }

    @DisplayName("X_IR_DCE eliminates unreachable declarations from the generated JS")
    @BtaV2StrategyAgnosticCompilationTest
    fun dceEliminatesUnreachableDeclarations(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-with-main")
            module.compile()

            module.link(compilationConfigAction = {
                it.compilerArguments[MAIN] = JsMainCallMode.CALL
                it.compilerArguments[X_IR_DCE] = true
            }) {
                assertOutputFileContains(module.expectedOutputFileName, "usedByMainMarker")
                assertOutputFileDoesNotContain(module.expectedOutputFileName, "deadUnusedFunctionMarker")
            }
        }
    }
}
