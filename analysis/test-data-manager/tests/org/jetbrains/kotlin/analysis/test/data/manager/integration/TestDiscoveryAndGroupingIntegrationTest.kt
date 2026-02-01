/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.integration

import org.jetbrains.kotlin.analysis.test.data.manager.GroupingResult
import org.jetbrains.kotlin.analysis.test.data.manager.VariantChainConflict
import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerRunner
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts.FakeConflictingTestABGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts.FakeConflictingTestBAGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts.FakeConflictingTestXBGenerated
import org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts.FakeNonConflictingTestXYGenerated
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.platform.engine.Filter
import org.junit.platform.engine.discovery.PackageNameFilter

/**
 * Integration tests for the discovery and grouping workflow.
 *
 * These tests verify the full pipeline:
 * 1. `buildDiscoveryRequest()` - builds JUnit discovery request with filters
 * 2. `discoverTests()` - discovers tests from the test plan
 * 3. `groupByVariantDepth()` - groups tests and detects conflicts
 *
 * Tests use production functions with fake test classes to verify real behavior.
 */
internal class TestDiscoveryAndGroupingIntegrationTest : AbstractFakeTestIntegrationTest() {
    /**
     * Formats grouping result as unified readable string.
     * Shows: groups (tests that run in parallel), test names with outer classes, variant chains, conflicts.
     *
     * Example output:
     * ```
     * === Group 0 (golden) ===
     * FakeGoldenAnalysisApiTestGenerated.testSymbols() -> []
     * FakeGoldenAnalysisApiTestGenerated.testTypes() -> []
     * FakeGoldenAnalysisApiTestGenerated.Expressions.testCall() -> []
     *
     * === Group 1 (depth=1) ===
     * Variants: [[knm], [lib]]
     * FakeLibLightClassesTestGenerated.testSimple() -> [lib]
     * FakeKnmLightClassesTestGenerated.testSimple() -> [knm]
     *
     * === Conflicts ===
     * [a, b] writes 'b' which [b, a] reads
     * ```
     */
    private fun formatResult(result: GroupingResult): String = buildString {
        for (group in result.groups) {
            val header = if (group.variantDepth == 0) "Group 0 (golden)" else "Group ${group.variantDepth} (depth=${group.variantDepth})"
            appendLine("=== $header ===")
            if (group.variantDepth > 0 && group.uniqueVariantChains.isNotEmpty()) {
                appendLine("Variants: ${group.uniqueVariantChains}")
            }

            // Sort by full class path + method name for deterministic output
            for (test in group.tests.sortedBy { getShortClassName(it.uniqueId) + "." + it.displayName }) {
                val className = getShortClassName(test.uniqueId)
                appendLine("$className.${test.displayName} -> ${test.variantChain}")
            }

            appendLine()
        }

        if (result.conflicts.isNotEmpty()) {
            appendLine("=== Conflicts ===")
            // Sort conflicts for deterministic output
            result.conflicts.map(VariantChainConflict::format).sorted().forEach(::appendLine)
        }
    }.trimEnd()

    private fun assertGroupingResultWithoutConflicts(
        testClassPattern: String? = null,
        testDataPath: String? = null,
        expected: String,
    ) {
        assertGroupingResult(
            testClassPattern = testClassPattern,
            testDataPath = testDataPath,
            PackageNameFilter.excludePackageNames(FakeConflictingTestABGenerated::class.java.`package`.name),
            expected = expected,
        )
    }

    private fun assertGroupingResult(
        testClassPattern: String? = null,
        testDataPath: String? = null,
        vararg additionalFilters: Filter<*>,
        expected: String,
    ) {
        val tests = discoverFakeTests(testClassPattern, testDataPath, *additionalFilters)
        val result = TestDataManagerRunner.groupByVariantDepth(tests)

        assertEquals(expected.trimIndent(), formatResult(result))
    }

