/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import org.jetbrains.kotlin.konan.target.KonanTarget as CompilerKonanTarget

class KonanTargetConsistency {
    @Test
    fun supportedTargetsAreConsistent() {
        assertEquals(
            expected = CompilerKonanTarget.predefinedTargets.keys.sorted(),
            actual = KonanTarget.entries.filter { it.releaseVersionsMetadata.removedVersion == null }.map { it.targetName }.sorted(),
            message = "Targets supported by the compiler differ from the non-removed entries of the 'KonanTarget' DSL type. " +
                    "Please add the missing targets or set 'removedVersion' for the removed ones.",
        )
    }
}
