/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCallTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCandidatesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractCompilerFacilityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractFirPluginPrototypeCompilerFacilityTestWithAnalysis
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractFirPluginPrototypeMultiModuleCompilerFacilityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByDelegatedMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByPsiTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByReferenceTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderForSetterParameterTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingModuleByFileTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider.AbstractExitPointSnapshotTest
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
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.klibSourceFileProvider.AbstractGetKlibSourceFileNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.multiplatformInfoProvider.AbstractExpectForActualTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiExpressionPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiKtTypeByPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.readWriteAccess.AbstractReadWriteAccessTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.referenceResolveProvider.AbstractIsImplicitCompanionReferenceTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolveExtensionInfoProvider.AbstractResolveExtensionInfoProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSignatureContractsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSignatureSubstitutionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSymbolAsSignatureTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSymbolSubstitutionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.smartCastProvider.AbstractHLSmartCastInfoTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substitutorProvider.AbstractCreateInheritanceTypeSubstitutorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substututorFactory.AbstractSubstitutorBuilderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.subtyping.AbstractLenientSubtypingTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.subtyping.AbstractLenientTypeEqualityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.subtyping.AbstractSubtypingTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.subtyping.AbstractTypeEqualityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractIsSubclassOfTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractOverriddenDeclarationProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractRendererTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractSymbolRenderingByReferenceTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider.AbstractAnnotationApplicableTargetsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractBuildClassTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractTypeParameterTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractDoubleColonReceiverTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractFunctionClassKindTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractIsDenotableTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.AbstractAnalysisApiGetSuperTypesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.AbstractHasCommonSubtypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.AbstractTypeReferenceTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.visibilityChecker.AbstractVisibilityCheckerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session.AbstractModuleStateModificationAnalysisSessionInvalidationTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractAbbreviatedTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractAnalysisApiSubstitutorsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractBuiltInTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractTypeByDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.components.psiDeclarationProvider.AbstractPsiDeclarationProviderTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.*
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

internal fun AnalysisApiTestGroup.generateAnalysisApiTests() {
    component(
        directory = "resolver",
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource, TestModuleKind.LibrarySource) and
                analysisSessionModeIs(AnalysisSessionMode.Normal),
    ) {
        val init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit = { data ->
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

                        // Tests which rely on missing dependencies (e.g. the main module missing a dependency to a library module) will not
                        // work as expected with library source modules, because they use "rest symbol providers" which provide symbols from
                        // all other libraries (as dependencies between libraries are not usually known). So the "missing" dependencies
                        // would effectively not be missing.
                        add("missingDependency")
                    }

                    else -> {}
                }
            }

            model(data, "singleByPsi", excludeDirsRecursively = excludeDirs)
        }

        test<AbstractResolveCallTest>(init = init)
        test<AbstractResolveCandidatesTest>(init = init)
        test<AbstractReferenceResolveTest>(init = init)
    }

    test<AbstractDanglingFileReferenceResolveTest>(
        filter = frontendIs(FrontendKind.Fir)
                and testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource)
    ) {
        model("danglingFileReferenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
    }

    component(
        "compilerFacility",
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource, TestModuleKind.LibraryBinary)
                and frontendIs(FrontendKind.Fir)
                and analysisSessionModeIs(AnalysisSessionMode.Normal)
                and analysisApiModeIs(AnalysisApiMode.Ide)
    ) {
        test<AbstractCompilerFacilityTest>(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource)) {
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

    generateAnalysisApiNonComponentsTests()

    group(
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource, TestModuleKind.LibraryBinaryDecompiled) and
                analysisApiModeIs(AnalysisApiMode.Standalone)
    ) {
        generateAnalysisApiStandaloneTests()
    }
}

