/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnDeclarationsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnFilesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnTypesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractMultiModuleResolveCallTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCallTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCandidatesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByDelegatedMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByPsiTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractIsUsedAsExpressionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractReturnTargetSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractWhenMissingCasesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpectedExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractHLExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.importOptimizer.AbstractAnalysisApiImportOptimizerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.multiplatformInfoProvider.AbstractExpectForActualTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiExpressionPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSignatureContractsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSignatureSubstitutionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSymbolAsSignatureTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSymbolSubstitutionTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.smartCastProvider.AbstractHLSmartCastInfoTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substututorFactory.AbstractSubstitutorBuilderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractIsSubclassOfTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractOverriddenDeclarationProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractRendererTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolInfoProvider.AbstractAnnotationApplicableTargetsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractBuildClassTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractTypeParameterTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractFunctionClassKindTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractIsDenotableTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.AbstractAnalysisApiGetSuperTypesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.AbstractHasCommonSubtypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceResolveTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceShortenerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.scopes.*
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSingleSymbolByPsi
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByPsiTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByReferenceTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.AbstractAnalysisApiSubstitutorsTest
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
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource) and
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

            else -> {
                model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }
    }

    group(filter = testModuleKindIs(TestModuleKind.Source)) {
        generateAnalysisApiComponentsTests()
        generateAnalysisApiNonComponentsTests()
    }
}

private fun AnalysisApiTestGroup.generateAnalysisApiNonComponentsTests() {
    group("scopes", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(
            AbstractSubstitutionOverridesUnwrappingTest::class,
            filter = frontendIs(FrontendKind.Fir),
        ) {
            model("substitutionOverridesUnwrapping")
        }

        test(
            AbstractMemberScopeByFqNameTest::class,
            filter = frontendIs(FrontendKind.Fir),
        ) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model("memberScopeByFqName")
                AnalysisApiMode.Standalone ->
                    model("memberScopeByFqName", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
            }
        }

        test(AbstractFileScopeTest::class) {
            model("fileScopeTest", extension = "kt")
        }

        test(AbstractDelegateMemberScopeTest::class) {
            model("delegatedMemberScope")
        }
    }

    group("symbols", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractSymbolByPsiTest::class) {
            model("symbolByPsi")
        }

        test(AbstractSingleSymbolByPsi::class) {
            model("singleSymbolByPsi")
        }

        test(
            AbstractSymbolByFqNameTest::class
        ) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model("symbolByFqName")
                AnalysisApiMode.Standalone ->
                    model("symbolByFqName", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
            }
        }

        test(AbstractSymbolByReferenceTest::class) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model("symbolByReference")
                AnalysisApiMode.Standalone ->
                    model("symbolByReference", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
            }
        }
    }

    group("types", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractTypeByDeclarationReturnTypeTest::class) {
            model("byDeclarationReturnType")
        }
    }

    group("annotations") {
        test(AbstractAnalysisApiAnnotationsOnTypesTest::class) {
            model("annotationsOnTypes")
        }

        test(AbstractAnalysisApiAnnotationsOnDeclarationsTest::class) {
            model("annotationsOnDeclaration")
        }

        test(
            AbstractAnalysisApiAnnotationsOnFilesTest::class,
            filter = analysisSessionModeIs(AnalysisSessionMode.Normal),
        ) {
            model("annotationsOnFiles")
        }

        test(AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest::class) {
            model("metaAnnotations")
        }

    }

    group("substitutors", filter = frontendIs(FrontendKind.Fir)) {
        test(AbstractAnalysisApiSubstitutorsTest::class) {
            model("typeSubstitution")
        }
    }

    group("standalone", filter = analysisApiModeIs(AnalysisApiMode.Standalone)) {
        test(AbstractPsiDeclarationProviderTest::class) {
            model("singleModule")
        }
    }
}


