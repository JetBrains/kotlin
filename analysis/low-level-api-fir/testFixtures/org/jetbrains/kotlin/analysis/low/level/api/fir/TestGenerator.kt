/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.AbstractResolveToFirSymbolTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirOutOfContentRootContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirSourceLikeContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractSourceLikeDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLPartialDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.AbstractResolveExtensionDisposalAfterModificationEventTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.AbstractCombinedPackageDelegationSymbolProviderTest
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("analysis/low-level-api-fir/tests-gen", "analysis/low-level-api-fir/testData") {
            testClass<AbstractSourceLikeLazyAnnotationsResolveTest> {
                model("lazyAnnotations", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSourceDeprecationsResolveTest> {
                model("lazyResolveDeprecation", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractFirSourceLikeLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractFirOutOfContentRootLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirOutOfContentRootWithDependenciesLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirCustomScriptDefinitionLazyDeclarationResolveTest> {
                model("lazyResolveCustomScriptDefinition", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceLikeLazyTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootLazyTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractSourceLikeLazyDeclarationResolveForTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootLazyDeclarationResolveForTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractStdLibSourcesLazyDeclarationResolveTest> {
                model("lazyResolveStdlibSources")
            }

            testClass<AbstractBuiltinsBinaryLazyDeclarationResolveTest> {
                model("lazyResolveBuiltinsBinary")
            }

            testClass<AbstractFirSourceLazyDeclarationResolveByReferenceTest> {
                model("lazyResolveByReference", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSourceLikeLazyDeclarationResolveScopeBasedTest> {
                model("lazyResolveScopes", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootLazyDeclarationResolveScopeBasedTest> {
                model("lazyResolveScopes", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractErrorResistanceTest> {
                model("errorResistance")
            }

            testClass<AbstractSourceLikeInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractSourceLikeDanglingFileInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootDanglingFileInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractCodeFragmentInBlockModificationTest> {
                model("inBlockModification/codeFragments", recursive = false, pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractContentAndResolutionScopesProvidersTest> {
                model("contentAndResolutionScopesProviders", recursive = false, pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            // Modifiable PSI tests must not be generated until KT-63650 is fixed.
//        testClass<AbstractDeclarationModificationServiceCallExpressionCalleeResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/callExpression",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }
//
//        testClass<AbstractDeclarationModificationServiceDotQualifiedExpressionReceiverResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/dotQualifiedExpression",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }
//
//        testClass<AbstractDeclarationModificationServiceDotQualifiedExpressionSelectorResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/dotQualifiedExpression",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }
//
//        testClass<AbstractDeclarationModificationServicePropertyDeclarationInitializerResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/propertyDeclaration",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }

            testClass<AbstractSourceLikeFileStructureTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootFileStructureTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirSourceLikeContextCollectionTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractFirOutOfContentRootContextCollectionTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractSourceLikeDiagnosticTraversalCounterTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSourceLikeWholeFileResolvePhaseTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootWholeFileResolvePhaseTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractSourcePartialRawFirBuilderTestCase> {
                model("partialRawBuilder", testMethod = "doRawFirTest")
            }

            testClass<AbstractOutOfContentRootPartialRawFirBuilderTestCase> {
                model("partialRawBuilder", testMethod = "doRawFirTest")
            }

            testClass<AbstractSourceLikeGetOrBuildFirTest> {
                model("getOrBuildFir", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractOutOfContentRootGetOrBuildFirTest> {
                model("getOrBuildFir", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractInterruptingSourceLikeGetOrBuildFirTest> {
                model("getOrBuildFirWithInterruption", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractLibraryGetOrBuildFirTest> {
                model("getOrBuildFirBinary")
            }

            testClass<AbstractStdLibBasedGetOrBuildFirTest> {
                model("getOrBuildFirForStdLib")
            }

            testClass<AbstractSourceLikeFileBasedKotlinDeclarationProviderTest> {
                model("fileBasedDeclarationProvider", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSourceLikeNonLocalDeclarationAnchorTest> {
                model("nonLocalDeclarationAnchors", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSourceLikeClassIdTest> {
                model("classId", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractCompilationPeerAnalysisTest> {
                model("compilationPeers")
            }

            testClass<AbstractContextCollectorSourceTest> {
                model("contextCollector", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractContextCollectorScriptTest> {
                model("contextCollector", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractResolveExtensionDisposalAfterModificationEventTest> {
                model("resolveExtensionDisposal")
            }

            testClass<AbstractResolveToFirSymbolTest> {
                model("resolveToFirSymbol")
            }

            testClass<AbstractPsiClassResolveToFirSymbolTest> {
                model("resolveToFirSymbolPsiClass")
            }

            testClass<AbstractSourceLikePsiBasedContainingClassCalculatorConsistencyTest> {
                model("psiBasedContainingClass", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractCombinedPackageDelegationSymbolProviderTest> {
                model("symbolProviders/combinedPackageDelegationSymbolProvider")
            }

            testClass<AbstractSourceTypeArgumentAnnotationCollectionTest> {
                model("annotationPlacement", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            run {
                fun TestGroup.TestClass.modelInit() {
                    model("compilerLikeAnalysis", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                }

                testClass<AbstractLLDiagnosticsTest> {
                    modelInit()
                }
                testClass<AbstractLLReversedDiagnosticsTest> {
                    modelInit()
                }
                testClass<AbstractLLPartialDiagnosticsTest> {
                    modelInit()
                }
            }

            testClass<AbstractSourceLikeDesignationByPsiTest> {
                model("designationByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }

        testGroup(testsRoot = "analysis/low-level-api-fir/tests-gen", testDataRoot = "analysis/analysis-api/testData") {
            testClass<AbstractCodeFragmentCapturingTest> {
                model(
                    relativeRootPath = "components/compilerFacility/compilation/codeFragments/capturing",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
                )
            }

            // Session invalidation test data is shared with analysis session invalidation tests.
            testClass<AbstractModuleStateModificationLLFirSessionInvalidationTest> {
                model("sessions/sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            testClass<AbstractModuleOutOfBlockModificationLLFirSessionInvalidationTest> {
                model("sessions/sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            testClass<AbstractGlobalModuleStateModificationLLFirSessionInvalidationTest> {
                model("sessions/sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            testClass<AbstractGlobalSourceModuleStateModificationLLFirSessionInvalidationTest> {
                model("sessions/sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            testClass<AbstractGlobalSourceOutOfBlockModificationLLFirSessionInvalidationTest> {
                model("sessions/sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            testClass<AbstractCodeFragmentContextModificationLLFirSessionInvalidationTest> {
                model("sessions/sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            testClass<AbstractSourceLikeResolveCandidatesFirTreeConsistencyTest> {
                model("components/resolver/singleByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractSourceLikeResolveCandidatesByFileFirTreeConsistencyTest> {
                model("components/resolver/allByPsi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }
        }
    }
}
