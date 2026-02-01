/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.integration

import org.jetbrains.kotlin.analysis.test.data.manager.filters.formatTestName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Integration tests for [formatTestName] functionality.
 *
 * These tests verify that test names are correctly formatted for all fake test classes.
 */
internal class FormatTestNameIntegrationTest : AbstractFakeTestIntegrationTest() {
    @Test
    fun `formatTestName renders all fake tests correctly`() {
        val formattedNames = discoverFakeTests { it.formatTestName() }
            .sorted()
            .joinToString("\n")

        assertEquals(
            $$"""
                FakeConflictingTestABGenerated.testAB
                FakeConflictingTestBAGenerated.testBA
                FakeConflictingTestXBGenerated.testXB
                FakeFirTestGenerated$Resolve.testInference
                FakeFirTestGenerated.testChecker
                FakeGoldenAnalysisApiTestGenerated$Expressions.testCall
                FakeGoldenAnalysisApiTestGenerated$Expressions.testLambda
                FakeGoldenAnalysisApiTestGenerated.testSymbols
                FakeGoldenAnalysisApiTestGenerated.testTypes
                FakeGoldenLightClassesTestGenerated$NestedDir.testInner
                FakeGoldenLightClassesTestGenerated.testSimple
                FakeKnmLightClassesTestGenerated$NestedDir.testInner
                FakeKnmLightClassesTestGenerated.testSimple
                FakeLibKmpLightClassesTestGenerated$NestedDir.testInner
                FakeLibKmpLightClassesTestGenerated.testSimple
                FakeLibLightClassesTestGenerated$NestedDir.testInner
                FakeLibLightClassesTestGenerated.testSimple
                FakeLibrarySourceTestGenerated$Expressions.testCall
                FakeLibrarySourceTestGenerated$Expressions.testLambda
                FakeLibrarySourceTestGenerated.testSymbols
                FakeLibrarySourceTestGenerated.testTypes
                FakeNonConflictingTestXYGenerated.testXY
                FakeStandaloneAnalysisApiTestGenerated$Expressions.testCall
                FakeStandaloneAnalysisApiTestGenerated$Expressions.testLambda
                FakeStandaloneAnalysisApiTestGenerated.testSymbols
                FakeStandaloneAnalysisApiTestGenerated.testTypes
                FakeWasmLightClassesTestGenerated$NestedDir.testInner
                FakeWasmLightClassesTestGenerated.testSimple
            """.trimIndent(),
            formattedNames,
        )
    }
}