    @Test
    fun `discovery with default arguments finds all tests and conflicts`() {
        assertGroupingResult(
            expected = """
                === Group 0 (golden) ===
                FakeFirTestGenerated.Resolve.testInference() -> []
                FakeFirTestGenerated.testChecker() -> []
                FakeGoldenAnalysisApiTestGenerated.Expressions.testCall() -> []
                FakeGoldenAnalysisApiTestGenerated.Expressions.testLambda() -> []
                FakeGoldenAnalysisApiTestGenerated.testSymbols() -> []
                FakeGoldenAnalysisApiTestGenerated.testTypes() -> []
                FakeGoldenLightClassesTestGenerated.NestedDir.testInner() -> []
                FakeGoldenLightClassesTestGenerated.testSimple() -> []
                
                === Group 1 (depth=1) ===
                Variants: [[knm], [lib], [librarySource], [standalone]]
                FakeKnmLightClassesTestGenerated.NestedDir.testInner() -> [knm]
                FakeKnmLightClassesTestGenerated.testSimple() -> [knm]
                FakeLibLightClassesTestGenerated.NestedDir.testInner() -> [lib]
                FakeLibLightClassesTestGenerated.testSimple() -> [lib]
                FakeLibrarySourceTestGenerated.Expressions.testCall() -> [librarySource]
                FakeLibrarySourceTestGenerated.Expressions.testLambda() -> [librarySource]
                FakeLibrarySourceTestGenerated.testSymbols() -> [librarySource]
                FakeLibrarySourceTestGenerated.testTypes() -> [librarySource]
                FakeStandaloneAnalysisApiTestGenerated.Expressions.testCall() -> [standalone]
                FakeStandaloneAnalysisApiTestGenerated.Expressions.testLambda() -> [standalone]
                FakeStandaloneAnalysisApiTestGenerated.testSymbols() -> [standalone]
                FakeStandaloneAnalysisApiTestGenerated.testTypes() -> [standalone]
                
                === Group 2 (depth=2) ===
                Variants: [[a, b], [b, a], [knm, wasm], [lib, kmp.lib], [x, b], [x, y]]
                FakeConflictingTestABGenerated.testAB() -> [a, b]
                FakeConflictingTestBAGenerated.testBA() -> [b, a]
                FakeConflictingTestXBGenerated.testXB() -> [x, b]
                FakeLibKmpLightClassesTestGenerated.NestedDir.testInner() -> [lib, kmp.lib]
                FakeLibKmpLightClassesTestGenerated.testSimple() -> [lib, kmp.lib]
                FakeNonConflictingTestXYGenerated.testXY() -> [x, y]
                FakeWasmLightClassesTestGenerated.NestedDir.testInner() -> [knm, wasm]
                FakeWasmLightClassesTestGenerated.testSimple() -> [knm, wasm]
                
                === Conflicts ===
                [a, b] writes 'b' which [b, a] reads
                [b, a] writes 'a' which [a, b] reads
                [x, b] writes 'b' which [a, b] reads
                [x, b] writes 'b' which [b, a] reads
            """
        )
    }

    @Test
    fun `discovery with non-matching pattern finds nothing`() {
        assertGroupingResult(
            testClassPattern = ".*NonExistentPattern.*",
            expected = ""
        )
    }

    @Test
    fun `discovery with testClassPattern for lightclasses fakes`() {
        assertGroupingResult(
            testClassPattern = ".*Fake.*LightClassesTestGenerated",
            expected = """
                === Group 0 (golden) ===
                FakeGoldenLightClassesTestGenerated.NestedDir.testInner() -> []
                FakeGoldenLightClassesTestGenerated.testSimple() -> []

                === Group 1 (depth=1) ===
                Variants: [[knm], [lib]]
                FakeKnmLightClassesTestGenerated.NestedDir.testInner() -> [knm]
                FakeKnmLightClassesTestGenerated.testSimple() -> [knm]
                FakeLibLightClassesTestGenerated.NestedDir.testInner() -> [lib]
                FakeLibLightClassesTestGenerated.testSimple() -> [lib]

                === Group 2 (depth=2) ===
                Variants: [[knm, wasm], [lib, kmp.lib]]
                FakeLibKmpLightClassesTestGenerated.NestedDir.testInner() -> [lib, kmp.lib]
                FakeLibKmpLightClassesTestGenerated.testSimple() -> [lib, kmp.lib]
                FakeWasmLightClassesTestGenerated.NestedDir.testInner() -> [knm, wasm]
                FakeWasmLightClassesTestGenerated.testSimple() -> [knm, wasm]
            """
        )
    }

