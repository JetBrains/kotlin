/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.jetbrains.kotlin.arguments.description.actualCommonCompilerArguments
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.config.ReturnValueCheckerMode as CompilerReturnValueCheckerMode

class ReturnValueCheckerModeConsistency {

    @Test
    fun allCompilerTypeValuesArePresent() {
        CompilerReturnValueCheckerMode.entries.forEach { entry ->
            assertTrue(
                actual = ReturnValueCheckerMode.entries.any { it.modeState == entry.state },
                message = "Missing entry $entry in DSL types"
            )
        }
    }

    @Test
    fun explicitApiArgumentValueDescription() {
        val returnValueCheckerArg = actualCommonCompilerArguments.arguments.single { it.name == "Xreturn-value-checker" }

        assertEquals(
            expected = 10644755,
            actual = returnValueCheckerArg.valueDescription.hashCode(),
            message = "Value description in '${returnValueCheckerArg.name}' should be updated by moving current value to 'oldValues'."
        )
    }
}