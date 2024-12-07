/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertNoOutputSetChanges
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertRemovedOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class SourceChangesTrackingTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Intra-module IC tracks source changes in consecutive builds")
    @TestMetadata("jvm-module-1")
    fun testConsequentBuilds(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = trackedModule("jvm-module-1")
            module1.createPredefinedFile("secret.kt", "new-file")
            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "secret.kt")
                assertAddedOutputs(module, scenarioModule, "SecretKt.class")
            }

            // replaces bar.kt with bar.kt.1
            module1.replaceFileWithVersion("bar.kt", "add-default-argument")
            module1.deleteFile("secret.kt")

            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "bar.kt")
                assertRemovedOutputs(module, scenarioModule, "SecretKt.class")
            }
        }
    }
}