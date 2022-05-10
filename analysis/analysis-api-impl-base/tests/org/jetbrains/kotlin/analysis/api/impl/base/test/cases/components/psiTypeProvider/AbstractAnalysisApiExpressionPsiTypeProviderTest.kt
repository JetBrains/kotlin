/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiExpressionPsiTypeProviderTest : AbstractAnalysisApiSingleFileTest(){
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declarationAtCaret = testServices.expressionMarkerProvider.getSelectedElement(ktFile) as KtExpression
        val actual = analyze(ktFile) {
            val returnType = declarationAtCaret.getKtType()
                ?: error("Not a typable expression ${declarationAtCaret::class} ${declarationAtCaret.text}")
            val psiType = returnType.asPsiType(declarationAtCaret)
            buildString {
                appendLine("KtType: ${returnType.render()}")
                appendLine("PsiType: $psiType")
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
