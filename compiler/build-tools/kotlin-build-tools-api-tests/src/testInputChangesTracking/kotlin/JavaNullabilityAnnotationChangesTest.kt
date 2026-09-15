/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Java nullability annotation changes in incremental compilation")
class JavaNullabilityAnnotationChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89329: Adding @Nullable to a Java base-class method should recompile the Kotlin usage that goes through a subclass")
    @TestMetadata("ic-scenarios/java-nullable-base-method")
    fun testAddingNullableToJavaBaseClassMethod(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/java-nullable-base-method")

            mod.replaceFileWithVersion("Base.java", "add-nullable")

            mod.compile {
                // TODO(KT-89329): the dirty set is empty, so `Usage.kt` is never rechecked and the build wrongly
                //  succeeds, unlike a clean build. Once fixed, the compilation has to `expectFail()` with
                //  "Initializer type mismatch: expected 'String', actual 'String?'" and recompile `Usage.kt`.
                assertNoCompiledSources()
            }

            mod.changeFile("Usage.kt") { "$it\n" }

            mod.compile {
                expectFailWithError(".*Usage\\.kt:\\d+:\\d+ Initializer type mismatch: expected 'String', actual 'String\\?'.*".toRegex())
            }
        }
    }
}
