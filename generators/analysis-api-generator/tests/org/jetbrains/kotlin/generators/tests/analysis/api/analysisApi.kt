/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.descriptors.test.AbstractKtFe10ResolveCallTest
import org.jetbrains.kotlin.analysis.api.descriptors.test.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.test.symbols.AbstractKtFe10CompileTimeConstantEvaluatorTest
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
}


private fun TestGroupSuite.generateAnalysisApiComponentsTests() {
    component("callResolver") {
        test(
            fir = AbstractFirResolveCallTest::class, fe10 = AbstractKtFe10ResolveCallTest::class,
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
            fir = null /*TODO*/, fe10 = null
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
