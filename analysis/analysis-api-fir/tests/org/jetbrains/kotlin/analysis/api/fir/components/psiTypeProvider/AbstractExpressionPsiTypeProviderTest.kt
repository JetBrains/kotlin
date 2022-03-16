/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.fir.FirFrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractHLApiSingleFileTest
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExpressionPsiTypeProviderTest : AbstractHLApiSingleFileTest(){
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declarationAtCaret = testServices.expressionMarkerProvider.getSelectedElement(ktFile) as KtExpression
        val actual = analyse(ktFile) {
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
