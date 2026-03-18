/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.codebaseTest

import org.jetbrains.kotlin.AbstractSurfaceDumpConsistencyTest
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class KtReferencesSurfaceDumpConsistencyTest : AbstractSurfaceDumpConsistencyTest() {
    @Test
    fun testNestedClassCoverage() {
        validateApiDump(Paths.get("api/kt-references.api"))
    }
}
