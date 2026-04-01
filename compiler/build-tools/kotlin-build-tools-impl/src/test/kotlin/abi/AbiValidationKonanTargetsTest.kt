/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi

import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetType
import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class AbiValidationKonanTargetsTest {
    @Test
    fun test() {
        val unknowKonanTargets = KonanTarget.predefinedTargets.keys.mapNotNull {
            try {
                KlibTargetType.fromKonanTargetName(it)
                null
            } catch (_: Exception) {
                it
            }
        }
        assertEquals(emptyList(), unknowKonanTargets, "There are unrecognized in ABI Validation Konan targets: $unknowKonanTargets")
    }
}