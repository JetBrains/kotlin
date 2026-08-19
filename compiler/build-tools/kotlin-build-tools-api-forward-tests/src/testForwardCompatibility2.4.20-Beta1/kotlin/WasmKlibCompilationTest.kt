/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_IR_MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerKlibArguments.Companion.X_IR_PER_MODULE_OUTPUT_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments.Companion.X_WASM_KCLASS_FQN
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments.Companion.X_WASM_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.wasmProject
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.currentKotlinWasmWasiStdlibKlibLocation
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalCompilerArgument::class)
@DisplayName("Functional tests for the Wasm klib compilation operation of the BTA")
class WasmKlibCompilationTest : BaseCompilationTest() {

    @DisplayName("Compiling Kotlin sources produces an unpacked klib with IR and metadata fragments")
    @BtaV2StrategyAgnosticCompilationTest
    fun compilesToUnpackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            module("js-ic-basic-lib").compile {
                assertIsUnpackedKlibWithIr()
                assertKnmFileCount(expectedCount = 3)
                assertKlibManifestProperties("builtins_platform" to "WASM", "wasm_targets" to "wasm-js")
            }
        }
    }

    @DisplayName("Disabled NOPACK produces a packed klib file")
    @BtaV2StrategyAgnosticCompilationTest
    fun packedKlibIsProducedWhenNopackIsDisabled(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
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
        wasmProject(strategyConfig) {
            module("empty").compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*No source files.*"))
            }
        }
    }

    @DisplayName("Kotlin sources in named packages produce fragments in the matching package directories")
    @BtaV2StrategyAgnosticCompilationTest
    fun namedPackageSourcesProduceFragmentsInPackageDirectory(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            module("basic-multimodule-project/module-3").compile {
                assertIsUnpackedKlibWithIr()
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
        wasmProject(strategyConfig) {
            module("ic-scenarios/KT-85074").compile {
                assertIsUnpackedKlibWithIr()
                assertKnmFileCount(packageFqName = "org.example.foo", expectedCount = 2)
                assertKnmFileCount(packageFqName = "root_package", expectedCount = 0)
            }
        }
    }

    @DisplayName("IR_OUTPUT_NAME is written to the klib manifest as unique_name value")
    @BtaV2StrategyAgnosticCompilationTest
    fun irOutputNameIsWrittenToManifest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val outputName = "my-lib"
        wasmProject(strategyConfig) {
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
        val outputName = "my-lib"
        wasmProject(strategyConfig) {
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
        val moduleName = "renamed-lib"
        wasmProject(strategyConfig) {
            module("js-ic-basic-lib").compile(compilationConfigAction = {
                it.compilerArguments[X_IR_MODULE_NAME] = moduleName
            }) {
                assertIsUnpackedKlibWithIr()
                assertKlibManifestProperties("unique_name" to moduleName)
            }
        }
    }

    @DisplayName("X_IR_MODULE_NAME does not affect the file name of the packed klib")
    @BtaV2StrategyAgnosticCompilationTest
    fun irModuleNameDoesNotAffectThePackedKlibFileName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val outputName = "my-lib"
        val moduleName = "renamed-lib"
        wasmProject(strategyConfig) {
            module("js-ic-basic-lib").compile(compilationConfigAction = {
                it.compilerArguments[IR_OUTPUT_NAME] = outputName
                it.compilerArguments[X_IR_MODULE_NAME] = moduleName
                it.compilerArguments[NOPACK] = false
            }) {
                assertIsPackedKlib(outputName)
            }
        }
    }

    @DisplayName("X_IR_PER_MODULE_OUTPUT_NAME is written to the klib manifest as the output name")
    @BtaV2StrategyAgnosticCompilationTest
    fun perModuleOutputNameIsWrittenToManifest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val perModuleOutputName = "lib-output"
        wasmProject(strategyConfig) {
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

    @DisplayName("OPT_IN allows using an API that requires opt-in and compiles to an unpacked klib")
    @BtaV2StrategyAgnosticCompilationTest
    fun optInAllowsUsingAnApiThatRequiresOptIn(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            module("opt-in-usage").compile(compilationConfigAction = {
                it.compilerArguments[OPT_IN] = listOf("MyExperimentalApi")
            }) {
                assertIsUnpackedKlibWithIr()
            }
        }
    }

    @DisplayName("Using an API that requires opt-in without OPT_IN is reported as a compiler error")
    @BtaV2StrategyAgnosticCompilationTest
    fun optInApiUsageWithoutOptInIsReportedAsError(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            module("opt-in-usage").compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*This API is experimental and requires opt-in.*"))
            }
        }
    }

    @DisplayName("X_WASM_TARGET set to wasm-js compiles the sources to an unpacked klib targeting wasm-js")
    @BtaV2StrategyAgnosticCompilationTest
    fun wasmJsTargetCompilesToKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            module("js-ic-basic-lib").compile(compilationConfigAction = {
                it.compilerArguments[X_WASM_TARGET] = WasmTarget.WASM_JS
            }) {
                assertIsUnpackedKlibWithIr()
                assertKlibManifestProperties("builtins_platform" to "WASM", "wasm_targets" to "wasm-js")
            }
        }
    }

    @DisplayName("X_WASM_TARGET set to wasm-wasi compiles the sources to an unpacked klib targeting wasm-wasi")
    @BtaV2StrategyAgnosticCompilationTest
    fun wasmWasiTargetCompilesToKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            // the wasm-wasi target requires the wasm-wasi stdlib klib rather than the default wasm-js one
            module("js-ic-basic-lib", stdlibClasspath = listOf(currentKotlinWasmWasiStdlibKlibLocation)).compile(
                compilationConfigAction = {
                    it.compilerArguments[X_WASM_TARGET] = WasmTarget.WASM_WASI
                }
            ) {
                assertIsUnpackedKlibWithIr()
                assertKlibManifestProperties("builtins_platform" to "WASM", "wasm_targets" to "wasm-wasi")
            }
        }
    }

    @DisplayName("X_WASM_KCLASS_FQN enabled allows KClass.qualifiedName and compiles to an unpacked klib")
    @BtaV2StrategyAgnosticCompilationTest
    fun wasmKClassFqnEnabledAllowsQualifiedName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            module("wasm-kclass-fqn").compile(compilationConfigAction = {
                it.compilerArguments[X_WASM_KCLASS_FQN] = true
            }) {
                assertIsUnpackedKlibWithIr()
            }
        }
    }

    @DisplayName("X_WASM_KCLASS_FQN disabled rejects KClass.qualifiedName as an unsupported reflection API")
    @BtaV2StrategyAgnosticCompilationTest
    fun wasmKClassFqnDisabledRejectsQualifiedName(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            module("wasm-kclass-fqn").compile(compilationConfigAction = {
                it.compilerArguments[X_WASM_KCLASS_FQN] = false
            }) {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*This reflection API is not supported in Kotlin/Wasm.*"))
            }
        }
    }

    @DisplayName("X_FRIEND_MODULES makes internal declarations of a dependency visible")
    @BtaV2StrategyAgnosticCompilationTest
    fun internalDeclarationsOfAFriendModuleAreVisible(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val libModule = module("friend-modules/lib")
            val appModule = module("friend-modules/app", listOf(libModule))
            libModule.compile()
            appModule.compile(compilationConfigAction = {
                it.compilerArguments[X_FRIEND_MODULES] = listOf(libModule.outputDirectory)
            }) {
                assertIsUnpackedKlibWithIr()
            }
        }
    }

    @DisplayName("Internal declarations of a dependency are not visible without X_FRIEND_MODULES")
    @BtaV2StrategyAgnosticCompilationTest
    fun internalDeclarationsOfANonFriendModuleAreNotVisible(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
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
        wasmProject(strategyConfig) {
            val library = module("js-ic-basic-lib")
            val app = module("js-ic-basic-app", dependencies = listOf(library))

            library.compile {
                assertIsUnpackedKlibWithIr()
            }

            app.compile {
                assertIsUnpackedKlibWithIr()
            }
        }
    }
}
