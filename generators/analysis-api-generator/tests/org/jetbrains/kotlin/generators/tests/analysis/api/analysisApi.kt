/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.descriptors.test.annotations.AbstractAnalysisApiFe10AnnotationsOnDeclarationsTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.annotations.AbstractAnalysisApiFe10AnnotationsOnTypesTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.compileTimeConstantProvider.AbstractKtFe10CompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.expressionInfoProvider.AbstractKtFe10ReturnTargetSymbolTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.expressionInfoProvider.AbstractKtFe10WhenMissingCasesTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.expressionTypeProvider.AbstractKtFe10ExpectedExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.expressionTypeProvider.AbstractKtFe10HLExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.smartCastProvider.AbstractKtFe10HLSmartCastInfoTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.symbolDeclarationOverridesProvider.AbstractKtFe10OverriddenDeclarationProviderTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.symbolDeclarationRenderer.AbstractKtFe10RendererTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.typeCreator.AbstractKtFe10TypeParameterTypeTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.typeProvider.AbstractKtFe10HasCommonSubtypeTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.expressionTypeProvider.AbstractKtFe10DeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.scopes.AbstractKtFe10SubstitutionOverridesUnwrappingTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.symbols.AbstractKtFe10SymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.symbols.AbstractKtFe10SymbolByPsiTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.symbols.AbstractKtFe10SymbolByReferenceTest
import org.jetbrains.kotlin.analysis.api.fir.AbstractFirReferenceResolveTest
import org.jetbrains.kotlin.analysis.api.fir.annotations.AbstractAnalysisApiFirAnnotationsOnDeclarationsTest
import org.jetbrains.kotlin.analysis.api.fir.annotations.AbstractAnalysisApiFirAnnotationsOnFilesTest
import org.jetbrains.kotlin.analysis.api.fir.annotations.AbstractAnalysisApiFirAnnotationsOnTypesTest
import org.jetbrains.kotlin.analysis.api.fir.components.callResolver.AbstractFirResolveCallTest
import org.jetbrains.kotlin.analysis.api.fir.components.compileTimeConstantProvider.AbstractFirCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.fir.components.expressionInfoProvider.AbstractFirReturnTargetSymbolTest
import org.jetbrains.kotlin.analysis.api.fir.components.expressionInfoProvider.AbstractFirWhenMissingCasesTest
import org.jetbrains.kotlin.analysis.api.fir.components.expressionTypeProvider.AbstractFirDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.fir.components.expressionTypeProvider.AbstractFirExpectedExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.fir.components.expressionTypeProvider.AbstractFirHLExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.fir.components.importOptimizer.AbstractHLImportOptimizerTest
import org.jetbrains.kotlin.analysis.api.fir.components.psiTypeProvider.AbstractExpressionPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.fir.components.psiTypeProvider.AbstractPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.fir.components.smartCastProvider.AbstractFirHLSmartCastInfoTest
import org.jetbrains.kotlin.analysis.api.fir.components.symbolDeclarationOverridesProvider.AbstractFirOverriddenDeclarationProviderTest
import org.jetbrains.kotlin.analysis.api.fir.components.symbolDeclarationRenderer.AbstractFirRendererTest
import org.jetbrains.kotlin.analysis.api.fir.components.typeCreator.AbstractFirTypeParameterTypeTest
import org.jetbrains.kotlin.analysis.api.fir.components.typeInfoProvider.AbstractFirFunctionClassKindTest
import org.jetbrains.kotlin.analysis.api.fir.components.typeProvider.AbstractFirGetSuperTypesTest
import org.jetbrains.kotlin.analysis.api.fir.components.typeProvider.AbstractFirHasCommonSubtypeTest
import org.jetbrains.kotlin.analysis.api.fir.scopes.AbstractFirDelegateMemberScopeTest
import org.jetbrains.kotlin.analysis.api.fir.scopes.AbstractFirFileScopeTest
import org.jetbrains.kotlin.analysis.api.fir.scopes.AbstractFirMemberScopeByFqNameTest
import org.jetbrains.kotlin.analysis.api.fir.scopes.AbstractFirSubstitutionOverridesUnwrappingTest
import org.jetbrains.kotlin.analysis.api.fir.symbols.AbstractFirSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.fir.symbols.AbstractFirSymbolByPsiTest
import org.jetbrains.kotlin.analysis.api.fir.symbols.AbstractFirSymbolByReferenceTest
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun TestGroupSuite.generateAnalysisApiTests() {
    generateAnalysisApiComponentsTests()
    generateAnalysisApiNonComponentsTests()
}

