/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.junit.jupiter.api.Test
import org.jetbrains.kotlin.config.ExplicitApiMode as CompilerExplicitApiMode
import kotlin.test.assertTrue

class ExplicitApiConsistency {

    @Test
    fun allCompilerTypeValuesArePresent() {
        CompilerExplicitApiMode.entries.forEach { entry ->
            assertTrue(
                actual = ExplicitApiMode.entries.any { it.modeName == entry.state },
                message = "Missing entry $entry in DSL types"
            )
        }
    }
}