/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.analysis.api.standalone.AbstractStandaloneModeSingleModuleTest
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.*
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.AnalysisMode
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.Frontend
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.TestModuleKind
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.createConfigurator

internal fun TestGroupSuite.generateStandaloneModeTests() {
    testGroup(
        "analysis/analysis-api-standalone/tests",
        "analysis/analysis-api/testData"
    ) {
        val configurator = createConfigurator(Frontend.FIR, TestModuleKind.STANDALONE_MODE, analysisMode = AnalysisMode.NORMAL)
        testClass<AbstractStandaloneModeSingleModuleTest> {
            method(FrontendConfiguratorTestModel(configurator))
            model("standalone/singleModule")
        }
    }
}
