/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.lowLevelFir.AbstractStandalonePsiClassResolveToFirSymbolTest
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

/**
 * Most Standalone FIR tests are generated from Analysis API test data with [generateAnalysisApiTests]. In contrast,
 * [generateStandaloneFirLowLevelTests] specifically generates Standalone variants of LL FIR tests. These tests are defined in
 * `analysis-api-standalone` instead of `low-level-api-fir` because they need access to Standalone mode test configurators such as
 * [StandaloneModeConfigurator][org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeConfigurator].
 */
internal fun TestGroupSuite.generateStandaloneFirLowLevelTests() {
    testGroup("analysis/analysis-api-standalone/tests-gen", "analysis/low-level-api-fir/testData") {
        testClass<AbstractStandalonePsiClassResolveToFirSymbolTest> {
            model("resolveToFirSymbolPsiClass", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }
}
