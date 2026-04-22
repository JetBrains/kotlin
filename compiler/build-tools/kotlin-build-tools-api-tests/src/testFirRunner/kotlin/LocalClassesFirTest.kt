/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.compile
import org.jetbrains.kotlin.buildtools.tests.compilation.util.moduleWithFir
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class LocalClassesFirTest : BaseCompilationTest() {
    @DisplayName("Changing super type of local class should trigger rebuild of the local class")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("ic-scenarios/KT-85074")
    fun localClassSuperTypeChange(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = moduleWithFir("ic-scenarios/KT-85074")

            module.replaceFileWithVersion("Foo.kt", "addVal")

            module.compile {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    ".*Class '<anonymous>' is not abstract and does not implement abstract member:\nval bar: String".toRegex()
                )
            }
        }
    }

    @DisplayName("Changing local class should not trigger rebuild of usages")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("ic-scenarios/local-class-change")
    fun localClassChange(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = moduleWithFir("ic-scenarios/local-class-change")

            module.replaceFileWithVersion("Foo.kt", "changeLocalClass")

            module.compile(expectedDirtySet = setOf("Foo.kt"))
        }
    }
}
