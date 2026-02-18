/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.AbstractSurfaceDumpConsistencyTest
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class PsiSurfaceDumpConsistencyTest : AbstractSurfaceDumpConsistencyTest() {
    private companion object {
        private val API_SURFACE_PATH = Paths.get("api/psi-api.api")
    }

    @Test
    fun testNestedClassCoverage() = validateApiDump(API_SURFACE_PATH)
}
