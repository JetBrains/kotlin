/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.descriptors.test.AbstractKtFe10ResolveCallTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.test.symbols.AbstractKtFe10SymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.symbols.AbstractKtFe10SymbolByPsiTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.symbols.AbstractKtFe10SymbolByReferenceTest
import org.jetbrains.kotlin.analysis.api.fir.AbstractFirReferenceResolveTest
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.scopes.AbstractFirDelegateMemberScopeTest
import org.jetbrains.kotlin.analysis.api.fir.scopes.AbstractFirFileScopeTest
import org.jetbrains.kotlin.analysis.api.fir.scopes.AbstractFirMemberScopeByFqNameTest
import org.jetbrains.kotlin.analysis.api.fir.symbols.AbstractFirSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.fir.symbols.AbstractFirSymbolByPsiTest
import org.jetbrains.kotlin.analysis.api.fir.symbols.AbstractFirSymbolByReferenceTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractFileStructureTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractInnerDeclarationsResolvePhaseTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTestSpec

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"


    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("analysis/analysis-api-fir/tests", "analysis/analysis-api/testData") {
            testClass<AbstractFirResolveCallTest> {
                model("analysisSession/resolveCall")
            }

            testClass<AbstractFirMemberScopeByFqNameTest> {
                model("scopes/memberScopeByFqName")
            }

            testClass<AbstractFirFileScopeTest> {
                model("scopes/fileScopeTest", extension = "kt")
            }

            testClass<AbstractFirDelegateMemberScopeTest> {
                model("scopes/delegatedMemberScope")
            }

            testClass<AbstractFirSymbolByPsiTest> {
                model("symbols/symbolByPsi")
            }

            testClass<AbstractFirSymbolByFqNameTest> {
                model("symbols/symbolByFqName")
            }

            testClass<AbstractFirSymbolByReferenceTest> {
                model("symbols/symbolByReference")
            }

            testClass<AbstractFirCompileTimeConstantEvaluatorTest> {
                model("components/compileTimeConstantEvaluator")
            }

            testClass<AbstractFirExpectedExpressionTypeTest> {
                model("components/expectedExpressionType")
            }

            testClass<AbstractFirFunctionClassKindTest> {
                model("components/functionClassKind")
            }

            testClass<AbstractFirOverriddenDeclarationProviderTest> {
                model("components/overriddenDeclarations")
            }

            testClass<AbstractFirHLExpressionTypeTest> {
                model("components/expressionType")
            }

            testClass<AbstractFirRendererTest> {
                model("components/declarationRenderer")
            }

            testClass<AbstractFirReferenceResolveTest> {
                model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractHLImportOptimizerTest> {
                model("components/importOptimizer", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractFirHasCommonSubtypeTest> {
                model("components/hasCommonSubtype")
            }

            testClass<AbstractFirGetSuperTypesTest> {
                model("components/getSuperTypes")
            }

            testClass<AbstractPsiTypeProviderTest> {
                model("components/psiTypeProvider")
            }

            testClass<AbstractExpressionPsiTypeProviderTest> {
                model("components/expressionPsiType")
            }

            testClass<AbstractFirHLSmartCastInfoTest> {
                model("components/smartCastInfo")
            }

            testClass<AbstractFirWhenMissingCasesTest> {
                model("components/whenMissingCases")
            }
        }

        testGroup("analysis/analysis-api-fe10/tests", "analysis/analysis-api/testData") {
            testClass<AbstractKtFe10ResolveCallTest> {
                model("analysisSession/resolveCall")
            }

//            testClass<AbstractKtFe10MemberScopeByFqNameTest> {
//                model("scopes/memberScopeByFqName")
//            }

//            testClass<AbstractKtFe10FileScopeTest> {
//                model("scopes/fileScopeTest", extension = "kt")
//            }

            testClass<AbstractKtFe10SymbolByPsiTest> {
                model("symbols/symbolByPsi")
            }

//            testClass<AbstractKtFe10CompileTimeConstantEvaluatorTest> {
//                model("components/compileTimeConstantEvaluator")
//            }

            testClass<AbstractKtFe10SymbolByFqNameTest> {
                model("symbols/symbolByFqName")
            }

            testClass<AbstractKtFe10SymbolByReferenceTest> {
                model("symbols/symbolByReference")
            }

            testClass<AbstractKtFe10ExpectedExpressionTypeTest> {
                model("components/expectedExpressionType")
            }

//            testClass<AbstractKtFe10FunctionClassKindTest> {
//                model("components/functionClassKind")
//            }

            testClass<AbstractKtFe10OverriddenDeclarationProviderTest> {
                model("components/overriddenDeclarations")
            }

            testClass<AbstractKtFe10HLExpressionTypeTest> {
                model("components/expressionType")
            }

            testClass<AbstractKtFe10RendererTest> {
                model("components/declarationRenderer")
            }

//            testClass<AbstractKtFe10ReferenceResolveTest> {
//                model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
//            }

            testClass<AbstractKtFe10HasCommonSubtypeTest> {
                model("components/hasCommonSubtype")
            }

            testClass<AbstractKtFe10HLSmartCastInfoTest> {
                model("components/smartCastInfo")
            }

            testClass<AbstractKtFe10WhenMissingCasesTest> {
                model("components/whenMissingCases")
            }
        }

        testGroup("analysis/low-level-api-fir/tests", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractFirLazyBodiesCalculatorTest> {
                model("rawBuilder", testMethod = "doTest")
            }
        }

        testGroup("analysis/low-level-api-fir/tests", "analysis/low-level-api-fir/testdata") {
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
            "analysis/low-level-api-fir/tests",
            "compiler/fir/analysis-tests/testData",
        ) {
            testClass<AbstractDiagnosisCompilerTestDataTest>(suiteTestClassName = "DiagnosisCompilerFirTestdataTestGenerated") {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup(
            "analysis/low-level-api-fir/tests",
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


        testGroup("analysis/low-level-api-fir/tests", testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH) {
            testClass<AbstractDiagnosisCompilerTestDataSpecTest>(suiteTestClassName = "FirIdeSpecTest") {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = excludedFirTestdataPattern,
                )
            }
        }
        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH) {
            testClass<AbstractFirDiagnosticTestSpec> {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = excludedFirTestdataPattern
                )
            }
        }
    }
}
