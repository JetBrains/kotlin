/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.api.fir.components.importOptimizer.AbstractHLImportOptimizerTest
import org.jetbrains.kotlin.analysis.api.fir.components.psiTypeProvider.AbstractExpressionPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.fir.components.psiTypeProvider.AbstractPsiTypeProviderTest
import org.jetbrains.kotlin.analysis.api.fir.components.typeProvider.AbstractFirGetSuperTypesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.AbstractReferenceResolveTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.annotations.AbstractAnalysisApiAnnotationsOnDeclarationsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.annotations.AbstractAnalysisApiAnnotationsOnFilesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.annotations.AbstractAnalysisApiAnnotationsOnTypesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.compileTimeConstantProvider.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.diagnosticProvider.AbstractCollectDiagnosticsTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionInfoProvider.AbstractReturnTargetSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionInfoProvider.AbstractWhenMissingCasesTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionTypeProvider.AbstractExpectedExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.expressionTypeProvider.AbstractHLExpressionTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.smartCastProvider.AbstractHLSmartCastInfoTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.symbolDeclarationOverridesProvider.AbstractIsSubclassOfTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.symbolDeclarationOverridesProvider.AbstractOverriddenDeclarationProviderTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.symbolDeclarationRenderer.AbstractRendererTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.typeCreator.AbstractTypeParameterTypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.typeInfoProvider.AbstractFunctionClassKindTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.typeInfoProvider.AbstractIsDenotableTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.components.typeProvider.AbstractHasCommonSubtypeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.fir.AbstractResolveCallTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.scopes.AbstractDelegateMemberScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.scopes.AbstractFileScopeTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.scopes.AbstractMemberScopeByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.scopes.AbstractSubstitutionOverridesUnwrappingTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.symbols.AbstractSymbolByPsiTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.symbols.AbstractSymbolByReferenceTest
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.TestModuleKind
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.component
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.group
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.test
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun TestGroupSuite.generateAnalysisApiTests() {
    generateAnalysisApiComponentsTests()
    generateAnalysisApiNonComponentsTests()
}

private fun TestGroupSuite.generateAnalysisApiNonComponentsTests() {
    test(
        AbstractReferenceResolveTest::class,
        testModuleKinds = TestModuleKind.SOURCE_AND_LIBRARY_SOURCE,
        addFe10 = false,
    ) { moduleKind ->
        when (moduleKind) {
            TestModuleKind.LIBRARY_SOURCE -> {
                model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME, excludeDirsRecursively = listOf("withErrors"))
            }
            else -> {
                model("referenceResolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }
    }

    group("scopes") {
        test(
            AbstractSubstitutionOverridesUnwrappingTest::class,
            generateFe10 = false,
        ) {
            model("substitutionOverridesUnwrapping")
        }

        test(
            AbstractMemberScopeByFqNameTest::class,
            generateFe10 = false,
        ) {
            model("memberScopeByFqName")
        }

        test(
            AbstractFileScopeTest::class,
            generateFe10 = false,
        ) {
            model("fileScopeTest", extension = "kt")
        }

        test(
            AbstractDelegateMemberScopeTest::class,
            generateFe10 = false,
        ) {
            model("delegatedMemberScope")
        }
    }

    group("symbols") {
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
            generateFe10 = false // TODO "fails with Rewrite at slice ANNOTATION key"
        ) {
            model("annotationsOnFiles")
        }
    }
}


private fun TestGroupSuite.generateAnalysisApiComponentsTests() {
    component("callResolver") {
        test(AbstractResolveCallTest::class) {
            model("resolveCall")
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

    component("diagnosticsProvider") {
        test(AbstractCollectDiagnosticsTest::class) {
            model("diagnostics")
        }
    }

    component("importOptimizer") {
        test(
            AbstractHLImportOptimizerTest::class,
            generateFe10 = false,
        ) {
            model("analyseImports", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    component("psiTypeProvider") {
        test(AbstractPsiTypeProviderTest::class, generateFe10 = false) {
            model("psiType/forDeclaration")
        }

        test(AbstractExpressionPsiTypeProviderTest::class, generateFe10 = false) {
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
        test(AbstractFunctionClassKindTest::class, generateFe10 = false) {
            model("functionClassKind")
        }
        test(AbstractFirGetSuperTypesTest::class, generateFe10 = false) {
            model("superTypes")
        }
        test(AbstractIsDenotableTest::class) {
            model("isDenotable", excludedPattern = ".*\\.descriptors\\.kt$")
        }
    }

    component("typeProvider") {
        test(AbstractHasCommonSubtypeTest::class) {
            model("haveCommonSubtype")
        }
    }
}
