/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.frontend.api

import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.idea.fir.frontend.api.AbstractReferenceResolveTest
import org.jetbrains.kotlin.idea.fir.frontend.api.components.AbstractExpectedExpressionTypeTest
import org.jetbrains.kotlin.idea.fir.frontend.api.components.AbstractHLExpressionTypeTest
import org.jetbrains.kotlin.idea.fir.frontend.api.components.AbstractOverriddenDeclarationProviderTest
import org.jetbrains.kotlin.idea.fir.frontend.api.components.AbstractRendererTest
import org.jetbrains.kotlin.idea.fir.frontend.api.fir.AbstractResolveCallTest
import org.jetbrains.kotlin.idea.fir.frontend.api.scopes.AbstractFileScopeTest
import org.jetbrains.kotlin.idea.fir.frontend.api.scopes.AbstractMemberScopeByFqNameTest
import org.jetbrains.kotlin.idea.fir.frontend.api.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.idea.fir.frontend.api.symbols.AbstractSymbolByPsiTest
import org.jetbrains.kotlin.idea.fir.frontend.api.symbols.AbstractSymbolByReferenceTest
import org.jetbrains.kotlin.idea.fir.low.level.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic.AbstractDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic.AbstractFirContextCollectionTest
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataSpecTest
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataTest
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.AbstractFileStructureTest
import org.jetbrains.kotlin.idea.fir.low.level.api.resolve.AbstractInnerDeclarationsResolvePhaseTest
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.generators.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"


    generateTestGroupSuiteWithJUnit5(args) {

        testGroup("idea/idea-frontend-fir/tests", "idea/idea-frontend-fir/testData") {
            testClass<AbstractResolveCallTest> {
                model("analysisSession/resolveCall")
            }

            testClass<AbstractMemberScopeByFqNameTest> {
                model("memberScopeByFqName")
            }

            testClass<AbstractFileScopeTest> {
                model("fileScopeTest", extension = "kt")
            }

            testClass<AbstractSymbolByPsiTest> {
                model("symbols/symbolByPsi")
            }

            testClass<AbstractSymbolByFqNameTest> {
                model("symbols/symbolByFqName")
            }

            testClass<AbstractSymbolByReferenceTest> {
                model("symbols/symbolByReference")
            }

            testClass<AbstractExpectedExpressionTypeTest> {
                model("components/expectedExpressionType")
            }

            testClass<AbstractOverriddenDeclarationProviderTest> {
                model("components/overridenDeclarations")
            }

            testClass<AbstractHLExpressionTypeTest> {
                model("components/expressionType")
            }

            testClass<AbstractRendererTest> {
                model("components/declarationRenderer")
            }

            testClass<AbstractReferenceResolveTest> {
                model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup("idea/idea-frontend-fir/idea-fir-low-level-api/tests", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractFirLazyBodiesCalculatorTest> {
                model("rawBuilder", testMethod = "doTest")
            }
        }

        testGroup("idea/idea-frontend-fir/idea-fir-low-level-api/tests", "idea/idea-frontend-fir/idea-fir-low-level-api/testdata") {
            testClass<AbstractFirOnAirResolveTest> {
                model("onAirResolve")
            }

            testClass<AbstractFirLazyDeclarationResolveTest> {
                model("lazyResolve")
            }

            testClass<AbstractFileStructureTest> {
                model("fileStructure")
            }

            testClass<AbstractFirContextCollectionTest> {
                model("fileStructure")
            }

            testClass<AbstractDiagnosticTraversalCounterTest> {
                model("diagnosticTraversalCounter")
            }

            testClass<AbstractInnerDeclarationsResolvePhaseTest> {
                model("innerDeclarationsResolve")
            }

            testClass<AbstractPartialRawFirBuilderTestCase> {
                model("partialRawBuilder", testMethod = "doRawFirTest")
            }

            testClass<AbstractGetOrBuildFirTest> {
                model("getOrBuildFir")
            }
        }

        testGroup(
            "idea/idea-frontend-fir/idea-fir-low-level-api/tests",
            "compiler/fir/analysis-tests/testData",
        ) {
            testClass<AbstractDiagnosisCompilerTestDataTest>(suiteTestClassName = "DiagnosisCompilerFirTestdataTestGenerated") {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup(
            "idea/idea-frontend-fir/idea-fir-low-level-api/tests",
            "compiler/testData",
        ) {
            testClass<AbstractDiagnosisCompilerTestDataTest>(suiteTestClassName = "DiagnosisCompilerTestFE10TestdataTestGenerated") {
                model(
                    "diagnostics/tests",
                    excludedPattern = excludedFirTestdataPattern,
                )
                model(
                    "diagnostics/testsWithStdLib",
                    excludedPattern = excludedFirTestdataPattern,
                    excludeDirs = listOf("native")
                )
            }
        }


        testGroup("idea/idea-frontend-fir/idea-fir-low-level-api/tests", testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH) {
            testClass<AbstractDiagnosisCompilerTestDataSpecTest>(suiteTestClassName = "FirIdeSpecTest") {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = excludedFirTestdataPattern,
                )
            }
        }
    }
}