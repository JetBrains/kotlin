/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.compilerRefIndexConfiguration
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.FILE_IDS_TO_PATHS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.LOOKUPS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.SUBTYPES_FILENAME
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.exists

@DisplayName("The compiler reference index configuration")
class CompilerRefIndexConfigurationTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("An incremental compilation writes the index to the configured directory instead of the IC working directory")
    @TestMetadata("basic-multimodule-project/module-1")
    fun indexGoesToTheConfiguredDirectory(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            val criDirectory = module.buildDirectory.resolve("configured-cri")
            module.compileIncrementally(SourcesChanges.Unknown, compilationConfigAction = { builder ->
                builder[BaseCompilationOperation.COMPILER_REF_INDEX] = builder.compilerRefIndexConfiguration(criDirectory)
            }) {
                for (fileName in listOf(LOOKUPS_FILENAME, FILE_IDS_TO_PATHS_FILENAME, SUBTYPES_FILENAME)) {
                    assertTrue(criDirectory.resolve(fileName).exists()) { "$fileName is missing from $criDirectory" }
                }
                assertFalse(module.icCachesDir.resolve(CriToolchain.DATA_PATH).exists())
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("With only the deprecated flag, an incremental compilation still writes the index to the IC working directory")
    @TestMetadata("basic-multimodule-project/module-1")
    fun deprecatedFlagWritesToTheIcWorkingDirectory(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(SourcesChanges.Unknown, compilationConfigAction = { builder ->
                @Suppress("DEPRECATION")
                builder[BaseCompilationOperation.GENERATE_COMPILER_REF_INDEX] = true
            }) {
                assertTrue(module.icCachesDir.resolve(CriToolchain.DATA_PATH).resolve(LOOKUPS_FILENAME).exists())
            }
        }
    }
}
