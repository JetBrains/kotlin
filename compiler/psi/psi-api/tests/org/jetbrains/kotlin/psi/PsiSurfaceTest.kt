/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.AbstractSurfaceTest
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class PsiSurfaceTest : AbstractSurfaceTest() {
    private companion object {
        private val API_SURFACE_PATH = Paths.get("compiler/psi/psi-api/api/psi-api.api")
    }

    @Test
    fun testNestedClassCoverage() = validateApiDump(API_SURFACE_PATH)
}
