/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpPositioningStrategiesRegistrationTest {
    private val standardStrategies: List<PositioningStrategyKind> =
        PositioningStrategyKinds::class.java.declaredClasses
            .filter { PositioningStrategyKind::class.java.isAssignableFrom(it) }
            .map { it.getField("INSTANCE").get(null) as PositioningStrategyKind }

    @Test
    fun standardStrategiesAreCollected() {
        assertTrue(PositioningStrategyKinds.DEFAULT in standardStrategies) {
            "Reflection didn't find the standard strategies"
        }
    }

    @Test
    fun allStandardStrategiesAreRegisteredForKmp() {
        val notRegistered = standardStrategies.filter { it !in KmpPositioningStrategies.kindToStrategy }.map { it::class.simpleName }
        assertEquals(emptyList<String>(), notRegistered, "Strategies without a registered implementation")
    }
}
