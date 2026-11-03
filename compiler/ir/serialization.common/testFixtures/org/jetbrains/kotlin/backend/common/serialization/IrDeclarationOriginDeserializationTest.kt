/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * KLIBs store declaration origins by name, so [IrDeclarationDeserializer] keeps resolving renamed origins under
 * their legacy names via [IrDeclarationDeserializer.legacyDeclarationOriginNames]. A legacy name must never be
 * reused for a current origin, otherwise the alias would silently shadow it (KT-90675).
 */
abstract class IrDeclarationOriginDeserializationTest {
    @Test
    fun testLegacyOriginNamesDoNotCollideWithCurrentOnes() {
        val collisions = IrDeclarationDeserializer.legacyDeclarationOriginNames.keys
            .filter { it in IrDeclarationDeserializer.currentDeclarationOriginNames }
        assertTrue(collisions.isEmpty()) {
            "Legacy origin names must not collide with current ones: $collisions"
        }
    }

    @Test
    fun testLegacyOriginNamesResolveToCurrentOrigins() {
        for ([legacyName, origin] in IrDeclarationDeserializer.legacyDeclarationOriginNames) {
            assertTrue(origin.name in IrDeclarationDeserializer.currentDeclarationOriginNames) {
                "Legacy name $legacyName maps to $origin, which is not a current origin"
            }
        }
    }
}
