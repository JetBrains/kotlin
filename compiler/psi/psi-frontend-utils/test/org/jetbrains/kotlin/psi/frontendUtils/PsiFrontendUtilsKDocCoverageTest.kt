/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.frontendUtils

import org.jetbrains.kotlin.AbstractPsiKDocCoverageTest
import org.junit.jupiter.api.Test

class PsiFrontendUtilsKDocCoverageTest : AbstractPsiKDocCoverageTest() {
    override val sourceDirectories: List<SourceDirectory.ForDumpFileComparison> = listOf(
        SourceDirectory.ForDumpFileComparison(
            listOf("src/org/jetbrains/kotlin"),
            "api/psi-frontend-utils-api.undocumented",
        )
    )

    @Test
    fun testKDocCoverage() {
        doTest()
    }
}