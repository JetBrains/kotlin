/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompletion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertExactOutput
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertFailure
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class InlinedLambdaChangeTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Assert status-quo: changes of inlined lambda's body does not trigger compilation of its users")
    @TestMetadata("ic-scenarios/jvm-inline-lib")
    fun testStatusQuo(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/jvm-inline-lib")
            val app = module("ic-scenarios/jvm-inline-app", dependencies = listOf(lib))

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedLambda.kt", "changeLambdaBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedLambda.kt")
            }
            app.compile { module, scenarioModule ->
                //assertCompiledSources(module, "callSite.kt")
                assertCompiledSources(module, emptySet())
            }
            app.executeCompiledCode("CallSiteKt") {
                // this is why KT-62555 is a bug - the call site now has inlined the old version of the code,
                // and incremental build did not pick it up
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedLambda.kt", "changeFunctionBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedLambda.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(WITH_BOTH_CHANGES)
            }

            /**
             * Bonus round: now we go back to the second step. Reverse change of lambda body forces the recompilation
             * of call site. And now the updated lambda code would be inlined
             */
            lib.replaceFileWithVersion("inlinedLambda.kt", "changeLambdaBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedLambda.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(WITH_NEW_LAMBDA_BODY)
            }
        }
    }

    private companion object {
        const val INITIAL_OUTPUT = "42"
        const val WITH_NEW_LAMBDA_BODY = "45"
        const val WITH_BOTH_CHANGES = "48"
    }
}
