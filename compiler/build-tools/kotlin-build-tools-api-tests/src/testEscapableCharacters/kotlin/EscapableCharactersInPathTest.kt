/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class EscapableCharactersInPathTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Classpath contains whitespaces or other escapable characters")
    @TestMetadata("jvm-module-1")
    fun testInClasspath(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1")

            module1.createPredefinedFile("secret.kt", "new-file")

            module1.compile {
                assertCompiledSources("secret.kt")
                assertAddedOutputs("SecretKt.class")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Sources path contains whitespaces or other escapable characters")
    @TestMetadata("jvm module with \$trange c#aracters")
    fun testInModulePath(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm module with \$trange c#aracters")

            module1.replaceFileWithVersion("a.kt", "change-return-type")

            module1.compile {
                expectFailWithError(".*/b.kt:6:18 Return type mismatch: expected 'kotlin.Int', actual 'kotlin.String'.".toRegex())
            }
        }
    }
}