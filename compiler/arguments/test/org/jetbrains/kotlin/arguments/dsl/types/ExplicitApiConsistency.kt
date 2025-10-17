/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.jetbrains.kotlin.arguments.description.actualCommonCompilerArguments
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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

    @Test
    fun explicitApiArgumentValueDescription() {
        val explicitApiArg = actualCommonCompilerArguments.arguments.single { it.name == "Xexplicit-api" }

        assertEquals(
            expected = -312461075,
            actual = explicitApiArg.valueDescription.hashCode(),
            message = "Value description in '${explicitApiArg.name}' should be updated by moving current value to 'oldValues'."
        )
    }

    @Test
    fun explicitApiReturnTypesArgumentValueDescription() {
        val explicitApiReturnTypesArg = actualCommonCompilerArguments.arguments.single { it.name == "XXexplicit-return-types" }

        assertEquals(
            expected = -312461075,
            actual = explicitApiReturnTypesArg.valueDescription.hashCode(),
            message = "Value description in '${explicitApiReturnTypesArg.name}' should be updated by moving current value to 'oldValues'."
        )
    }
}