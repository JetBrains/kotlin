/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiExpressionPsiTypeProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val declarationAtCaret = when (val element = testServices.expressionMarkerProvider.getSelectedElement(mainFile)) {
            is KtExpression -> element
            is KtValueArgument -> element.getArgumentExpression()!!
            else -> error("Unexpected element: $element of ${element::class}")
        }

        val containingDeclaration = declarationAtCaret.parentOfType<KtDeclaration>()
            ?: error("Can't find containing declaration for $declarationAtCaret")

        val containingClass = getContainingKtLightClass(containingDeclaration, mainFile)
        val psiContext = containingClass.findLightDeclarationContext(containingDeclaration)
            ?: error("Can't find psi context for $containingDeclaration")

        val actual = analyze(mainFile) {
            val returnType = declarationAtCaret.getKaType()
            if (returnType != null) {
                prettyPrint {
                    appendLine("${KaType::class.simpleName}: ${AnalysisApiPsiTypeProviderTestUtils.render(analysisSession, returnType)}")
                    for (allowErrorTypes in listOf(false, true)) {
                        for (typeMappingMode in KaTypeMappingMode.entries) {
                            for (isAnnotationMethod in listOf(false, true)) {
                                val psiType = returnType.asPsiType(psiContext, allowErrorTypes, typeMappingMode, isAnnotationMethod)
                                appendLine("asPsiType(allowErrorTypes=$allowErrorTypes, mode=$typeMappingMode, isAnnotationMethod=$isAnnotationMethod):")
                                withIndent {
                                    appendLine("PsiType: ${AnalysisApiPsiTypeProviderTestUtils.render(psiType)}")
                                }
                                appendLine()
                            }
                        }
                    }
                }
            } else {
                "null"
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
