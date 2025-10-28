/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import org.jetbrains.kotlin.AbstractSurfaceDumpConsistencyTest
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class AnalysisApiSurfaceDumpConsistencyTest : AbstractSurfaceDumpConsistencyTest() {
    private companion object {
        private val API_SURFACE_PATHS = listOf(
            Paths.get("analysis/analysis-api/api/analysis-api.api"),
            Paths.get("analysis/analysis-api-standalone/api-unstable/analysis-api-standalone.api"),
            Paths.get("analysis/analysis-api-platform-interface/api-unstable/analysis-api-platform-interface.api")
        )
    }

    @Test
    fun testNestedClassCoverage() = API_SURFACE_PATHS.forEach { path ->
        validateApiDump(path)
    }
}