private fun AnalysisApiTestGroup.generateAnalysisApiComponentsTests() {
    component("callResolver", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractResolveCallTest::class) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model("resolveCall")
                AnalysisApiMode.Standalone ->
                    model("resolveCall", excludeDirsRecursively = listOf("withTestCompilerPluginEnabled"))
            }
        }

        test(
            AbstractResolveCandidatesTest::class
        ) {
            model("resolveCandidates")
        }
    }

    component("multiModuleCallResolver", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractMultiModuleResolveCallTest::class, filter = frontendIs(FrontendKind.Fir)) {
            when (it.analysisApiMode) {
                AnalysisApiMode.Ide ->
                    model("resolveCall")
                AnalysisApiMode.Standalone ->
                    model("resolveCall")
            }
        }
    }

    component("compileTimeConstantProvider") {
        test(AbstractCompileTimeConstantEvaluatorTest::class) {
            model("evaluate")
        }
    }

    component("expressionInfoProvider") {
        test(AbstractWhenMissingCasesTest::class) {
            model("whenMissingCases")
        }

        test(AbstractReturnTargetSymbolTest::class) {
            model("returnExpressionTargetSymbol")
        }

        test(AbstractIsUsedAsExpressionTest::class) {
            model("isUsedAsExpression")
        }
    }

    component("referenceShortener", filter = frontendIs(FrontendKind.Fir) and analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractReferenceShortenerTest::class) {
            model("referenceShortener")
        }
    }

    component("expressionTypeProvider") {
        test(AbstractExpectedExpressionTypeTest::class) {
            model("expectedExpressionType")
        }

        test(
            AbstractHLExpressionTypeTest::class
        ) {
            model("expressionType")
        }

        test(
            AbstractDeclarationReturnTypeTest::class
        ) {
            model("declarationReturnType")
        }
    }

    component("diagnosticsProvider", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractCollectDiagnosticsTest::class) {
            model("diagnostics")
        }
    }

    // for K1, symbols do not have a proper equality implementation, so the tests are failing
    component("containingDeclarationProvider", filter = frontendIs(FrontendKind.Fir)) {
        test(AbstractContainingDeclarationProviderByPsiTest::class) {
            model("containingDeclarationByPsi")
        }

        test(AbstractContainingDeclarationProviderByMemberScopeTest::class) {
            model("containingDeclarationFromMemberScope")
        }

        test(AbstractContainingDeclarationProviderByDelegatedMemberScopeTest::class) {
            model("containingDeclarationByDelegatedMemberScope")
        }
    }

    component("importOptimizer") {
        test(
            AbstractAnalysisApiImportOptimizerTest::class,
            filter = frontendIs(FrontendKind.Fir) and analysisSessionModeIs(AnalysisSessionMode.Normal),
        ) {
            model("analyseImports", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    component("multiplatformInfoProvider") {
        test(AbstractExpectForActualTest::class) {
            model("expectForActual")
        }
    }

    component("psiTypeProvider") {
        test(AbstractAnalysisApiPsiTypeProviderTest::class) {
            model("psiType/forDeclaration")
        }

        test(AbstractAnalysisApiExpressionPsiTypeProviderTest::class, filter = frontendIs(FrontendKind.Fir)) {
            model("psiType/forExpression")
        }
    }

    component("smartCastProvider") {
        test(AbstractHLSmartCastInfoTest::class) {
            model("smartCastInfo")
        }
    }

    component("symbolDeclarationOverridesProvider") {
        test(AbstractOverriddenDeclarationProviderTest::class) {
            model("overriddenSymbols")
        }

        test(AbstractIsSubclassOfTest::class) {
            model("isSubclassOf")
        }
    }

    component("symbolDeclarationRenderer") {
        test(AbstractRendererTest::class) {
            model("renderDeclaration")
        }
    }

    component("symbolInfoProvider") {
        test(AbstractAnnotationApplicableTargetsTest::class) {
            model("annotationApplicableTargets")
        }
    }

    component("typeCreator") {
        test(AbstractTypeParameterTypeTest::class) {
            model("typeParameter")
        }

        test(AbstractBuildClassTypeTest::class, filter = analysisSessionModeIs(AnalysisSessionMode.Normal)/*no non-file context element*/) {
            model("classType")
        }
    }

    component("typeInfoProvider") {
        test(AbstractFunctionClassKindTest::class) {
            model("functionClassKind")
        }
        test(AbstractAnalysisApiGetSuperTypesTest::class, filter = frontendIs(FrontendKind.Fir)) {
            model("superTypes")
        }
        test(AbstractIsDenotableTest::class) {
            model("isDenotable", excludedPattern = ".*\\.descriptors\\.kt$")
        }
    }

    component("typeProvider") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test(AbstractHasCommonSubtypeTest::class) {
                model("haveCommonSubtype")
            }
        }
    }

    component("signatureSubstitution") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir)) {
            test(AbstractAnalysisApiSymbolAsSignatureTest::class) {
                model("symbolAsSignature")
            }

            test(AbstractAnalysisApiSymbolSubstitutionTest::class) {
                model("symbolSubstitution")
            }

            test(AbstractAnalysisApiSignatureSubstitutionTest::class) {
                model("signatureSubstitution")
            }

            test(AbstractAnalysisApiSignatureContractsTest::class) {
                model("signatureContracts")
            }
        }
    }

    component("substitutorFactory") {
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and frontendIs(FrontendKind.Fir)) {
            test(AbstractSubstitutorBuilderTest::class) {
                model("substitutorBuilder")
            }
        }
    }

    component("scopeProvider") {
        group(filter = frontendIs(FrontendKind.Fir)) {
            test(AbstractTypeScopeTest::class) {
                model("typeScope")
            }
        }
    }
}