    @Test
    fun `discovery with testDataPath filters by TestMetadata`() {
        assertGroupingResultWithoutConflicts(
            testClassPattern = ".*Fake.*",
            testDataPath = "testData/lightClasses",
            expected = """
                === Group 0 (golden) ===
                FakeGoldenLightClassesTestGenerated.NestedDir.testInner() -> []
                FakeGoldenLightClassesTestGenerated.testSimple() -> []

                === Group 1 (depth=1) ===
                Variants: [[knm], [lib]]
                FakeKnmLightClassesTestGenerated.NestedDir.testInner() -> [knm]
                FakeKnmLightClassesTestGenerated.testSimple() -> [knm]
                FakeLibLightClassesTestGenerated.NestedDir.testInner() -> [lib]
                FakeLibLightClassesTestGenerated.testSimple() -> [lib]

                === Group 2 (depth=2) ===
                Variants: [[knm, wasm], [lib, kmp.lib]]
                FakeLibKmpLightClassesTestGenerated.NestedDir.testInner() -> [lib, kmp.lib]
                FakeLibKmpLightClassesTestGenerated.testSimple() -> [lib, kmp.lib]
                FakeWasmLightClassesTestGenerated.NestedDir.testInner() -> [knm, wasm]
                FakeWasmLightClassesTestGenerated.testSimple() -> [knm, wasm]
            """
        )
    }

    @Test
    fun `discovery with testDataPath for analysis fakes`() {
        assertGroupingResultWithoutConflicts(
            testClassPattern = ".*Fake.*",
            testDataPath = "testData/analysis",
            expected = """
                === Group 0 (golden) ===
                FakeGoldenAnalysisApiTestGenerated.Expressions.testCall() -> []
                FakeGoldenAnalysisApiTestGenerated.Expressions.testLambda() -> []
                FakeGoldenAnalysisApiTestGenerated.testSymbols() -> []
                FakeGoldenAnalysisApiTestGenerated.testTypes() -> []

                === Group 1 (depth=1) ===
                Variants: [[librarySource], [standalone]]
                FakeLibrarySourceTestGenerated.Expressions.testCall() -> [librarySource]
                FakeLibrarySourceTestGenerated.Expressions.testLambda() -> [librarySource]
                FakeLibrarySourceTestGenerated.testSymbols() -> [librarySource]
                FakeLibrarySourceTestGenerated.testTypes() -> [librarySource]
                FakeStandaloneAnalysisApiTestGenerated.Expressions.testCall() -> [standalone]
                FakeStandaloneAnalysisApiTestGenerated.Expressions.testLambda() -> [standalone]
                FakeStandaloneAnalysisApiTestGenerated.testSymbols() -> [standalone]
                FakeStandaloneAnalysisApiTestGenerated.testTypes() -> [standalone]
            """
        )
    }

    @Test
    fun `grouping with golden-only tests`() {
        assertGroupingResult(
            testClassPattern = ".*FakeGolden.*|.*FakeFir.*",
            expected = """
                === Group 0 (golden) ===
                FakeFirTestGenerated.Resolve.testInference() -> []
                FakeFirTestGenerated.testChecker() -> []
                FakeGoldenAnalysisApiTestGenerated.Expressions.testCall() -> []
                FakeGoldenAnalysisApiTestGenerated.Expressions.testLambda() -> []
                FakeGoldenAnalysisApiTestGenerated.testSymbols() -> []
                FakeGoldenAnalysisApiTestGenerated.testTypes() -> []
                FakeGoldenLightClassesTestGenerated.NestedDir.testInner() -> []
                FakeGoldenLightClassesTestGenerated.testSimple() -> []
            """
        )
    }

