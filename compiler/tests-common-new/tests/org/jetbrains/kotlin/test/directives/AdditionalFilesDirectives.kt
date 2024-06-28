/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object AdditionalFilesDirectives : SimpleDirectivesContainer() {
    val CHECK_TYPE by directive(
        description = """
            Adds util functions for type checking
            See file ./compiler/testData/diagnostics/helpers/types/checkType.kt
        """.trimIndent()
    )

    val CHECK_TYPE_WITH_EXACT by directive(
        description = """
            Adds util functions for type checking that use @kotlin.internal.Exact annotation
            See file ./compiler/testData/diagnostics/helpers/types/checkTypeWithExact.kt
        """.trimIndent()
    )

    val WITH_COROUTINES by directive(
        description = """
            Adds util functions for checking coroutines
            See file ./compiler/testData/diagnostics/helpers/coroutines/CoroutineHelpers.kt
        """.trimIndent()
    )

    val CHECK_STATE_MACHINE by directive(
        description = """
            Adds util functions for checking state machines
            May be enabled only with $WITH_COROUTINES directive
            See file ./compiler/testData/diagnostics/helpers/coroutines/StateMachineChecker.kt
        """.trimIndent()
    )

    val CHECK_TAIL_CALL_OPTIMIZATION by directive(
        description = """
            Adds util functions for checking tail call optimizations
            May be enabled only with $WITH_COROUTINES directive
            See file ./compiler/testData/diagnostics/helpers/coroutines/TailCallOptimizationChecker.kt
        """.trimIndent()
    )

    val SPEC_HELPERS by directive(
        description = """
            Adds util functions from `test-spec` modules
            See directory ./compiler/tests-spec/helpers/
        """.trimIndent()
    )

    val INFERENCE_HELPERS by directive(
        description = """
            Adds util functions for type checking
            See file ./compiler/testData/diagnostics/helpers/inference/inferenceUtils.kt
        """.trimIndent()
    )
}
