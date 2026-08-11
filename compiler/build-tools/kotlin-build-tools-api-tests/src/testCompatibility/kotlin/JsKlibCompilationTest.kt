/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_IR_MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerKlibArguments.Companion.X_IR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalCompilerArgument::class)
@DisplayName("Functional tests for the JS klib compilation operation of the BTA")
class JsKlibCompilationTest : BaseCompilationTest() {

    @DisplayName("Compiling JS sources produces an unpacked klib with IR and metadata fragments")
    @BtaV2StrategyAgnosticCompilationTest
    fun compilesToUnpackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            module("js-ic-basic-lib").compile {
                assertIsUnpackedJsKlib()
                assertKnmFileCount(expectedCount = 3)
            }
        }
    }

    @DisplayName("Disabled NOPACK produces a packed klib file instead of a directory")
    @BtaV2StrategyAgnosticCompilationTest
    fun packedKlibIsProducedWhenNopackIsDisabled(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile(compilationConfigAction = {
                it.compilerArguments[NOPACK] = false
            }) {
                assertIsPackedKlib(module.moduleName)
            }
        }
    }

    @DisplayName("Compilation of an empty source list is reported as a compiler error")
    @BtaV2StrategyAgnosticCompilationTest
    fun emptySourceListIsReportedAsError(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            module("empty").compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*Specify at least one source file or directory.*"))
            }
        }
    }

    @DisplayName("JS sources in named packages produce fragments in the matching package directories")
    @BtaV2StrategyAgnosticCompilationTest
    fun namedPackageSourcesProduceFragmentsInPackageDirectory(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            module("basic-multimodule-project/module-3").compile {
                assertIsUnpackedJsKlib()
                assertKnmFileCount(packageFqName = "p", expectedCount = 1)
                assertKnmFileCount(packageFqName = "p2", expectedCount = 1)
                assertKnmFileCount(packageFqName = "p3", expectedCount = 1)
                assertKnmFileCount(packageFqName = "root_package", expectedCount = 0)
            }
        }
    }

    @DisplayName("Fragments of a multi-level package are stored in a directory named after the whole package")
    @BtaV2StrategyAgnosticCompilationTest
    fun multiLevelPackageSourcesProduceFragmentsInOnePackageDirectory(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            module("ic-scenarios/KT-85074").compile {
                assertIsUnpackedJsKlib()
                assertKnmFileCount(packageFqName = "org.example.foo", expectedCount = 2)
                assertKnmFileCount(packageFqName = "root_package", expectedCount = 0)
            }
        }
    }

    @DisplayName("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY makes the generated copy() private as the primary constructor")
    @BtaV2StrategyAgnosticCompilationTest
    fun dataClassCopyVisibilityIsConsistentWhenRequested(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            module("js-data-class-copy").compile(compilationConfigAction = {
                it.compilerArguments[X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY] = true
            }) {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    ".*[Cc]annot access.*copy.*private.*".toRegex(RegexOption.DOT_MATCHES_ALL),
                )
            }
        }
    }

    @DisplayName("IR_OUTPUT_NAME is written to the klib manifest as unique_name value")
    @BtaV2StrategyAgnosticCompilationTest
    fun irOutputNameIsWrittenToManifest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val outputName = "my-js-module"
        jsProject(strategyConfig) {
            module("js-ic-basic-lib").compile(compilationConfigAction = {
                it.compilerArguments[IR_OUTPUT_NAME] = outputName
            }) {
                assertKlibManifestProperties("unique_name" to outputName)
            }
        }
    }

    @DisplayName("IR_OUTPUT_NAME defines the file name of the packed klib")
    @BtaV2StrategyAgnosticCompilationTest
    fun irOutputNameDefinesThePackedKlibFileName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val outputName = "my-js-module"
        jsProject(strategyConfig) {
            module("js-ic-basic-lib").compile(compilationConfigAction = {
                it.compilerArguments[IR_OUTPUT_NAME] = outputName
                it.compilerArguments[NOPACK] = false
            }) {
                assertIsPackedKlib(outputName)
            }
        }
    }

    @DisplayName("X_IR_MODULE_NAME overrides the unique_name value written to the klib manifest")
    @BtaV2StrategyAgnosticCompilationTest
    fun irModuleNameOverridesTheModuleNameInManifest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val moduleName = "js-lib"
        jsProject(strategyConfig) {
            module("js-ic-basic-lib").compile(compilationConfigAction = {
                it.compilerArguments[X_IR_MODULE_NAME] = moduleName
            }) {
                assertIsUnpackedJsKlib()
                assertKlibManifestProperties("unique_name" to moduleName)
            }
        }
    }

    @DisplayName("X_IR_MODULE_NAME does not affect the file name of the packed klib")
    @BtaV2StrategyAgnosticCompilationTest
    fun irModuleNameDoesNotAffectThePackedKlibFileName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val outputName = "lib"
        val moduleName = "js-lib"
        jsProject(strategyConfig) {
            module("js-ic-basic-lib").compile(compilationConfigAction = {
                it.compilerArguments[IR_OUTPUT_NAME] = outputName
                it.compilerArguments[X_IR_MODULE_NAME] = moduleName
                it.compilerArguments[NOPACK] = false
            }) {
                assertIsPackedKlib(outputName)
            }
        }
    }

    @DisplayName("X_IR_PER_MODULE_OUTPUT_NAME is written to the klib manifest as the JS output name")
    @BtaV2StrategyAgnosticCompilationTest
    fun perModuleOutputNameIsWrittenToManifest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val perModuleOutputName = "my-js-output"
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.compile {
                assertKlibManifestHasNoProperties("jsOutputName")
            }
            module.compile(compilationConfigAction = {
                it.compilerArguments[X_IR_PER_MODULE_OUTPUT_NAME] = perModuleOutputName
            }) {
                assertKlibManifestProperties("jsOutputName" to perModuleOutputName)
            }
        }
    }

    @DisplayName("X_FRIEND_MODULES makes internal declarations of a dependency visible")
    @BtaV2StrategyAgnosticCompilationTest
    fun internalDeclarationsOfAFriendModuleAreVisible(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val libModule = module("friend-modules/lib")
            val appModule = module("friend-modules/app", listOf(libModule))
            libModule.compile()
            appModule.compile(compilationConfigAction = {
                it.compilerArguments[X_FRIEND_MODULES] = listOf(libModule.outputDirectory)
            }) {
                assertIsUnpackedJsKlib()
            }
        }
    }

    @DisplayName("Internal declarations of a dependency are not visible without X_FRIEND_MODULES")
    @BtaV2StrategyAgnosticCompilationTest
    fun internalDeclarationsOfANonFriendModuleAreNotVisible(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val libModule = module("friend-modules/lib")
            val appModule = module("friend-modules/app", listOf(libModule))
            libModule.compile()
            appModule.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*[Cc]annot access.*internalGreeting.*"))
            }
        }
    }

    @DisplayName("A lib+app graph compiles the app klib against the library klib across the module graph")
    @BtaV2StrategyAgnosticCompilationTest
    fun multiModuleGraphCompilesTheAppAgainstTheLibraryKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val library = module("js-ic-basic-lib")
            val app = module("js-ic-basic-app", dependencies = listOf(library))

            library.compile {
                assertIsUnpackedJsKlib()
            }

            app.compile {
                assertIsUnpackedJsKlib()
            }
        }
    }
}
