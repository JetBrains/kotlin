/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.tools.analysisApiKdocCoverageTest

import org.jetbrains.kotlin.util.KDocCoverageTest

class AnalysisApiKDocCoverageTest() : KDocCoverageTest() {
    override val sourceDirectories: List<SourceDirectoryWithOutput> = listOf(
        SourceDirectoryWithOutput(
            "/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api",
            "/analysis/analysis-api/api/analysis-api.undocumented"
        ),
        SourceDirectoryWithOutput(
            "/analysis/analysis-api-platform-interface/src/org/jetbrains/kotlin/analysis/api/platform",
            "/analysis/analysis-api/api/analysis-api-platform-interface.undocumented"
        ),
    )

    fun testKDocCoverage() {
        doTest()
    }
}