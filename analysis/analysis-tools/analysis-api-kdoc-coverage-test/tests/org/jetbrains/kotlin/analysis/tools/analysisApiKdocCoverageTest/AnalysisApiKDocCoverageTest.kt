/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.tools.analysisApiKdocCoverageTest

import org.jetbrains.kotlin.util.KDocCoverageTest

class AnalysisApiKDocCoverageTest() : KDocCoverageTest() {
    override val sourceCodePath: String = "/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api"
    override val generatedFilePath: String =
        "/analysis/analysis-tools/analysis-api-kdoc-coverage-test/undocumented/analysis-api.undocumented"

    fun testKDocCoverage() {
        doTest()
    }
}