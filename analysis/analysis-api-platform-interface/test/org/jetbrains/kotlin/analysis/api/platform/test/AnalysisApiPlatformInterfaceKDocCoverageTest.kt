package org.jetbrains.kotlin.analysis.api.platform.test

import org.jetbrains.kotlin.AbstractKDocCoverageTest
import org.junit.jupiter.api.Test

class AnalysisApiPlatformInterfaceKDocCoverageTest : AbstractKDocCoverageTest() {
    override val sourceDirectories: List<SourceDirectory.ForDumpFileComparison> = listOf(
        SourceDirectory.ForDumpFileComparison(
            listOf("src/org/jetbrains/kotlin/analysis/api/platform"),
            "api/analysis-api-platform-interface.undocumented",
        ),
    )

    @Test
    fun testKDocCoverage() {
        doTest()
    }
}
