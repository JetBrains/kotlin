/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Star-import conflicts")
class StarImportConflictTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-85241: Adding a Java nested class that conflicts with a Kotlin star import should trigger an ambiguity error in IC")
    @TestMetadata("ic-scenarios/kt-85241")
    fun testStarImportAmbiguityDetectedAfterAddingJavaNestedClass(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-85241")

            mod.replaceFileWithVersion("com/example/SomeClass.java", "add-nested-class")
            mod.compile {
                expectFail()
                assertCompiledSources("foo.kt")
            }
        }
    }
}
