/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Function overload changes in incremental compilation")
class FunctionOverloadChangesTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89565: Removing a function overload should recompile its usages")
    @TestMetadata("ic-scenarios/renameFileWithFunctionOverloadAndCreateConflict")
    fun testRemovingFunctionOverloadWithConflictingNewOverload(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/renameFileWithFunctionOverloadAndCreateConflict")

            mod.deleteFile("foo/foo2.kt")
            mod.createPredefinedFile("foo/foo3.kt", "new-file")

            mod.compile {
                // TODO(KT-89565): `useF.kt` is not recompiled, so only one of the three errors is reported.
                //  Once fixed, it has to be `assertCompiledSources("foo/foo3.kt", "use/useF.kt")`.
                expectFailWithError(
                    ".*Conflicting overloads:.*fun f\\(a: Any\\): Unit.*".toRegex(RegexOption.DOT_MATCHES_ALL)
                )
                assertCompiledSources("foo/foo3.kt")
            }
        }
    }
}
