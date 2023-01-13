/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiExpressionPsiTypeProviderTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declarationAtCaret = testServices.expressionMarkerProvider.getSelectedElement(ktFile) as KtExpression
        val containingDeclaration = declarationAtCaret.parentOfType<KtDeclaration>()
            ?: error("Can't find containing declaration for $declarationAtCaret")
        val containingClass = getContainingKtLightClass(containingDeclaration, ktFile)
        val psiContext = containingClass.findLightDeclarationContext(containingDeclaration)
            ?: error("Can't find psi context for $containingDeclaration")
        val actual = analyze(ktFile) {
            val returnType = declarationAtCaret.getKtType()
                ?: error("Not a typable expression ${declarationAtCaret::class} ${declarationAtCaret.text}")
            val psiType = returnType.asPsiType(psiContext, allowErrorTypes = false)
            buildString {
                appendLine("KtType: ${returnType.render(position = Variance.INVARIANT)}")
                appendLine("PsiType: $psiType")
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
