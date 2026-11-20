/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import org.jetbrains.kotlin.analysis.api.fir.test.cases.components.resolver.AbstractSourceLikeResolveCandidatesByFileFirTreeConsistencyTest
import org.jetbrains.kotlin.analysis.api.fir.test.cases.components.resolver.AbstractSourceLikeResolveCandidatesFirTreeConsistencyTest
import org.jetbrains.kotlin.analysis.api.fir.test.cases.imports.AbstractKaDefaultImportsProviderTest
import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.*
import org.jetbrains.kotlin.generators.tests.analysis.api.generateAnalysisApiTests
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        AnalysisApiTestGenerator(this, listOf(AnalysisApiFirTestConfiguratorFactory)).run {
            generateAnalysisApiTests()

            group("imports") {
                test<AbstractKaDefaultImportsProviderTest>(
                    filter = analysisSessionModeIs(AnalysisSessionMode.Normal)
                            and testModuleKindIs(TestModuleKind.Source),
                ) {
                    model("defaultImportProvider")
                }
            }
        }

        testGroup(testsRoot = "analysis/analysis-api-fir/tests-gen", testDataRoot = "analysis/analysis-api/testData") {
            testClass<AbstractSourceLikeResolveCandidatesFirTreeConsistencyTest> {
                model("components/resolver/singleByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSourceLikeResolveCandidatesByFileFirTreeConsistencyTest> {
                model("components/resolver/allByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }
    }
}
