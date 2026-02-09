/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.AbstractResolveToFirSymbolTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.*
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
            testClass<AbstractSourceLazyAnnotationsResolveTest> {
                model("lazyAnnotations", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptLazyAnnotationsResolveTest> {
                model("lazyAnnotations", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceDeprecationsResolveTest> {
                model("lazyResolveDeprecation", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirSourceLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirOutOfContentRootLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirOutOfContentRootWithDependenciesLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirScriptLazyDeclarationResolveTest> {
                model("lazyResolve", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractFirCustomScriptDefinitionLazyDeclarationResolveTest> {
                model("lazyResolveCustomScriptDefinition", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceLazyTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootLazyTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptLazyTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceLazyDeclarationResolveForTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootLazyDeclarationResolveForTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptLazyDeclarationResolveForTypeAnnotationsTest> {
                model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractStdLibSourcesLazyDeclarationResolveTest> {
                model("lazyResolveStdlibSources")
            }

            testClass<AbstractBuiltinsBinaryLazyDeclarationResolveTest> {
                model("lazyResolveBuiltinsBinary")
            }

            testClass<AbstractFirSourceLazyDeclarationResolveByReferenceTest> {
                model("lazyResolveByReference")
            }

            testClass<AbstractSourceLazyDeclarationResolveScopeBasedTest> {
                model("lazyResolveScopes", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootLazyDeclarationResolveScopeBasedTest> {
                model("lazyResolveScopes", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptLazyDeclarationResolveScopeBasedTest> {
                model("lazyResolveScopes", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractErrorResistanceTest> {
                model("errorResistance")
            }

            testClass<AbstractSourceInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceDanglingFileInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootDanglingFileInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptDanglingFileInBlockModificationTest> {
                model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractCodeFragmentInBlockModificationTest> {
                model("inBlockModification/codeFragments", recursive = false, pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractContentAndResolutionScopesProvidersTest> {
                model("contentAndResolutionScopesProviders", recursive = false, pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
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

            testClass<AbstractSourceFileStructureTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootFileStructureTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptFileStructureTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractFirSourceContextCollectionTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirOutOfContentRootContextCollectionTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptContextCollectionTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceDiagnosticTraversalCounterTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptDiagnosticTraversalCounterTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceWholeFileResolvePhaseTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootWholeFileResolvePhaseTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptWholeFileResolvePhaseTest> {
                model("fileStructure", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourcePartialRawFirBuilderTestCase> {
                model("partialRawBuilder", testMethod = "doRawFirTest")
            }

            testClass<AbstractOutOfContentRootPartialRawFirBuilderTestCase> {
                model("partialRawBuilder", testMethod = "doRawFirTest")
            }

            testClass<AbstractSourceGetOrBuildFirTest> {
                model("getOrBuildFir", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractOutOfContentRootGetOrBuildFirTest> {
                model("getOrBuildFir", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptGetOrBuildFirTest> {
                model("getOrBuildFir", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractInterruptingSourceGetOrBuildFirTest> {
                model("getOrBuildFirWithInterruption", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractInterruptingScriptGetOrBuildFirTest> {
                model("getOrBuildFirWithInterruption", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractLibraryGetOrBuildFirTest> {
                model("getOrBuildFirBinary")
            }

            testClass<AbstractStdLibBasedGetOrBuildFirTest> {
                model("getOrBuildFirForStdLib")
            }

            testClass<AbstractSourceFileBasedKotlinDeclarationProviderTest> {
                model("fileBasedDeclarationProvider", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptFileBasedKotlinDeclarationProviderTest> {
                model("fileBasedDeclarationProvider", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceNonLocalDeclarationAnchorTest> {
                model("nonLocalDeclarationAnchors", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptNonLocalDeclarationAnchorTest> {
                model("nonLocalDeclarationAnchors", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceClassIdTest> {
                model("classId", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptClassIdTest> {
                model("classId", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceClassIdConsistencyTest> {
                model("classId", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptClassIdConsistencyTest> {
                model("classId", pattern = TestGeneratorUtil.KTS)
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

            testClass<AbstractSourcePsiBasedContainingClassCalculatorConsistencyTest> {
                model("psiBasedContainingClass", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptPsiBasedContainingClassCalculatorConsistencyTest> {
                model("psiBasedContainingClass", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractCombinedPackageDelegationSymbolProviderTest> {
                model("symbolProviders/combinedPackageDelegationSymbolProvider")
            }
        }

        testGroup("analysis/low-level-api-fir/tests-gen", "analysis/analysis-api/testData") {
            testClass<AbstractCodeFragmentCapturingTest> {
                model("components/compilerFacility/compilation/codeFragments/capturing", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup(
            "analysis/low-level-api-fir/tests-gen",
            "analysis/low-level-api-fir/testData",
        ) {
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

        testGroup(testsRoot = "analysis/low-level-api-fir/tests-gen", testDataRoot = "analysis/analysis-api/testData") {
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

            testClass<AbstractSourceResolveCandidatesFirTreeConsistencyTest> {
                model("components/resolver/singleByPsi", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptResolveCandidatesFirTreeConsistencyTest> {
                model("components/resolver/singleByPsi", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractSourceResolveCandidatesByFileFirTreeConsistencyTest> {
                model("components/resolver/allByPsi", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractScriptResolveCandidatesByFileFirTreeConsistencyTest> {
                model("components/resolver/allByPsi", pattern = TestGeneratorUtil.KTS)
            }
        }
    }
}