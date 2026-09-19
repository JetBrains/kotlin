/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.jsProject
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalCompilerArgument::class)
@DisplayName("Functional tests for the JS klib compilation operation of the BTA")
class JsKlibCompilationTest : BaseCompilationTest() {

    @DisplayName("Compiling Kotlin sources produces an unpacked klib with IR and metadata fragments")
    @BtaV2StrategyAgnosticCompilationTest
    fun compilesToUnpackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            module("js-ic-basic-lib").compile {
                assertIsUnpackedKlibWithIr()
                assertKnmFileCount(expectedCount = 3)
            }
        }
    }

    @DisplayName("Disabled NOPACK produces a packed klib file")
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

    @DisplayName("Kotlin sources in named packages produce fragments in the matching package directories")
    @BtaV2StrategyAgnosticCompilationTest
    fun namedPackageSourcesProduceFragmentsInPackageDirectory(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            module("basic-multimodule-project/module-3").compile {
                assertIsUnpackedKlibWithIr()
                assertKnmFileCount(packageFqName = "p", expectedCount = 1)
                assertKnmFileCount(packageFqName = "p2", expectedCount = 1)
                assertKnmFileCount(packageFqName = "p3", expectedCount = 1)
                assertKnmFileCount(packageFqName = "root_package", expectedCount = 0)
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
                assertIsUnpackedKlibWithIr()
            }

            app.compile {
                assertIsUnpackedKlibWithIr()
            }
        }
    }
}
