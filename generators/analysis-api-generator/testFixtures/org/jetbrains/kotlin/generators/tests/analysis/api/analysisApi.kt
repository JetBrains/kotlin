/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.fir.test.cases.imports.AbstractKaDefaultImportsProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.analysisScopeProvider.AbstractCanBeAnalysedTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractCompilerFacilityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractFirPluginPrototypeCompilerFacilityTestWithAnalysis
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractFirPluginPrototypeMultiModuleCompilerFacilityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerPluginGeneratedDeclarationsProvider.AbstractCompilerPluginGeneratedDeclarationsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider.AbstractExitPointSnapshotTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider.AbstractHLSmartCastInfoTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCodeFragmentCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractDanglingFileCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractElementDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractIsUsedAsExpressionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractReturnTargetSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractWhenMissingCasesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpectedExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractHLExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.importOptimizer.AbstractAnalysisApiImportOptimizerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.inheritorsProvider.AbstractDanglingFileSealedInheritorsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.inheritorsProvider.AbstractSealedInheritorsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.AbstractDeclarationTypeAsPsiTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.AbstractExpressionTypeAsPsiTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.AbstractPsiTypeAsKaTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.klibSourceFileProvider.AbstractGetKlibSourceFileNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.readWriteAccess.AbstractReadWriteAccessTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.referenceResolveProvider.AbstractIsImplicitCompanionReferenceTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider.AbstractGetExpectsForActualTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider.AbstractOriginalConstructorIfTypeAliasedTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolveExtensionInfoProvider.AbstractResolveExtensionInfoProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSignatureContractsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSignatureSubstitutionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSymbolAsSignatureTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSymbolSubstitutionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substitutorProvider.AbstractCreateInheritanceTypeSubstitutorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substututorFactory.AbstractSubstitutorBuilderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractIsSubclassOfTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractOverriddenDeclarationProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractRendererTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractSymbolRenderingByReferenceTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider.AbstractAnnotationApplicableTargetsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider.AbstractCanBeOperatorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider.AbstractSamClassBySamConstructorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractBuildArrayTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractBuildClassTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractTypeParameterTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractDoubleColonReceiverTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractFunctionClassKindTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractIsDenotableTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeRelationChecker.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.visibilityChecker.AbstractVisibilityCheckerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractIsReferenceToTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceImportAliasTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceShortenerForWholeFileTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceShortenerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisExceptionWrappingTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisRejectionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.*
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.*
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

internal fun AnalysisApiTestGroup.generateAnalysisApiTests() {
    component(
        directory = "resolver",
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource, TestModuleKind.LibrarySource) and
                analysisSessionModeIs(AnalysisSessionMode.Normal),
    ) {
        val singleByPsiInit: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit = { data ->
            val excludeDirs = buildList {
                if (data.analysisApiMode == AnalysisApiMode.Standalone ||
                    data.frontend == FrontendKind.Fe10 ||
                    data.moduleKind == TestModuleKind.LibrarySource
                ) {
                    add("withTestCompilerPluginEnabled")
                }

                when (data.moduleKind) {
                    TestModuleKind.LibrarySource -> {
                        // Sources with errors cannot be compiled to a library.
                        add("withErrors")

                        // Main modules in tests which rely on missing dependencies (e.g. the main module missing a dependency to a library
                        // module) cannot be compiled to a library.
                        add("missingDependency")

                        // For some platforms, `Cloneable` is unresolved and the test cannot be compiled to a library. We could place these
                        // tests under `withErrors`, but it'd split the test data, which is undesirable.
                        add("cloneable")
                    }

                    else -> {}
                }
            }

            model(data, "singleByPsi", excludeDirsRecursively = excludeDirs)
        }

        test<AbstractResolveCallTest>(init = singleByPsiInit)
        test<AbstractResolveCandidatesTest>(init = singleByPsiInit)
        test<AbstractResolveReferenceTest>(init = singleByPsiInit)

        group(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource)) {
            val allByPsiInit: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit = { data ->
                model(data, "allByPsi")
            }

            test<AbstractResolveCallByFileTest>(init = allByPsiInit)
            test<AbstractResolveCandidatesByFileTest>(init = allByPsiInit)
            test<AbstractResolveReferenceByFileTest>(init = allByPsiInit)
        }
    }

    group(filter = frontendIs(FrontendKind.Fir) and testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource)) {
        test<AbstractPhysicalResolveDanglingFileReferenceTest> {
            model("danglingFileReferenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        test<AbstractNonPhysicalResolveDanglingFileReferenceTest> {
            model("danglingFileReferenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    component(
        "compilerFacility",
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource, TestModuleKind.LibraryBinary)
                and frontendIs(FrontendKind.Fir)
                and analysisSessionModeIs(AnalysisSessionMode.Normal)
                and analysisApiModeIs(AnalysisApiMode.Ide)
    ) {
        test<AbstractCompilerFacilityTest>(filter = testModuleKindIs(TestModuleKind.LibrarySource)) {
            model("compilation", pattern = TestGeneratorUtil.KT, excludeDirs = listOf("codeFragments/reifiedTypeParams"))
        }

        test<AbstractCompilerFacilityTest>(filter = testModuleKindIs(TestModuleKind.Source)) {
            model("compilation", pattern = TestGeneratorUtil.KT)
        }

        test<AbstractFirPluginPrototypeMultiModuleCompilerFacilityTest>(filter = testModuleKindIs(TestModuleKind.Source)) {
            model("firPluginPrototypeMultiModule", pattern = TestGeneratorUtil.KT)
        }

        test<AbstractFirPluginPrototypeCompilerFacilityTestWithAnalysis>(filter = testModuleKindIs(TestModuleKind.Source)) {
            model("bugsFromRealComposeApps", pattern = TestGeneratorUtil.KT)
        }
    }

    group(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource)) {
        generateAnalysisApiComponentsTests()
        generateResolveExtensionsTests()
    }

    component(
        "compilerPluginGeneratedDeclarationsProvider",
        filter = frontendIs(FrontendKind.Fir) and
                testModuleKindIs(TestModuleKind.Source) and
                analysisSessionModeIs(AnalysisSessionMode.Normal) and
                analysisApiModeIs(AnalysisApiMode.Ide)
    ) {
        test<AbstractCompilerPluginGeneratedDeclarationsTest> {
            model(it, "compilerPluginGeneratedDeclarations")
        }
    }


    generateAnalysisApiNonComponentsTests()
}

