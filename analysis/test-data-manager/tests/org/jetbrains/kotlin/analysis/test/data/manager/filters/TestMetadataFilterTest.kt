/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import org.jetbrains.kotlin.analysis.test.data.manager.fakes.analysis.FakeGoldenAnalysisApiTestGenerated
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestDescriptor

internal class TestMetadataFilterTest : AbstractPostDiscoveryFilterTest() {
    @Test
    fun `ClassSource matching path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `ClassSource not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `MethodSource matching combined path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/symbols")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource with partial path match is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `MethodSource not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated::testSymbols),
        )
    }

    @Test
    fun `container type is always included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("nonexistent/path")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(type = TestDescriptor.Type.CONTAINER),
        )
    }

    @Test
    fun `descriptor without source is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorWithSource(source = null),
        )
    }

    @Test
    fun `multiple includePaths - any match results in included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("nonexistent/path", "testData/analysis", "another/path")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `multiple includePaths - none match results in excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("nonexistent/path", "another/path", "third/path")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated>(),
        )
    }

    @Test
    fun `class without TestMetadata annotation is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            descriptor = descriptorFromClass<NoMetadataClass>(),
        )
    }

    @Test
    fun `Nested class matching path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/expressions")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated.Expressions>(),
        )
    }

    @Test
    fun `Nested class not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromClass<FakeGoldenAnalysisApiTestGenerated.Expressions>(),
        )
    }

    @Test
    fun `Method in nested class with combined path is included`() {
        assertIncluded(
            filter = TestMetadataFilter(listOf("testData/analysis/api/expressions/call")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated.Expressions::testCall),
        )
    }

    @Test
    fun `Method in nested class not matching path is excluded`() {
        assertExcluded(
            filter = TestMetadataFilter(listOf("testData/other")),
            descriptor = descriptorFromMethod(FakeGoldenAnalysisApiTestGenerated.Expressions::testCall),
        )
    }
}
