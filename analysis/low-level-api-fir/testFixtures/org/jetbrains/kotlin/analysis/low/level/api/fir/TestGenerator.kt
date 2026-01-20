/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.AbstractResolveToFirSymbolTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirOutOfContentRootContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirSourceContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractScriptContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractScriptDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractSourceDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLPartialDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedSandboxBackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedSandboxDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedScriptBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedScriptWithCustomDefBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedScriptWithCustomDefDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedWasmJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedWasmWasiDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLSandboxBackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLSandboxDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLScriptBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLScriptWithCustomDefBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLScriptWithCustomDefDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLWasmJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLWasmWasiDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractCodeFragmentInBlockModificationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractContentAndResolutionScopesProvidersTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractOutOfContentRootDanglingFileInBlockModificationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractOutOfContentRootFileStructureTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractOutOfContentRootInBlockModificationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractScriptDanglingFileInBlockModificationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractScriptFileStructureTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractScriptInBlockModificationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractSourceDanglingFileInBlockModificationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractSourceFileStructureTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractSourceInBlockModificationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractErrorResistanceTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractOutOfContentRootLazyDeclarationResolveScopeBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractOutOfContentRootWholeFileResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractScriptLazyDeclarationResolveScopeBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractScriptResolveCandidatesByFileFirTreeConsistencyTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractScriptResolveCandidatesFirTreeConsistencyTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractScriptWholeFileResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceLazyDeclarationResolveScopeBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceResolveCandidatesByFileFirTreeConsistencyTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceResolveCandidatesFirTreeConsistencyTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceWholeFileResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.AbstractResolveExtensionDisposalAfterModificationEventTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.AbstractCodeFragmentContextModificationLLFirSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.AbstractGlobalModuleStateModificationLLFirSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.AbstractGlobalSourceModuleStateModificationLLFirSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.AbstractGlobalSourceOutOfBlockModificationLLFirSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.AbstractModuleOutOfBlockModificationLLFirSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.AbstractModuleStateModificationLLFirSessionInvalidationTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.AbstractCombinedPackageDelegationSymbolProviderTest
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.canFreezeIDE
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("analysis/low-level-api-fir/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractFirSourceLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirOutOfContentRootLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirScriptLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractLLSourceAnnotationArgumentsCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractLLSourceStubBasedResolutionTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractLLScriptStubBasedResolutionTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KTS)
            }
        }

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
            "compiler/fir/analysis-tests/testData",
        ) {
            fun TestGroup.TestClass.modelInit() {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME.canFreezeIDE, excludeDirs = listOf("headerMode"))
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME.canFreezeIDE)
            }

            testClass<AbstractLLDiagnosticsTest>(suiteTestClassName = "LLDiagnosticsFirTestGenerated") {
                modelInit()
            }

            testClass<AbstractLLReversedDiagnosticsTest>(suiteTestClassName = "LLReversedDiagnosticsFirTestGenerated") {
                modelInit()
            }

            testClass<AbstractLLPartialDiagnosticsTest>(suiteTestClassName = "LLPartialDiagnosticsFirTestGenerated") {
                modelInit()
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

        testGroup(
            "analysis/low-level-api-fir/tests-gen",
            "plugins/scripting/scripting-tests/testData",
        ) {
            run {
                fun TestGroup.TestClass.scriptDiagnosticsInit() {
                    model(
                        "diagnostics/testScripts",
                        pattern = TestGeneratorUtil.KTS,
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    )
                }

                testClass<AbstractLLScriptWithCustomDefDiagnosticsTest> {
                    scriptDiagnosticsInit()
                }

                testClass<AbstractLLReversedScriptWithCustomDefDiagnosticsTest>() {
                    scriptDiagnosticsInit()
                }
            }

            run {
                fun TestGroup.TestClass.scriptCustomDefBackBoxInit() {
                    model(
                        "codegen/testScripts",
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                        pattern = KT_OR_KTS,
                    )
                }

                testClass<AbstractLLScriptWithCustomDefBlackBoxTest> {
                    scriptCustomDefBackBoxInit()
                }

                testClass<AbstractLLReversedScriptWithCustomDefBlackBoxTest>() {
                    scriptCustomDefBackBoxInit()
                }
            }
        }

        testGroup(
            "analysis/low-level-api-fir/tests-gen",
            "compiler/testData/diagnostics",
        ) {
            fun TestGroup.TestClass.modelInitWasmJs() {
                model("wasmTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            fun TestGroup.TestClass.modelInitWasmWasi() {
                model("wasmWasiTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLWasmJsDiagnosticsTest>(suiteTestClassName = "LLWasmJsDiagnosticsFe10TestGenerated") {
                modelInitWasmJs()
            }

            testClass<AbstractLLWasmWasiDiagnosticsTest>(suiteTestClassName = "LLWasmWasiDiagnosticsFe10TestGenerated") {
                modelInitWasmWasi()
            }

            testClass<AbstractLLReversedWasmJsDiagnosticsTest>(suiteTestClassName = "LLReversedWasmJsDiagnosticsFe10TestGenerated") {
                modelInitWasmJs()
            }

            testClass<AbstractLLReversedWasmWasiDiagnosticsTest>(suiteTestClassName = "LLReversedWasmWasiDiagnosticsFe10TestGenerated") {
                modelInitWasmWasi()
            }
        }

        testGroup(
            "analysis/low-level-api-fir/tests-gen",
            "compiler/testData",
        ) {
            fun TestGroup.TestClass.modelInit() {
                model(
                    "diagnostics/testsWithJsStdLib",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    pattern = KT_OR_KTS,
                )
            }

            testClass<AbstractLLJsDiagnosticsTest>(suiteTestClassName = "LLJsDiagnosticsFe10TestGenerated") {
                modelInit()
            }

            testClass<AbstractLLReversedJsDiagnosticsTest>(suiteTestClassName = "LLReversedJsDiagnosticsFe10TestGenerated") {
                modelInit()
            }
        }

        testGroup(
            "analysis/low-level-api-fir/tests-gen",
            "compiler/testData",
        ) {
            fun TestGroup.TestClass.modelInit() {
                model(
                    "diagnostics/tests",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    pattern = KT_OR_KTS,
                )
                model(
                    "diagnostics/testsWithStdLib",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    excludeDirs = listOf("native"),
                    pattern = KT_OR_KTS,
                )
            }

            testClass<AbstractLLDiagnosticsTest>(suiteTestClassName = "LLDiagnosticsFe10TestGenerated") {
                modelInit()
            }

            testClass<AbstractLLReversedDiagnosticsTest>(suiteTestClassName = "LLReversedDiagnosticsFe10TestGenerated") {
                modelInit()
            }

            testClass<AbstractLLPartialDiagnosticsTest>(suiteTestClassName = "LLPartialDiagnosticsFe10TestGenerated") {
                modelInit()
            }

            testClass<AbstractLLBlackBoxTest> {
                model(
                    "codegen/box",
                    excludeDirs = listOf(
                        "script", // script is excluded until KT-60127 is implemented
                        "multiplatform/k1",
                    )
                )
            }

            testClass<AbstractLLReversedBlackBoxTest> {
                model(
                    "codegen/box",
                    excludeDirs = listOf(
                        "script", // script is excluded until KT-60127 is implemented
                        "multiplatform/k1",
                    )
                )
            }

            testClass<AbstractLLBlackBoxTest>(suiteTestClassName = "LLBlackBoxModernJdkTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractLLReversedBlackBoxTest>(suiteTestClassName = "LLReversedBlackBoxModernJdkTestGenerated") {
                model("codegen/boxModernJdk")
            }

            run {
                fun TestGroup.TestClass.scriptBlackBoxInit() {
                    model("codegen/script", pattern = TestGeneratorUtil.KTS)
                }

                testClass<AbstractLLScriptBlackBoxTest> {
                    scriptBlackBoxInit()
                }

                testClass<AbstractLLReversedScriptBlackBoxTest> {
                    scriptBlackBoxInit()
                }
            }
        }

        testGroup("analysis/low-level-api-fir/tests-gen", testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH) {
            fun TestGroup.TestClass.modelInit() {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
            }

            testClass<AbstractLLSpecTest> {
                modelInit()
            }

            testClass<AbstractLLReversedSpecTest> {
                modelInit()
            }
        }

        testGroup("analysis/low-level-api-fir/tests-gen", testDataRoot = "plugins/plugin-sandbox/testData") {
            testClass<AbstractLLSandboxBackBoxTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLReversedSandboxBackBoxTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLSandboxDiagnosticsTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLReversedSandboxDiagnosticsTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
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