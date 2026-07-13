/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import org.jetbrains.kotlin.AbstractKDocCoverageTest
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test

class AnalysisApiKDocCoverageTest : AbstractKDocCoverageTest() {
    override val sourceDirectories: List<SourceDirectory.ForDumpFileComparison> = listOf(
        SourceDirectory.ForDumpFileComparison(
            listOf("src/org/jetbrains/kotlin/analysis/api"),
            "api/analysis-api.undocumented",
        ),
    )

    override val ignoredPackages: List<FqName>
        get() = listOf(
            FqName("org.jetbrains.kotlin.analysis.api.internals"),
        )

    @Test
    fun testKDocCoverage() {
        doTest()
    }
}
