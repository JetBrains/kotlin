/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.MetadataProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.supportsMetadata
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText
import org.jetbrains.kotlin.buildtools.tests.compilation.model.metadataProject as metadataProjectForStrategy

@DisplayName("Functional tests for the metadata (common/KMP) compilation operation of the BTA")
class MetadataCompilationTest : BaseCompilationTest() {

    @DisplayName("Compiling common sources produces an unpacked metadata klib")
    @BtaV2StrategyAgnosticCompilationTest
    fun compilesToUnpackedKlib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        metadataProject(strategyConfig) {
            val module = moduleWithCommonSource()
            module.compile {
                assertIsUnpackedMetadataKlib()
                assertKnmFileCount(packageFqName = "", expectedCount = 1)
            }
        }
    }

    @DisplayName("Common sources in a named package produce fragments in the matching package directory")
    @BtaV2StrategyAgnosticCompilationTest
    fun namedPackageSourcesProduceFragmentsInPackageDirectory(strategyConfig: CompilerExecutionStrategyConfiguration) {
        metadataProject(strategyConfig) {
            val module = moduleWithCommonSource(packageName = "common")
            module.compile {
                assertIsUnpackedMetadataKlib()
                assertKnmFileCount(packageFqName = "common", expectedCount = 1)
            }
        }
    }

    @DisplayName("MODULE_NAME is reflected in the metadata klib manifest")
    @BtaV2StrategyAgnosticCompilationTest
    fun moduleNameIsWrittenToManifest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val moduleName = "my-module"
        metadataProject(strategyConfig) {
            val module = moduleWithCommonSource()
            module.compile(compilationConfigAction = {
                it.compilerArguments[MetadataArguments.MODULE_NAME] = moduleName
            }) {
                assertOutputFileContains("default/manifest", moduleName)
            }
        }
    }

    @DisplayName("CLASSPATH dependency is passed to the metadata compiler")
    @BtaV2StrategyAgnosticCompilationTest
    fun classpathDependencyIsPassedToCompiler(strategyConfig: CompilerExecutionStrategyConfiguration) {
        metadataProject(strategyConfig) {
            val library = module("basic-multimodule-project/module-1")
            library.compile {}
            // module-2 references `Bar` from module-1, so it only compiles when the library is on the classpath.
            val consumer = module("basic-multimodule-project/module-2", dependencies = listOf(library))
            consumer.compile {
                assertIsUnpackedMetadataKlib()
                assertKnmFileCount(packageFqName = "", expectedCount = 2)
            }
        }
    }

    @DisplayName("Metadata compilation with an empty source list does not crash")
    @BtaV2StrategyAgnosticCompilationTest
    fun emptySourceListIsHandledGracefully(strategyConfig: CompilerExecutionStrategyConfiguration) {
        metadataProject(strategyConfig) {
            val module = module("empty")
            module.compile(assertions = {
                assertIsUnpackedMetadataKlib()
                assertNoKnmFiles()
            })
        }
    }

    private fun MetadataProject.moduleWithCommonSource(packageName: String? = null) =
        module("empty").also { module ->
            val packageDeclaration = packageName?.let { "package $it\n\n" } ?: ""
            module.sourcesDirectory.resolve("common.kt").writeText(packageDeclaration + "fun greeting(): String = \"hello from common\"")
        }

    private fun metadataProject(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        action: MetadataProject.() -> Unit,
    ) {
        assumeTrue(strategyConfig.first.supportsMetadata())
        metadataProjectForStrategy(strategyConfig, action)
    }
}
