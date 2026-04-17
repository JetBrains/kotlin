/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.compile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("KT-85740: Lookup on nullable parameter type is recorded when argument is null")
class NullableParamTypeLookupTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing the class used as a nullable parameter type triggers recompilation of the call site that passes null")
    @TestMetadata("ic-scenarios/kt-85740/module-a")
    fun testNullArgumentCallSiteTracksParameterType(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val moduleC = module("ic-scenarios/kt-85740/module-c")
            val moduleB = module("ic-scenarios/kt-85740/module-b", dependencies = listOf(moduleC))
            val moduleA = module("ic-scenarios/kt-85740/module-a", dependencies = listOf(moduleB, moduleC))

            // Remove class C from module-c. This simulates removing the module that provides C
            // from module-a's dependency list: C.class will be gone from module-c's output, so
            // the incremental classpath snapshot will report C as deleted.
            moduleC.deleteFile("c.kt")

            // Recompile module-c: the IC detects the source deletion, removes C.class from the
            // output, and produces no new class files.
            moduleC.compile(expectedDirtySet = setOf())

            // Do NOT recompile module-b. B.class still exists and its method descriptor still
            // references C (as a parameter type). This mirrors the real scenario where module-b
            // still compiles fine against module-c.

            // Recompile module-a with no source changes. The IC sees that C disappeared from
            // module-c's snapshot.
            //
            // With the fix: a.kt has a lookup on C (recorded when B().send(c = null) was
            // compiled), so a.kt is included in the dirty set, recompiled, and fails because C
            // is no longer on the classpath — matching the clean-build result.
            //
            // Without the fix: no lookup on C in module-a → the dirty set is empty → the IC
            // reports success while the stale a.class silently references a missing class.
            moduleA.compile {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    ".*/a.kt:3:13 Cannot access class 'C'. Check your module classpath for missing or conflicting dependencies.".toRegex()
                )
                assertCompiledSources(setOf("a.kt"))
            }
        }
    }
}