private fun AnalysisApiTestGroup.generateResolveExtensionsTests() {
    group(
        "resolveExtensions",
        filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and
                frontendIs(FrontendKind.Fir) and
                testModuleKindIs(TestModuleKind.Source)
    ) {
        test<AbstractReferenceResolveWithResolveExtensionTest> {
            model(it, "referenceResolve")
        }
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiNonComponentsTests() {
    group(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource)) {
        group("symbols", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test<AbstractSymbolByPsiTest> {
                model(it, "symbolByPsi")
            }

            test<AbstractSymbolByJavaPsiTest>(filter = frontendIs(FrontendKind.Fir)) {
                model(it, "symbolByJavaPsi")
            }

            test<AbstractSingleSymbolByPsiTest> {
                model(it, "singleSymbolByPsi")
            }

            test<AbstractSymbolRestoreFromDifferentModuleTest> {
                model(it, "symbolRestoreFromDifferentModule")
            }

            test<AbstractMultiModuleSymbolByPsiTest> {
                model(it, "multiModuleSymbolByPsi")
            }

            test<AbstractSymbolByFqNameTest> {
                when (it.analysisApiMode) {
                    AnalysisApiMode.Ide ->
                        model(it, "symbolByFqName")
                    AnalysisApiMode.Standalone ->
                        model(it, "symbolByFqName", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
                }
            }

            test<AbstractSymbolByReferenceTest> {
                when (it.analysisApiMode) {
                    AnalysisApiMode.Ide ->
                        model(it, "symbolByReference")
                    AnalysisApiMode.Standalone ->
                        model(it, "symbolByReference", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
                }
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
        }

        // Skip `TestModuleKind.Source` due to `KT-65038`.
        group(filter = frontendIs(FrontendKind.Fir) and testModuleKindIs(TestModuleKind.LibraryBinary)) {
            test<AbstractAbbreviatedTypeTest> {
                model(it, "abbreviatedType")
            }
        }
    }

    // We don't test Standalone API analysis session invalidation because it doesn't support modification (yet). The test infrastructure
    // registers an "always accessible" lifetime token, which is at odds with checking the validity of an analysis session after
    // invalidation.
    group(
        "sessions",
        filter = frontendIs(FrontendKind.Fir)
                and testModuleKindIs(TestModuleKind.Source)
                and analysisSessionModeIs(AnalysisSessionMode.Normal)
                and analysisApiModeIs(AnalysisApiMode.Ide)
    ) {
        test<AbstractModuleStateModificationAnalysisSessionInvalidationTest> {
            model("sessionInvalidation")
        }

        test<AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest> {
            model("sessionInvalidation")
        }

        test<AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest> {
            model("sessionInvalidation")
        }

        test<AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest> {
            model("sessionInvalidation")
        }

        test<AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest> {
            model("sessionInvalidation")
        }

        test<AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest> {
            model("sessionInvalidation")
        }
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiStandaloneTests() {
    group("standalone") {
        test<AbstractPsiDeclarationProviderTest>(
            filter = testModuleKindIs(TestModuleKind.Source)
        ) {
            model(it, "source")
        }

        test<AbstractPsiDeclarationProviderTest>(
            filter = testModuleKindIs(TestModuleKind.LibraryBinaryDecompiled)
        ) {
            model(it, "binary")
        }
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiComponentsTests() {
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

        test<AbstractContainingModuleByFileTest> {
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

    component("multiplatformInfoProvider") {
        test<AbstractExpectForActualTest> {
            model(it, "expectForActual")
        }
    }

    component("psiTypeProvider") {
        test<AbstractAnalysisApiPsiTypeProviderTest> {
            model(it, "psiType/forDeclaration")
        }

        test<AbstractAnalysisApiExpressionPsiTypeProviderTest> {
            model(it, "psiType/forExpression")
        }

        test<AbstractAnalysisApiKtTypeByPsiTypeProviderTest>(filter = frontendIs(FrontendKind.Fir)) {
            model(it, "psiType/asKtType")
        }
    }

    component("resolveExtensionInfoProvider", filter = frontendIs(FrontendKind.Fir)) {
        test<AbstractResolveExtensionInfoProviderTest> {
            model(it, "extensionScopeWithPsi")
        }
    }

    component("smartCastProvider") {
        test<AbstractHLSmartCastInfoTest> {
            model(it, "smartCastInfo")
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
    }

    component("typeCreator") {
        test<AbstractTypeParameterTypeTest> {
            model(it, "typeParameter")
        }

        test<AbstractBuildClassTypeTest>(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)/*no non-file context element*/) {
            model(it, "classType")
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

    component("subtyping") {
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
    }

    component("referenceResolveProvider") {
        test<AbstractIsImplicitCompanionReferenceTest> {
            model(it, "isImplicitReferenceToCompanion")
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

            group(filter = frontendIs(FrontendKind.Fir)) {
                test<AbstractSubstitutionOverridesUnwrappingTest> {
                    model(it, "substitutionOverridesUnwrapping")
                }

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
