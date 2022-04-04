/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.importOptimizer.AbstractAnalysisApiImportOptimizerTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiExpressionPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AbstractAnalysisApiPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.AbstractAnalysisApiGetSuperTypesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceResolveTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnDeclarationsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnFilesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnTypesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCallTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveCandidatesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractReturnTargetSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractWhenMissingCasesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpectedExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractHLExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.smartCastProvider.AbstractHLSmartCastInfoTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractIsSubclassOfTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider.AbstractOverriddenDeclarationProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractRendererTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator.AbstractTypeParameterTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractFunctionClassKindTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractIsDenotableTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider.AbstractHasCommonSubtypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.scopes.AbstractDelegateMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.scopes.AbstractFileScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.scopes.AbstractMemberScopeByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.scopes.AbstractSubstitutionOverridesUnwrappingTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByPsiTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByReferenceTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.*
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.AnalysisApiTestGroup
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.component
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

internal fun AnalysisApiTestGroup.generateAnalysisApiTests() {
    test(
        AbstractReferenceResolveTest::class,
        filter = frontendIs(FrontendKind.Fir) and
                testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource) and
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
            model("memberScopeByFqName")
        }

        test(
            AbstractFileScopeTest::class,
            filter = frontendIs(FrontendKind.Fir),
        ) {
            model("fileScopeTest", extension = "kt")
        }

        test(
            AbstractDelegateMemberScopeTest::class,
            filter = frontendIs(FrontendKind.Fir),
        ) {
            model("delegatedMemberScope")
        }
    }

    group("symbols", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractSymbolByPsiTest::class) {
            model("symbolByPsi")
        }

        test(
            AbstractSymbolByFqNameTest::class
        ) {
            model("symbolByFqName")
        }

        test(AbstractSymbolByReferenceTest::class) {
            model("symbolByReference")
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
            filter = frontendIs(FrontendKind.Fir) and analysisSessionModeIs(AnalysisSessionMode.Normal), // TODO "fe10 fails with Rewrite at slice ANNOTATION key"
        ) {
            model("annotationsOnFiles")
        }
    }
}


private fun AnalysisApiTestGroup.generateAnalysisApiComponentsTests() {
    component("callResolver", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        test(AbstractResolveCallTest::class) {
            model("resolveCall")
        }
        test(
            AbstractResolveCandidatesTest::class
        ) {
            model("resolveCandidates")
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

    component("importOptimizer") {
        test(
            AbstractAnalysisApiImportOptimizerTest::class,
            filter = frontendIs(FrontendKind.Fir) and analysisSessionModeIs(AnalysisSessionMode.Normal),
        ) {
            model("analyseImports", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    component("psiTypeProvider") {
        test(AbstractAnalysisApiPsiTypeProviderTest::class, filter = frontendIs(FrontendKind.Fir)) {
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

    component("typeCreator") {
        test(AbstractTypeParameterTypeTest::class) {
            model("typeParameter")
        }
    }

    component("typeInfoProvider") {
        test(AbstractFunctionClassKindTest::class, filter = frontendIs(FrontendKind.Fir)) {
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
}
