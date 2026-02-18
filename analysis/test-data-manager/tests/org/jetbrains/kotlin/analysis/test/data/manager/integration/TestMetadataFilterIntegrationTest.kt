/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.integration

import org.jetbrains.kotlin.analysis.test.data.manager.fakes.analysis.FakeGoldenAnalysisApiTestGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.compiler.FakeFirTestGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.filters.TestMetadataFilter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory

internal class TestMetadataFilterIntegrationTest {
    private fun assertDiscoveredTests(
        filter: TestMetadataFilter,
        expectedIncluded: Set<String>,
        expectedExcluded: Set<String>,
    ) {
        val selectors: List<DiscoverySelector> = listOf(
            FakeGoldenAnalysisApiTestGenerated::class.java,
            FakeFirTestGenerated::class.java,
        ).map(DiscoverySelectors::selectClass)

        assertDiscoveredTests(filter, expectedIncluded, expectedExcluded, selectors)
    }

    private fun assertDiscoveredTests(
        filter: TestMetadataFilter,
        expectedIncluded: Set<String>,
        expectedExcluded: Set<String>,
        selectors: List<DiscoverySelector>,
    ) {
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectors)
            .filters(filter)
            .build()

        val testPlan = LauncherFactory.create().discover(request)

        val discoveredTestNames = testPlan.roots
            .flatMap(testPlan::getDescendants)
            .filter(TestIdentifier::isTest)
            .map(TestIdentifier::getDisplayName)
            .toSet()

        for (expected in expectedIncluded) {
            assertTrue(expected in discoveredTestNames) {
                "Expected '$expected' to be included. Discovered: $discoveredTestNames"
            }
        }

        for (excluded in expectedExcluded) {
            assertFalse(excluded in discoveredTestNames) {
                "Expected '$excluded' to be excluded but was discovered"
            }
        }
    }

    @Test
    fun `filter includes analysis tests and excludes fir tests`() {
        assertDiscoveredTests(
            filter = TestMetadataFilter(listOf("testData/analysis")),
            expectedIncluded = setOf("testSymbols()", "testTypes()", "testCall()", "testLambda()"),
            expectedExcluded = setOf("testChecker()", "testInference()"),
        )
    }

    @Test
    fun `filter includes nested class tests`() {
        assertDiscoveredTests(
            filter = TestMetadataFilter(listOf("testData/analysis/api/expressions")),
            expectedIncluded = setOf("testCall()", "testLambda()"),
            expectedExcluded = setOf("testSymbols()", "testTypes()", "testChecker()", "testInference()"),
        )
    }

    @Test
    fun `filter for specific file in nested class`() {
        assertDiscoveredTests(
            filter = TestMetadataFilter(listOf("testData/analysis/api/expressions/call")),
            expectedIncluded = setOf("testCall()"),
            expectedExcluded = setOf("testSymbols()", "testTypes()", "testLambda()", "testChecker()", "testInference()"),
        )
    }

    @Test
    fun `filter with multiple paths`() {
        assertDiscoveredTests(
            filter = TestMetadataFilter(listOf("testData/analysis/api/symbols", "testData/compiler/fir/resolve")),
            expectedIncluded = setOf("testSymbols()", "testInference()"),
            expectedExcluded = setOf("testTypes()", "testCall()", "testLambda()", "testChecker()"),
        )
    }

    @Test
    fun `filter with exact file path including extension`() {
        assertDiscoveredTests(
            filter = TestMetadataFilter(listOf("testData/analysis/api/symbols.kt")),
            expectedIncluded = setOf("testSymbols()"),
            expectedExcluded = setOf("testTypes()", "testCall()", "testLambda()", "testChecker()", "testInference()"),
        )
    }

    @Test
    fun `filter with non-existing file path excludes all`() {
        assertDiscoveredTests(
            filter = TestMetadataFilter(listOf("testData/analysis/api/nonexistent.kt")),
            expectedIncluded = emptySet(),
            expectedExcluded = setOf("testSymbols()", "testTypes()", "testCall()", "testLambda()", "testChecker()", "testInference()"),
        )
    }
}
