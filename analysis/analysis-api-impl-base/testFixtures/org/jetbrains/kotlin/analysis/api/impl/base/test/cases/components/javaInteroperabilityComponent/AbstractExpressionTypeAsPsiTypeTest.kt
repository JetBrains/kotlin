/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.render
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExpressionTypeAsPsiTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val declarationAtCaret = when (val element = testServices.expressionMarkerProvider.getTopmostSelectedElement(contextFile)) {
                is KtExpression -> element
                is KtValueArgument -> element.getArgumentExpression()!!
                else -> error("Unexpected element: $element of ${element::class}")
            }

            val containingDeclaration = declarationAtCaret.parentOfType<KtDeclaration>()
                ?: error("Can't find containing declaration for $declarationAtCaret")

            val containingClass = getContainingKtLightClass(containingDeclaration, contextFile)
            val psiContext = containingClass.findLightDeclarationContext(containingDeclaration)
                ?: error("Can't find psi context for $containingDeclaration")

            val expressionType = declarationAtCaret.expressionType
            if (expressionType != null) {
                renderActual(expressionType, psiContext)
            } else {
                "null"
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

private fun KaSession.renderActual(expressionType: KaType, psiContext: KtLightElement<*, *>): String = prettyPrint {
    appendLine("${KaType::class.simpleName}: ${expressionType.render(useSiteSession)}")
    for (allowErrorTypes in listOf(false, true)) {
        for (typeMappingMode in KaTypeMappingMode.entries) {
            for (isAnnotationMethod in listOf(false, true)) {
                val psiType = expressionType.asPsiType(psiContext, allowErrorTypes, typeMappingMode, isAnnotationMethod)
                appendLine("asPsiType(allowErrorTypes=$allowErrorTypes, mode=$typeMappingMode, isAnnotationMethod=$isAnnotationMethod):")
                withIndent {
                    appendLine("PsiType: ${psiType?.render()}")
                }
                appendLine()
            }
        }
    }
}
