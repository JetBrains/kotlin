/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractMultiModuleResolveCallTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCallTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCandidatesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractCompilerFacilityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractMultiModuleCompilerFacilityTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByDelegatedMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByPsiTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderForSetterParameterTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCodeFragmentCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractDanglingFileCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractIsUsedAsExpressionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractReturnTargetSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractWhenMissingCasesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpectedExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractHLExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.importOptimizer.AbstractAnalysisApiImportOptimizerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.multiplatformInfoProvider.AbstractExpectForActualTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiExpressionPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiKtTypeByPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiPsiTypeProviderTest
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
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceImportAliasTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractAnalysisApiSubstitutorsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractBuiltInTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractTypeByDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.components.psiDeclarationProvider.AbstractPsiDeclarationProviderTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.*
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

internal fun AnalysisApiTestGroup.generateAnalysisApiTests() {
    test(
        AbstractReferenceResolveTest::class,
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource, TestModuleKind.LibrarySource) and
                analysisApiModeIs(AnalysisApiMode.Ide, AnalysisApiMode.Standalone),
    ) { data ->
        when (data.moduleKind) {
            TestModuleKind.LibrarySource -> {
                model(
                    "referenceResolve",
                    pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
                    excludeDirsRecursively = listOf("withErrors")
                )
            }

            TestModuleKind.ScriptSource -> model(data, "referenceResolve")

            else -> {
                model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }
    }

    test(
        AbstractDanglingFileReferenceResolveTest::class,
        filter = frontendIs(FrontendKind.Fir)
                and testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource)
    ) {
        model("danglingFileReferenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
    }

    component(
        "compilerFacility",
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource)
                and frontendIs(FrontendKind.Fir)
                and analysisSessionModeIs(AnalysisSessionMode.Normal)
                and analysisApiModeIs(AnalysisApiMode.Ide)
    ) {
        test(AbstractCompilerFacilityTest::class) {
            model("compilation", pattern = TestGeneratorUtil.KT)
        }

        test(AbstractMultiModuleCompilerFacilityTest::class, filter = testModuleKindIs(TestModuleKind.Source)) {
            model("compilationMultiModule", pattern = TestGeneratorUtil.KT)
        }
    }

    group(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource)) {
        generateAnalysisApiComponentsTests()
        generateAnalysisApiNonComponentsTests()
        generateResolveExtensionsTests()
    }
    group(
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.ScriptSource, TestModuleKind.LibraryBinary) and
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
        test(AbstractReferenceResolveWithResolveExtensionTest::class) {
            model(it, "referenceResolve")
        }
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiNonComponentsTests() {
    group("symbols", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractSymbolByPsiTest::class) {
            model(it, "symbolByPsi")
        }

        test(AbstractSymbolByJavaPsiTest::class, filter = frontendIs(FrontendKind.Fir)) {
            model(it, "symbolByJavaPsi")
        }

        test(AbstractSingleSymbolByPsiTest::class) {
            model(it, "singleSymbolByPsi")
        }

        test(AbstractSymbolRestoreFromDifferentModuleTest::class) {
            model(it, "symbolRestoreFromDifferentModule")
        }

        test(AbstractMultiModuleSymbolByPsiTest::class) {
            model(it, "multiModuleSymbolByPsi")
        }

        test(
            AbstractSymbolByFqNameTest::class
        ) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model(it, "symbolByFqName")
                AnalysisApiMode.Standalone ->
                    model(it, "symbolByFqName", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
            }
        }

        test(AbstractSymbolByReferenceTest::class) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model(it, "symbolByReference")
                AnalysisApiMode.Standalone ->
                    model(it, "symbolByReference", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
            }
        }
    }

    group("types", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractTypeByDeclarationReturnTypeTest::class) {
            model(it, "byDeclarationReturnType")
        }

        test(AbstractBuiltInTypeTest::class) {
            model(it, "builtins")
        }
    }

    group("annotations") {
        test(AbstractAnalysisApiAnnotationsOnTypesTest::class) {
            model(it, "annotationsOnTypes")
        }

        test(AbstractAnalysisApiAnnotationsOnDeclarationsTest::class) {
            model(it, "annotationsOnDeclaration")
        }

        test(AbstractAnalysisApiSpecificAnnotationOnDeclarationTest::class) {
            model(it, "specificAnnotations")
        }

        test(
            AbstractAnalysisApiAnnotationsOnFilesTest::class,
            filter = analysisSessionModeIs(AnalysisSessionMode.Normal),
        ) {
            model(it, "annotationsOnFiles")
        }

        test(AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest::class) {
            model(it, "metaAnnotations")
        }

    }

    group("imports", filter = frontendIs(FrontendKind.Fir)) {
        test(
            AbstractReferenceImportAliasTest::class,
            filter = analysisSessionModeIs(AnalysisSessionMode.Normal)
        ) {
            model(it, "importAliases")
        }
    }

    group("substitutors", filter = frontendIs(FrontendKind.Fir)) {
        test(AbstractAnalysisApiSubstitutorsTest::class) {
            model(it, "typeSubstitution")
        }
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiStandaloneTests() {
    group("standalone") {
        test(
            AbstractPsiDeclarationProviderTest::class,
            filter = testModuleKindIs(TestModuleKind.Source)
        ) {
            model(it, "source")
        }
        test(
            AbstractPsiDeclarationProviderTest::class,
            filter = testModuleKindIs(TestModuleKind.LibraryBinary)
        ) {
            model(it, "binary")
        }
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiComponentsTests() {
    component("callResolver", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractResolveCallTest::class) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model(it, "resolveCall")
                AnalysisApiMode.Standalone ->
                    model(it, "resolveCall", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
            }
        }

        test(
            AbstractResolveCandidatesTest::class
        ) {
            model(it, "resolveCandidates")
        }
    }

    component("multiModuleCallResolver", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractMultiModuleResolveCallTest::class, filter = frontendIs(FrontendKind.Fir)) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model(it, "resolveCall")
                AnalysisApiMode.Standalone ->
                    model(it, "resolveCall")
            }
        }
    }

    component("compileTimeConstantProvider") {
        test(AbstractCompileTimeConstantEvaluatorTest::class) {
            model(it, "evaluate")
        }
    }

    component("expressionInfoProvider") {
        test(AbstractWhenMissingCasesTest::class) {
            model(it, "whenMissingCases")
        }

        test(AbstractReturnTargetSymbolTest::class) {
            model(it, "returnExpressionTargetSymbol")
        }

        test(AbstractIsUsedAsExpressionTest::class) {
            model(it, "isUsedAsExpression")
        }
    }

    component("referenceShortener", filter = frontendIs(FrontendKind.Fir) and analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractReferenceShortenerTest::class) {
            model(it, "shortenRange")
        }

        test(AbstractReferenceShortenerForWholeFileTest::class) {
            model(it, "shortenWholeFile")
        }
    }

    component("expressionTypeProvider") {
        test(AbstractExpectedExpressionTypeTest::class) {
            model(it, "expectedExpressionType")
        }

        test(
            AbstractHLExpressionTypeTest::class
        ) {
            model(it, "expressionType")
        }

        test(
            AbstractDeclarationReturnTypeTest::class
        ) {
            model(it, "declarationReturnType")
        }
    }

    component("diagnosticsProvider", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractCollectDiagnosticsTest::class) {
            model(it, "diagnostics")
        }

        test(AbstractDanglingFileCollectDiagnosticsTest::class, filter = frontendIs(FrontendKind.Fir)) {
            model(it, "diagnostics")
        }

        test(
            AbstractCodeFragmentCollectDiagnosticsTest::class,
            filter = testModuleKindIs(TestModuleKind.Source) and frontendIs(FrontendKind.Fir),
        ) {
            model("codeFragmentDiagnostics", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    // for K1, symbols do not have a proper equality implementation, so the tests are failing
    component("containingDeclarationProvider", filter = frontendIs(FrontendKind.Fir)) {
        test(AbstractContainingDeclarationProviderByPsiTest::class) {
            model(it, "containingDeclarationByPsi")
        }

        test(AbstractContainingDeclarationProviderByMemberScopeTest::class) {
            model(it, "containingDeclarationFromMemberScope")
        }

        test(AbstractContainingDeclarationProviderForSetterParameterTest::class) {
            model(it, "containingDeclarationForSetterParameter")
        }

        test(AbstractContainingDeclarationProviderByDelegatedMemberScopeTest::class) {
            model(it, "containingDeclarationByDelegatedMemberScope")
        }
    }

    component("importOptimizer") {
        test(
            AbstractAnalysisApiImportOptimizerTest::class,
            filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir),
        ) {
            model(it, "analyseImports")
        }
    }

    component("multiplatformInfoProvider") {
        test(AbstractExpectForActualTest::class) {
            model(it, "expectForActual")
        }
    }

    component("psiTypeProvider") {
        test(AbstractAnalysisApiPsiTypeProviderTest::class) {
            model(it, "psiType/forDeclaration")
        }

        test(AbstractAnalysisApiExpressionPsiTypeProviderTest::class, filter = frontendIs(FrontendKind.Fir)) {
            model(it, "psiType/forExpression")
        }

        test(AbstractAnalysisApiKtTypeByPsiTypeProviderTest::class, filter = frontendIs(FrontendKind.Fir)) {
            model(it, "psiType/asKtType")
        }
    }

    component("resolveExtensionInfoProvider", filter = frontendIs(FrontendKind.Fir)) {
        test(AbstractResolveExtensionInfoProviderTest::class) {
            model(it, "extensionScopeWithPsi")
        }
    }

    component("smartCastProvider") {
        test(AbstractHLSmartCastInfoTest::class) {
            model(it, "smartCastInfo")
        }
    }

    component("symbolDeclarationOverridesProvider") {
        test(AbstractOverriddenDeclarationProviderTest::class) {
            model(it, "overriddenSymbols")
        }

        test(AbstractIsSubclassOfTest::class) {
            model(it, "isSubclassOf")
        }
    }

    component("symbolDeclarationRenderer") {
        test(AbstractRendererTest::class) {
            model(it, "renderDeclaration")
        }

        test(AbstractSymbolRenderingByReferenceTest::class, analysisApiModeIs(AnalysisApiMode.Ide, AnalysisApiMode.Standalone)) {
            model(it, "symbolRenderingByReference")
        }
    }

    component("symbolInfoProvider") {
        test(AbstractAnnotationApplicableTargetsTest::class) {
            model(it, "annotationApplicableTargets")
        }
    }

    component("typeCreator") {
        test(AbstractTypeParameterTypeTest::class) {
            model(it, "typeParameter")
        }

        test(AbstractBuildClassTypeTest::class, filter = analysisSessionModeIs(AnalysisSessionMode.Normal)/*no non-file context element*/) {
            model(it, "classType")
        }
    }

    component("typeInfoProvider") {
        test(AbstractFunctionClassKindTest::class) {
            model(it, "functionClassKind")
        }
        test(AbstractAnalysisApiGetSuperTypesTest::class, filter = frontendIs(FrontendKind.Fir)) {
            model(it, "superTypes")
        }
        test(AbstractDoubleColonReceiverTypeTest::class) {
            model(it, "doubleColonReceiverType")
        }
        test(AbstractIsDenotableTest::class) {
            model(it, "isDenotable", excludedPattern = ".*\\.descriptors\\.kt$")
        }
    }

    component("typeProvider") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test(AbstractHasCommonSubtypeTest::class) {
                model(it, "haveCommonSubtype")
            }
        }
        test(AbstractTypeReferenceTest::class) {
            model(it, "typeReference")
        }
    }

    component("signatureSubstitution") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir)) {
            test(AbstractAnalysisApiSymbolAsSignatureTest::class) {
                model(it, "symbolAsSignature")
            }

            test(AbstractAnalysisApiSymbolSubstitutionTest::class) {
                model(it, "symbolSubstitution")
            }

            test(AbstractAnalysisApiSignatureSubstitutionTest::class) {
                model(it, "signatureSubstitution")
            }

            test(AbstractAnalysisApiSignatureContractsTest::class) {
                model(it, "signatureContracts")
            }
        }
    }

    component("substitutorFactory") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir)) {
            test(AbstractSubstitutorBuilderTest::class) {
                model(it, "substitutorBuilder")
            }
        }
    }

    component("substitutorProvider", filter = frontendIs(FrontendKind.Fir)) {
        test(AbstractCreateInheritanceTypeSubstitutorTest::class) {
            model(it, "createInheritanceTypeSubstitutor")
        }
    }


    component("referenceResolveProvider") {
        test(AbstractIsImplicitCompanionReferenceTest::class) {
            model(it, "isImplicitReferenceToCompanion")
        }
    }

    component("scopeProvider") {
        group(filter = frontendIs(FrontendKind.Fir)) {
            test(AbstractTypeScopeTest::class) {
                model(it, "typeScope")
            }

            test(AbstractScopeContextForPositionTest::class) {
                model(it, "scopeContextForPosition")
            }

            test(AbstractFileImportingScopeContextTest::class) {
                model(it, "importingScopeContext")
            }
        }

        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test(
                AbstractSubstitutionOverridesUnwrappingTest::class,
                filter = frontendIs(FrontendKind.Fir),
            ) {
                model(it, "substitutionOverridesUnwrapping")
            }

            test(
                AbstractMemberScopeTest::class,
                filter = frontendIs(FrontendKind.Fir),
            ) {
                when (it.analysisApiMode) {
                    AnalysisApiMode.Ide ->
                        model(it, "memberScope")
                    AnalysisApiMode.Standalone ->
                        model(it, "memberScope", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
                }
            }

            test(
                AbstractStaticMemberScopeTest::class,
                filter = frontendIs(FrontendKind.Fir),
            ) {
                model(it, "staticMemberScope")
            }

            test(AbstractFileScopeTest::class) {
                model(it, "fileScopeTest")
            }

            test(AbstractDelegateMemberScopeTest::class) {
                model(it, "delegatedMemberScope")
            }

            test(
                AbstractDeclaredMemberScopeTest::class,
                filter = frontendIs(FrontendKind.Fir),
            ) {
                model(it, "declaredMemberScope")
            }

            test(
                AbstractStaticDeclaredMemberScopeTest::class,
                filter = frontendIs(FrontendKind.Fir),
            ) {
                model(it, "staticDeclaredMemberScope")
            }

            test(
                AbstractCombinedDeclaredMemberScopeTest::class,
                filter = frontendIs(FrontendKind.Fir),
            ) {
                model(it, "combinedDeclaredMemberScope")
            }
        }
    }
}