private fun TestGroupSuite.generateAnalysisApiNonComponentsTests() {
    test(
        fir = AbstractFirReferenceResolveTest::class, fe10 = null,
    ) {
        model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
    }

    group("scopes") {
        test(
            fir = AbstractFirSubstitutionOverridesUnwrappingTest::class,
            fe10 = AbstractKtFe10SubstitutionOverridesUnwrappingTest::class,
        ) {
            model("substitutionOverridesUnwrapping")
        }

        test(
            fir = AbstractFirMemberScopeByFqNameTest::class,
            fe10 = null,
        ) {
            model("memberScopeByFqName")
        }

        test(
            fir = AbstractFirFileScopeTest::class,
            fe10 = null,
        ) {
            model("fileScopeTest", extension = "kt")
        }

        test(
            fir = AbstractFirDelegateMemberScopeTest::class,
            fe10 = null,
        ) {
            model("delegatedMemberScope")
        }
    }

    group("symbols") {
        test(
            fir = AbstractFirSymbolByPsiTest::class,
            fe10 = AbstractKtFe10SymbolByPsiTest::class,
        ) {
            model("symbolByPsi")
        }

        test(
            fir = AbstractFirSymbolByFqNameTest::class,
            fe10 = AbstractKtFe10SymbolByFqNameTest::class,
        ) {
            model("symbolByFqName")
        }

        test(
            fir = AbstractFirSymbolByReferenceTest::class,
            fe10 = AbstractKtFe10SymbolByReferenceTest::class,
        ) {
            model("symbolByReference")
        }
    }

    group("annotations") {
        test(
            fir = AbstractAnalysisApiFirAnnotationsOnTypesTest::class,
            fe10 = AbstractAnalysisApiFe10AnnotationsOnTypesTest::class
        ) {
            model("annotationsOnTypes")
        }

        test(
            fir = AbstractAnalysisApiFirAnnotationsOnDeclarationsTest::class,
            fe10 = AbstractAnalysisApiFe10AnnotationsOnDeclarationsTest::class,
        ) {
            model("annotationsOnDeclaration")
        }

        test(
            fir = AbstractAnalysisApiFirAnnotationsOnFilesTest::class,
            fe10 = null // TODO "fails with Rewrite at slice ANNOTATION key"
            /*AbstractAnalysisApiFE10AnnotationsOnFilesTest*/
        ) {
            model("annotationsOnFiles")
        }
    }
}


private fun TestGroupSuite.generateAnalysisApiComponentsTests() {
    component("callResolver") {
        test(
            fir = AbstractFirResolveCallTest::class,
            // TODO: re-enable after KtFe10CallResolver is properly implemented
            fe10 = null // AbstractKtFe10ResolveCallTest::class,
        ) {
            model("resolveCall")
        }
    }

    component("compileTimeConstantProvider") {
        test(
            fir = AbstractFirCompileTimeConstantEvaluatorTest::class, fe10 = AbstractKtFe10CompileTimeConstantEvaluatorTest::class,
        ) {
            model("evaluate")
        }
    }

    component("expressionInfoProvider") {
        test(
            fir = AbstractFirWhenMissingCasesTest::class, fe10 = AbstractKtFe10WhenMissingCasesTest::class
        ) {
            model("whenMissingCases")
        }

        test(
            fir = AbstractFirReturnTargetSymbolTest::class, fe10 = AbstractKtFe10ReturnTargetSymbolTest::class
        ) {
            model("returnExpressionTargetSymbol")
        }
    }

    component("expressionTypeProvider") {
        test(
            fir = AbstractFirExpectedExpressionTypeTest::class, fe10 = AbstractKtFe10ExpectedExpressionTypeTest::class
        ) {
            model("expectedExpressionType")
        }

        test(
            fir = AbstractFirHLExpressionTypeTest::class, fe10 = AbstractKtFe10HLExpressionTypeTest::class
        ) {
            model("expressionType")
        }

        test(
            fir = AbstractFirDeclarationReturnTypeTest::class, fe10 = AbstractKtFe10DeclarationReturnTypeTest::class
        ) {
            model("declarationReturnType")
        }
    }

    component("importOptimizer") {
        test(
            fir = AbstractHLImportOptimizerTest::class,
            fe10 = null,
        ) {
            model("analyseImports", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    component("psiTypeProvider") {
        test(fir = AbstractPsiTypeProviderTest::class, fe10 = null) {
            model("psiType/forDeclaration")
        }

        test(fir = AbstractExpressionPsiTypeProviderTest::class, fe10 = null) {
            model("psiType/forExpression")
        }
    }

    component("smartCastProvider") {
        test(fir = AbstractFirHLSmartCastInfoTest::class, fe10 = AbstractKtFe10HLSmartCastInfoTest::class) {
            model("smartCastInfo")
        }
    }

    component("symbolDeclarationOverridesProvider") {
        test(fir = AbstractFirOverriddenDeclarationProviderTest::class, fe10 = AbstractKtFe10OverriddenDeclarationProviderTest::class) {
            model("overriddenSymbols")
        }
    }

    component("symbolDeclarationRenderer") {
        test(fir = AbstractFirRendererTest::class, fe10 = AbstractKtFe10RendererTest::class) {
            model("renderDeclaration")
        }
    }

    component("typeCreator") {
        test(fir = AbstractFirTypeParameterTypeTest::class, fe10 = AbstractKtFe10TypeParameterTypeTest::class) {
            model("typeParameter")
        }
    }

    component("typeInfoProvider") {
        test(fir = AbstractFirFunctionClassKindTest::class, fe10 = null) {
            model("functionClassKind")
        }
        test(fir = AbstractFirGetSuperTypesTest::class, fe10 = null) {
            model("superTypes")
        }
    }

    component("typeProvider") {
        test(fir = AbstractFirHasCommonSubtypeTest::class, fe10 = AbstractKtFe10HasCommonSubtypeTest::class) {
            model("haveCommonSubtype")
        }
    }
}
