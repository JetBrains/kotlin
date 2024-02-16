/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiExpressionPsiTypeProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
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
            val returnType = declarationAtCaret.getKtType()
            if (returnType != null) {
                prettyPrint {
                    appendLine("KtType: ${returnType.render(position = Variance.INVARIANT)}")
                    for (allowErrorTypes in listOf(false, true)) {
                        for (typeMappingMode in KtTypeMappingMode.entries) {
                            for (isAnnotationMethod in listOf(false, true)) {
                                val psiType = returnType.asPsiType(psiContext, allowErrorTypes, typeMappingMode, isAnnotationMethod)
                                appendLine("asPsiType(allowErrorTypes=$allowErrorTypes, mode=$typeMappingMode, isAnnotationMethod=$isAnnotationMethod):")
                                withIndent {
                                    appendLine(psiType.toString())
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
