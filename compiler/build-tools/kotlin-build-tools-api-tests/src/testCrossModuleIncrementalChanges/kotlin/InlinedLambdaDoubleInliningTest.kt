/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class InlinedLambdaDoubleInliningTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Double inlining test with app->lib1->lib2 structure")
    @TestMetadata("ic-scenarios/inline-double-inlining/app")
    fun testDoubleInlining(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib2 = module("ic-scenarios/inline-double-inlining/lib2")
            val lib1 = module(
                "ic-scenarios/inline-double-inlining/lib1",
                dependencies = listOf(lib2)
            )
            val app = module(
                "ic-scenarios/inline-double-inlining/app",
                dependencies = listOf(lib1)
            )

            app.execute(mainClass = "AppKt", exactOutput = "bar2 v1")

            lib2.replaceFileWithVersion("lib2.kt", "changeLambdaBody")
            lib2.compile()
            lib1.compile()
            app.compile()
            app.execute(mainClass = "AppKt", exactOutput = "bar2 v2")
        }
    }
}
