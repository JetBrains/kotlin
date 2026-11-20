/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.components.compileTimeConstantProvider

import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics
import org.jetbrains.kotlin.analysis.api.evaluation.evaluate
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirEvaluator
import org.jetbrains.kotlin.analysis.api.fir.components.evaluateByPsi
import org.jetbrains.kotlin.analysis.api.fir.test.cases.components.renderFrontendIndependentKClassNameOf
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompileTimeConstantEvaluatorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val markerProvider = testServices.expressionMarkerProvider
        val elementToPreresolve = markerProvider.getBottommostElementOfTypeAtCaretOrNull<KtElement>(mainFile, "preresolve")
        if (elementToPreresolve != null) {
            copyAwareAnalyzeForTest(elementToPreresolve) {
                // Trigger resolution implicitly
                it.diagnostics().toList()
            }
        }

        val element = markerProvider.getBottommostSelectedElementOfTypeByDirective(mainFile, mainModule)
        val expression = when (element) {
            is KtExpression -> element
            is KtValueArgument -> element.getArgumentExpression()
            else -> null
        } ?: testServices.assertions.fail { "Unsupported expression: $element" }

        val evaluation = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(expression) { contextExpression ->
                val firEvaluator = (useSiteSession as KaFirSession).evaluator as KaFirEvaluator
                Evaluation(
                    constantValue = contextExpression.evaluate(),
                    firConstantValue = with(firEvaluator) { contextExpression.evaluateByFir() },
                    isEvaluatedByPsi = contextExpression.evaluateByPsi() != null,
                )
            }
        }

        val renderedConstantValue = renderConstantValue(evaluation.constantValue)
        val actual = buildString {
            append("expression: ${expression.text}")
            if (evaluation.isEvaluatedByPsi) {
                append(" (by PSI)")
            }

            appendLine()
            append(renderedConstantValue)
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)

        // The evaluation may skip FIR for literals, so check that it agrees with the FIR-based one
        testServices.assertions.assertEquals(expected = renderConstantValue(evaluation.firConstantValue), actual = renderedConstantValue) {
            "The evaluation of `${expression.text}` must be the same as the FIR-based one"
        }

        testServices.assertions.assertTrue(evaluation.constantValue?.sourcePsi === evaluation.firConstantValue?.sourcePsi) {
            "The source of the constant `${expression.text}` must be the same as the FIR-based one"
        }
    }
}

private class Evaluation(
    val constantValue: KaConstantValue?,
    val firConstantValue: KaConstantValue?,
    val isEvaluatedByPsi: Boolean,
)

private fun renderConstantValue(constantValue: KaConstantValue?): String = buildString {
    appendLine("constant: ${constantValue?.render() ?: "NOT_EVALUATED"}")
    appendLine("constantValueKind: ${constantValue?.let { renderFrontendIndependentKClassNameOf(it) } ?: "NOT_EVALUATED"}")
    appendLine("constantValueSource: ${constantValue?.sourcePsi ?: "NOT_EVALUATED"}")
    appendLine("constantValueSourceText: ${constantValue?.sourcePsi?.text ?: "NOT_EVALUATED"}")
}