private fun AnalysisApiTestGroup.generateResolveExtensionsTests() {
    group(
        "resolveExtensions",
        filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and
                frontendIs(FrontendKind.Fir) and
                testModuleKindIs(TestModuleKind.Source)
    ) {
        test<AbstractResolveReferenceWithResolveExtensionTest> {
            model(it, "referenceResolve")
        }
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiNonComponentsTests() {
    group(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource)) {
        group("symbols", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            fun TestGroup.TestClass.symbolsModel(data: AnalysisApiTestConfiguratorFactoryData, path: String) {
                if (data.analysisApiMode == AnalysisApiMode.Standalone || data.frontend == FrontendKind.Fe10) {
                    model(data, path, excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
                } else {
                    model(data, path)
                }
            }

            test<AbstractSymbolByPsiTest> {
                symbolsModel(it, "symbolByPsi")
            }

            test<AbstractSymbolByJavaPsiTest>(filter = frontendIs(FrontendKind.Fir)) {
                symbolsModel(it, "symbolByJavaPsi")
            }

            test<AbstractSingleSymbolByPsiTest> {
                symbolsModel(it, "singleSymbolByPsi")
            }

            test<AbstractSymbolRestoreFromDifferentModuleTest> {
                symbolsModel(it, "symbolRestoreFromDifferentModule")
            }

            test<AbstractSymbolByFqNameTest> {
                symbolsModel(it, "symbolByFqName")
            }

            test<AbstractSymbolByReferenceTest> {
                symbolsModel(it, "symbolByReference")
            }
        }

        group("annotations") {
            test<AbstractAnalysisApiAnnotationsOnTypesTest> {
                model(it, "annotationsOnTypes")
            }

            test<AbstractAnalysisApiAnnotationsOnDeclarationsTest> {
                model(it, "annotationsOnDeclaration")
            }

            test<AbstractAnalysisApiSpecificAnnotationOnDeclarationTest> {
                model(it, "specificAnnotations")
            }

            test<AbstractAnalysisApiAnnotationsOnFilesTest>(
                filter = analysisSessionModeIs(AnalysisSessionMode.Normal),
            ) {
                model(it, "annotationsOnFiles")
            }

            test<AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest> {
                model(it, "metaAnnotations")
            }

        }

        group("imports", filter = frontendIs(FrontendKind.Fir)) {
            test<AbstractReferenceImportAliasTest>(
                filter = analysisSessionModeIs(AnalysisSessionMode.Normal)
            ) {
                model(it, "importAliases")
            }
        }

        group("references") {
            test<AbstractIsReferenceToTest>(filter = frontendIs(FrontendKind.Fir)) {
                model(it, "isReferenceTo")
            }
        }

        group("restrictedAnalysis", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test<AbstractRestrictedAnalysisExceptionWrappingTest> {
                model(it, "exceptionWrapping")
            }

            test<AbstractRestrictedAnalysisRejectionTest> {
                model(it, "restriction")
            }
        }

        group("substitutors", filter = frontendIs(FrontendKind.Fir)) {
            test<AbstractAnalysisApiSubstitutorsTest> {
                model(it, "typeSubstitution")
            }
        }
    }

    group("types", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        group(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource)) {
            test<AbstractTypeByDeclarationReturnTypeTest> {
                model(it, "byDeclarationReturnType")
            }

            test<AbstractBuiltInTypeTest> {
                model(it, "builtins")
            }

            group("typePointers", filter = frontendIs(FrontendKind.Fir)) {
                test<AbstractTypePointerConsistencyTest> {
                    model(it, "consistency")
                }
            }
        }

        group(filter = frontendIs(FrontendKind.Fir) and testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibraryBinary)) {
            test<AbstractAbbreviatedTypeTest> {
                model(it, "abbreviatedType")
            }
        }
    }

    group("sessions", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test<AbstractUseSiteLibraryModuleAnalysisRejectionTest>(filter = testModuleKindIs(TestModuleKind.LibraryBinary)) {
            model(it, "allowUseSiteLibraryModuleAnalysis")
        }

        // We don't test Standalone API analysis session invalidation because it doesn't support modification (yet). The test infrastructure
        // registers an "always accessible" lifetime token, which is at odds with checking the validity of an analysis session after
        // invalidation.
        group(
            filter = frontendIs(FrontendKind.Fir) and testModuleKindIs(TestModuleKind.Source) and analysisApiModeIs(AnalysisApiMode.Ide),
        ) {
            test<AbstractModuleStateModificationAnalysisSessionInvalidationTest> {
                model("sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest> {
                model("sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest> {
                model("sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest> {
                model("sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest> {
                model("sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest> {
                model("sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }
        }
    }

    group("imports") {
        test<AbstractKaDefaultImportsProviderTest>(
            filter = analysisSessionModeIs(AnalysisSessionMode.Normal)
                    and testModuleKindIs(TestModuleKind.Source)
                    and frontendIs(FrontendKind.Fir),
        ) {
            model("defaultImportProvider")
        }
    }

}

private fun AnalysisApiTestGroup.generateAnalysisApiComponentsTests() {
    component("analysisScopeProvider", filter = frontendIs(FrontendKind.Fir) and analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test<AbstractCanBeAnalysedTest> {
            model(it, "canBeAnalysed")
        }
    }

    component("compileTimeConstantProvider") {
        test<AbstractCompileTimeConstantEvaluatorTest> {
            model(it, "evaluate")
        }
    }

    component("expressionInfoProvider") {
        test<AbstractWhenMissingCasesTest> {
            model(it, "whenMissingCases")
        }

        test<AbstractReturnTargetSymbolTest> {
            model(it, "returnExpressionTargetSymbol")
        }

        test<AbstractIsUsedAsExpressionTest> {
            model(it, "isUsedAsExpression")
        }

        test<AbstractReadWriteAccessTest> {
            model(it, "readWriteAccess")
        }
    }

    component("referenceShortener", filter = frontendIs(FrontendKind.Fir) and analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test<AbstractReferenceShortenerTest> {
            model(it, "shortenRange")
        }

        test<AbstractReferenceShortenerForWholeFileTest> {
            model(it, "shortenWholeFile")
        }
    }

    component("expressionTypeProvider") {
        test<AbstractExpectedExpressionTypeTest> {
            model(it, "expectedExpressionType")
        }

        test<AbstractHLExpressionTypeTest> {
            model(it, "expressionType")
        }

        test<AbstractDeclarationReturnTypeTest> {
            model(it, "declarationReturnType")
        }
    }

    component("diagnosticsProvider", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test<AbstractCollectDiagnosticsTest> {
            model(it, "diagnostics")
        }

        test<AbstractDanglingFileCollectDiagnosticsTest>(filter = frontendIs(FrontendKind.Fir)) {
            model(it, "diagnostics")
        }

        test<AbstractElementDiagnosticsTest> {
            model(it, "elementDiagnostics")
        }

        test<AbstractCodeFragmentCollectDiagnosticsTest>(
            filter = testModuleKindIs(TestModuleKind.Source) and frontendIs(FrontendKind.Fir),
        ) {
            model("codeFragmentDiagnostics", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    // for K1, symbols do not have a proper equality implementation, so the tests are failing
    component("containingDeclarationProvider", filter = frontendIs(FrontendKind.Fir)) {
        test<AbstractContainingDeclarationProviderByPsiTest> {
            model(it, "containingDeclarationByPsi")
        }

        test<AbstractContainingDeclarationProviderByReferenceTest> {
            model(it, "containingDeclarationByReference")
        }

        test<AbstractContainingDeclarationProviderByMemberScopeTest> {
            model(it, "containingDeclarationFromMemberScope")
        }

        test<AbstractContainingDeclarationProviderForSetterParameterTest> {
            model(it, "containingDeclarationForSetterParameter")
        }

        test<AbstractContainingDeclarationProviderByDelegatedMemberScopeTest> {
            model(it, "containingDeclarationByDelegatedMemberScope")
        }

        // The containing module of a file in dependent analysis is always the dangling file module, so there's no sense in generating
        // dependent analysis tests here.
        test<AbstractContainingModuleByFileTest>(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            model(it, "containingModuleByFile")
        }
    }

    component("importOptimizer") {
        test<AbstractAnalysisApiImportOptimizerTest>(
            filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir),
        ) {
            model(it, "analyseImports")
        }
    }

    component("inheritorsProvider", filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir)) {
        test<AbstractSealedInheritorsTest> {
            model(it, "sealedInheritors")
        }

        test<AbstractDanglingFileSealedInheritorsTest> {
            model(it, "sealedInheritors")
        }
    }

    component("javaInteroperabilityComponent") {
        test<AbstractDeclarationTypeAsPsiTypeTest> {
            model(it, "asPsiType/forDeclaration")
        }

        test<AbstractExpressionTypeAsPsiTypeTest> {
            model(it, "asPsiType/forExpression")
        }

        test<AbstractPsiTypeAsKaTypeTest>(filter = frontendIs(FrontendKind.Fir)) {
            model(it, "asKaType")
        }
    }

    component("resolveExtensionInfoProvider", filter = frontendIs(FrontendKind.Fir)) {
        test<AbstractResolveExtensionInfoProviderTest> {
            model(it, "extensionScopeWithPsi")
        }
    }

    component("symbolDeclarationOverridesProvider") {
        test<AbstractOverriddenDeclarationProviderTest> {
            model(it, "overriddenSymbols")
        }

        test<AbstractIsSubclassOfTest> {
            model(it, "isSubclassOf")
        }
    }

    component("symbolDeclarationRenderer") {
        test<AbstractRendererTest> {
            model(it, "renderDeclaration")
        }

        test<AbstractSymbolRenderingByReferenceTest>(analysisApiModeIs(AnalysisApiMode.Ide, AnalysisApiMode.Standalone)) {
            model(it, "symbolRenderingByReference")
        }
    }

    component("symbolInfoProvider") {
        test<AbstractAnnotationApplicableTargetsTest> {
            model(it, "annotationApplicableTargets")
        }

        test<AbstractSamClassBySamConstructorTest> {
            model(it, "samClassBySamConstructor")
        }

        test<AbstractCanBeOperatorTest> {
            model(it, "canBeOperator")
        }
    }

    component("typeCreator") {
        test<AbstractTypeParameterTypeTest> {
            model(it, "typeParameter")
        }

        test<AbstractBuildClassTypeTest> {
            model(it, "classType")
        }

        test<AbstractBuildArrayTypeTest> {
            model(it, "arrayType")
        }
    }

    component("typeInfoProvider") {
        test<AbstractFunctionClassKindTest> {
            model(it, "functionClassKind")
        }
        test<AbstractAnalysisApiGetSuperTypesTest>(filter = frontendIs(FrontendKind.Fir)) {
            model(it, "superTypes")
        }
        test<AbstractDoubleColonReceiverTypeTest> {
            model(it, "doubleColonReceiverType")
        }
        test<AbstractIsDenotableTest> {
            model(it, "isDenotable", excludedPattern = ".*\\.descriptors\\.kt$")
        }
    }

    component("typeProvider") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test<AbstractHasCommonSubtypeTest> {
                model(it, "haveCommonSubtype")
            }
        }

        test<AbstractTypeReferenceTest> {
            model(it, "typeReference")
        }

        test<AbstractDefaultTypeTest> {
            model(it, "defaultType")
        }

        test<AbstractVarargArrayTypeTest> {
            model(it, "varargArrayType")
        }
    }

    component("signatureSubstitution") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir)) {
            test<AbstractAnalysisApiSymbolAsSignatureTest> {
                model(it, "symbolAsSignature")
            }

            test<AbstractAnalysisApiSymbolSubstitutionTest> {
                model(it, "symbolSubstitution")
            }

            test<AbstractAnalysisApiSignatureSubstitutionTest> {
                model(it, "signatureSubstitution")
            }

            test<AbstractAnalysisApiSignatureContractsTest> {
                model(it, "signatureContracts")
            }
        }
    }

    component("substitutorFactory") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir)) {
            test<AbstractSubstitutorBuilderTest> {
                model(it, "substitutorBuilder")
            }
        }
    }

    component("substitutorProvider", filter = frontendIs(FrontendKind.Fir)) {
        test<AbstractCreateInheritanceTypeSubstitutorTest> {
            model(it, "createInheritanceTypeSubstitutor")
        }
    }

    component("typeRelationChecker") {
        test<AbstractTypeEqualityTest> {
            model(it, "subtypingAndEquality")
        }

        test<AbstractLenientTypeEqualityTest> {
            model(it, "subtypingAndEquality")
        }

        test<AbstractSubtypingTest> {
            model(it, "subtypingAndEquality")
        }

        test<AbstractLenientSubtypingTest> {
            model(it, "subtypingAndEquality")
        }

        test<AbstractNonLenientClassIdSubtypingTypeRelationTest> {
            model(it, "subtypingAndEquality")
        }

        test<AbstractLenientClassIdSubtypingTypeRelationTest> {
            model(it, "subtypingAndEquality")
        }

        test<AbstractNonLenientClassSymbolSubtypingTypeRelationTest> {
            model(it, "subtypingAndEquality")
        }

        test<AbstractLenientClassSymbolSubtypingTypeRelationTest> {
            model(it, "subtypingAndEquality")
        }
    }

    component("referenceResolveProvider") {
        test<AbstractIsImplicitCompanionReferenceTest> {
            model(it, "isImplicitReferenceToCompanion")
        }
    }

    component("relationProvider") {
        test<AbstractOriginalConstructorIfTypeAliasedTest>(
            filter = frontendIs(FrontendKind.Fir),
        ) {
            model(it, "originalConstructorIfTypeAliased")
        }

        test<AbstractGetExpectsForActualTest> {
            model(it, "getExpectsForActual")
        }
    }

    component("scopeProvider") {
        group(filter = frontendIs(FrontendKind.Fir)) {
            test<AbstractTypeScopeTest> {
                model(it, "typeScope")
            }

            test<AbstractScopeContextForPositionTest> {
                model(it, "scopeContextForPosition")
            }

            test<AbstractFileImportingScopeContextTest> {
                model(it, "importingScopeContext")
            }
        }

        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test<AbstractFileScopeTest> {
                model(it, "fileScopeTest")
            }

            test<AbstractDelegateMemberScopeTest> {
                model(it, "delegatedMemberScope")
            }

            test<AbstractPackageScopeTest> {
                model(it, "packageScope")
            }

            test<AbstractSubstitutionOverridesUnwrappingTest> {
                model(it, "substitutionOverridesUnwrapping")
            }

            group(filter = frontendIs(FrontendKind.Fir)) {
                test<AbstractMemberScopeTest> {
                    when (it.analysisApiMode) {
                        AnalysisApiMode.Ide ->
                            model(it, "memberScope")
                        AnalysisApiMode.Standalone ->
                            model(it, "memberScope", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
                    }
                }

                test<AbstractStaticMemberScopeTest> {
                    model(it, "staticMemberScope")
                }

                test<AbstractDeclaredMemberScopeTest> {
                    model(it, "declaredMemberScope")
                }

                test<AbstractStaticDeclaredMemberScopeTest> {
                    model(it, "staticDeclaredMemberScope")
                }

                test<AbstractCombinedDeclaredMemberScopeTest> {
                    model(it, "combinedDeclaredMemberScope")
                }
            }
        }
    }

    component("dataFlowInfoProvider") {
        test<AbstractHLSmartCastInfoTest> {
            model(it, "smartCastInfo")
        }

        test<AbstractExitPointSnapshotTest>(filter = frontendIs(FrontendKind.Fir)) {
            model(it, "exitPointSnapshot")
        }
    }

    component("klibSourceFileNameProvider", filter = frontendIs(FrontendKind.Fir) and analysisApiModeIs(AnalysisApiMode.Standalone)) {
        test<AbstractGetKlibSourceFileNameTest> {
            model(it, "getKlibSourceFileName")
        }
    }

    component("visibilityChecker", filter = frontendIs(FrontendKind.Fir)) {
        test<AbstractVisibilityCheckerTest> {
            model(it, "visibility")
        }
    }
}
