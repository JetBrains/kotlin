/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import org.jetbrains.kotlin.AbstractKDocCoverageTest
import org.junit.jupiter.api.Test

class AnalysisApiKDocCoverageTest : AbstractKDocCoverageTest() {
    override val sourceDirectories: List<DocumentationLocations> = listOf(
        DocumentationLocations(
            listOf("analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api"),
            "analysis/analysis-api/api/analysis-api.undocumented",
        ),
        DocumentationLocations(
            listOf("analysis/analysis-api-platform-interface/src/org/jetbrains/kotlin/analysis/api/platform"),
            "analysis/analysis-api/api/analysis-api-platform-interface.undocumented",
        ),
    )

    @Test
    fun testKDocCoverage() {
        doTest()
    }
}