    @Test
    fun `conflict detected with swapped prefixes`() {
        assertGroupingResult(
            testClassPattern = "${FakeConflictingTestABGenerated::class.qualifiedName}|${FakeConflictingTestBAGenerated::class.qualifiedName}",
            expected = """
                === Group 2 (depth=2) ===
                Variants: [[a, b], [b, a]]
                FakeConflictingTestABGenerated.testAB() -> [a, b]
                FakeConflictingTestBAGenerated.testBA() -> [b, a]
                
                === Conflicts ===
                [a, b] writes 'b' which [b, a] reads
                [b, a] writes 'a' which [a, b] reads
            """
        )
    }

    @Test
    fun `conflict detected with same last prefix`() {
        assertGroupingResult(
            testClassPattern = "${FakeConflictingTestABGenerated::class.qualifiedName}|${FakeConflictingTestXBGenerated::class.qualifiedName}",
            expected = """
                === Group 2 (depth=2) ===
                Variants: [[a, b], [x, b]]
                FakeConflictingTestABGenerated.testAB() -> [a, b]
                FakeConflictingTestXBGenerated.testXB() -> [x, b]

                === Conflicts ===
                [x, b] writes 'b' which [a, b] reads
            """
        )
    }

    @Test
    fun `no conflicts with non-overlapping prefixes`() {
        assertGroupingResult(
            testClassPattern = "${FakeConflictingTestABGenerated::class.qualifiedName}|${FakeNonConflictingTestXYGenerated::class.qualifiedName}",
            expected = """
                === Group 2 (depth=2) ===
                Variants: [[a, b], [x, y]]
                FakeConflictingTestABGenerated.testAB() -> [a, b]
                FakeNonConflictingTestXYGenerated.testXY() -> [x, y]
            """
        )
    }

    @Test
    fun `discovery with nested testDataPath`() {
        assertGroupingResultWithoutConflicts(
            testDataPath = "testData/analysis/api/expressions",
            expected = """
                === Group 0 (golden) ===
                FakeGoldenAnalysisApiTestGenerated.Expressions.testCall() -> []
                FakeGoldenAnalysisApiTestGenerated.Expressions.testLambda() -> []

                === Group 1 (depth=1) ===
                Variants: [[standalone]]
                FakeStandaloneAnalysisApiTestGenerated.Expressions.testCall() -> [standalone]
                FakeStandaloneAnalysisApiTestGenerated.Expressions.testLambda() -> [standalone]
            """
        )
    }

    @Test
    fun `discovery with exact file path in testDataPath`() {
        assertGroupingResultWithoutConflicts(
            testDataPath = "testData/analysis/api/expressions/call",
            expected = """
                === Group 0 (golden) ===
                FakeGoldenAnalysisApiTestGenerated.Expressions.testCall() -> []

                === Group 1 (depth=1) ===
                Variants: [[standalone]]
                FakeStandaloneAnalysisApiTestGenerated.Expressions.testCall() -> [standalone]
            """
        )
    }

    @Test
    fun `discovery with multiple comma-separated testDataPaths`() {
        assertGroupingResultWithoutConflicts(
            testDataPath = "testData/analysis/api/symbols,testData/compiler/fir/resolve",
            expected = """
                === Group 0 (golden) ===
                FakeFirTestGenerated.Resolve.testInference() -> []
                FakeGoldenAnalysisApiTestGenerated.testSymbols() -> []

                === Group 1 (depth=1) ===
                Variants: [[standalone]]
                FakeStandaloneAnalysisApiTestGenerated.testSymbols() -> [standalone]
            """
        )
    }

