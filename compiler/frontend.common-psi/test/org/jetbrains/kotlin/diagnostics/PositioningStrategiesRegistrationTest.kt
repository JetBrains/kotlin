/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Checks that every standard [SourceElementPositioningStrategy] declared in [SourceElementPositioningStrategies]
 * has an implementation for each parse tree kind.
 */
class PositioningStrategiesRegistrationTest {
    private val standardStrategies: List<SourceElementPositioningStrategy> =
        SourceElementPositioningStrategies::class.java.declaredClasses
            .filter { SourceElementPositioningStrategy::class.java.isAssignableFrom(it) }
            .map { it.getField("INSTANCE").get(null) as SourceElementPositioningStrategy }

    @Test
    fun standardStrategiesAreCollected() {
        assertTrue(SourceElementPositioningStrategies.DEFAULT in standardStrategies) {
            "Reflection didn't find the standard strategies"
        }
    }

    @Test
    fun allStandardStrategiesAreRegisteredForLightTree() {
        checkRegistered(LightTreePositioningStrategies.keyToStrategy)
    }

    @Test
    fun allStandardStrategiesAreRegisteredForPsi() {
        checkRegistered(PsiPositioningStrategies.keyToStrategy)
    }

    private fun checkRegistered(keyToStrategy: Map<SourceElementPositioningStrategy, PositioningStrategy>) {
        val notRegistered = standardStrategies.filter { it !in keyToStrategy }.map { it::class.simpleName }
        assertEquals(emptyList<String>(), notRegistered, "Strategies without a registered implementation")
    }
}
