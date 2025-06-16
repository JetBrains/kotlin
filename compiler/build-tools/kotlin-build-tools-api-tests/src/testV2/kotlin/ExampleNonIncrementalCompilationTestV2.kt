/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.buildtools.api.tests.v2.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.model.project
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.junit.jupiter.api.DisplayName

class ExampleNonIncrementalCompilationTestV2 : BaseCompilationTest() {
    @DisplayName("Sample non-incremental compilation test with two modules")
    @DefaultStrategyAgnosticCompilationTest
    fun myTest(executionPolicy: ExecutionPolicy) = runTest {
        project(executionPolicy) {
            val module1 = module("jvm-module-1")
            module1.compile { module ->
                assertOutputs(module1, setOf("FooKt.class", "Bar.class", "BazKt.class"))
            }
        }
    }
}
