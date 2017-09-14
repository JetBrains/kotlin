/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiConstantEvaluationHelper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.impl.ConstantExpressionEvaluator
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotationForSourceEntry
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator as FrontendConstantExpressionEvaluator

class KotlinLightConstantExpressionEvaluator : ConstantExpressionEvaluator {
    private fun evalConstantValue(constantValue: ConstantValue<*>): Any? {
        return if (constantValue is ArrayValue) {
            val items = constantValue.value.map { evalConstantValue(it) }
            items.singleOrNull() ?: items
        }
        else constantValue.value
    }

    override fun computeConstantExpression(expression: PsiElement, throwExceptionOnOverflow: Boolean): Any? {
        return computeExpression(expression, throwExceptionOnOverflow, null)
    }

    override fun computeExpression(
            expression: PsiElement,
            throwExceptionOnOverflow: Boolean,
            auxEvaluator: PsiConstantEvaluationHelper.AuxEvaluator?
    ): Any? {
        if (expression !is KtLightAnnotationForSourceEntry.LightExpressionValue<*>) return null
        val expressionToCompute = expression.originalExpression ?: return null
        return when (expressionToCompute) {
            is KtExpression -> {
                val resolutionFacade = expressionToCompute.getResolutionFacade()
                val evaluator = FrontendConstantExpressionEvaluator(resolutionFacade.moduleDescriptor.builtIns,
                                                                    expressionToCompute.languageVersionSettings)
                val evaluatorTrace = DelegatingBindingTrace(resolutionFacade.analyze(expressionToCompute), "Evaluating annotation argument")

                val constant = evaluator.evaluateExpression(expressionToCompute, evaluatorTrace) ?: return null
                if (constant.isError) return null
                evalConstantValue(constant.toConstantValue(TypeUtils.NO_EXPECTED_TYPE))
            }

            is PsiExpression -> {
                JavaPsiFacade.getInstance(expressionToCompute.project)
                        .constantEvaluationHelper
                        .computeExpression(expressionToCompute, throwExceptionOnOverflow, auxEvaluator)
            }

            else -> null
        }
    }
}