    @Test
    fun `discovery with testDataPath including file extension`() {
        assertGroupingResultWithoutConflicts(
            testDataPath = "testData/analysis/api/symbols.kt",
            expected = """
                === Group 0 (golden) ===
                FakeGoldenAnalysisApiTestGenerated.testSymbols() -> []

                === Group 1 (depth=1) ===
                Variants: [[standalone]]
                FakeStandaloneAnalysisApiTestGenerated.testSymbols() -> [standalone]
            """
        )
    }

    @Test
    fun `discovery with non-existing testDataPath returns empty`() {
        assertGroupingResultWithoutConflicts(
            testDataPath = "testData/nonexistent/path",
            expected = ""
        )
    }

    @Test
    fun `discovery with testDataPath for nested directory only`() {
        assertGroupingResultWithoutConflicts(
            testDataPath = "testData/lightClasses/nestedDir",
            expected = """
                === Group 0 (golden) ===
                FakeGoldenLightClassesTestGenerated.NestedDir.testInner() -> []

                === Group 1 (depth=1) ===
                Variants: [[knm], [lib]]
                FakeKnmLightClassesTestGenerated.NestedDir.testInner() -> [knm]
                FakeLibLightClassesTestGenerated.NestedDir.testInner() -> [lib]

                === Group 2 (depth=2) ===
                Variants: [[knm, wasm], [lib, kmp.lib]]
                FakeLibKmpLightClassesTestGenerated.NestedDir.testInner() -> [lib, kmp.lib]
                FakeWasmLightClassesTestGenerated.NestedDir.testInner() -> [knm, wasm]
            """
        )
    }

    @Test
    fun `discovery filters by both testClassPattern and testDataPath together`() {
        assertGroupingResult(
            testClassPattern = ".*FakeLib.*LightClasses.*",
            testDataPath = "testData/lightClasses",
            expected = """
                === Group 1 (depth=1) ===
                Variants: [[lib]]
                FakeLibLightClassesTestGenerated.NestedDir.testInner() -> [lib]
                FakeLibLightClassesTestGenerated.testSimple() -> [lib]

                === Group 2 (depth=2) ===
                Variants: [[lib, kmp.lib]]
                FakeLibKmpLightClassesTestGenerated.NestedDir.testInner() -> [lib, kmp.lib]
                FakeLibKmpLightClassesTestGenerated.testSimple() -> [lib, kmp.lib]
            """
        )
    }

    @Test
    fun `grouping with prefixed-only tests (no golden)`() {
        assertGroupingResult(
            testClassPattern = ".*FakeLib.*LightClasses.*|.*FakeKnm.*",
            expected = """
                === Group 1 (depth=1) ===
                Variants: [[knm], [lib]]
                FakeKnmLightClassesTestGenerated.NestedDir.testInner() -> [knm]
                FakeKnmLightClassesTestGenerated.testSimple() -> [knm]
                FakeLibLightClassesTestGenerated.NestedDir.testInner() -> [lib]
                FakeLibLightClassesTestGenerated.testSimple() -> [lib]

                === Group 2 (depth=2) ===
                Variants: [[lib, kmp.lib]]
                FakeLibKmpLightClassesTestGenerated.NestedDir.testInner() -> [lib, kmp.lib]
                FakeLibKmpLightClassesTestGenerated.testSimple() -> [lib, kmp.lib]
            """
        )
    }

    private companion object {
        val CLASS_REGEX = Regex("""\[(?:class|nested-class):([^]]+)]""")

        /**
         * Extracts a short class name chain from test unique ID.
         * E.g., "[class:...FakeGoldenAnalysisApiTestGenerated]/[nested-class:Expressions]" -> "FakeGoldenAnalysisApiTestGenerated.Expressions"
         */
        private fun getShortClassName(uniqueId: String): String {
            return CLASS_REGEX.findAll(uniqueId).joinToString(".") { it.groupValues[1].substringAfterLast('.') }
        }
    }
}
