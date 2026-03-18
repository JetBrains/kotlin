/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.junit.jupiter.api.Test
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
}