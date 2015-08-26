/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeUtils

public class RemoveExplicitSuperQualifierInspection : IntentionBasedInspection<JetSuperExpression>(RemoveExplicitSuperQualifierIntention()) {
    override val problemHighlightType: ProblemHighlightType
        get() = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

public class RemoveExplicitSuperQualifierIntention : JetSelfTargetingRangeIntention<JetSuperExpression>(javaClass(), "Remove explicit supertype qualification") {
    override fun applicabilityRange(element: JetSuperExpression): TextRange? {
        if (element.superTypeQualifier == null) return null

        val qualifiedExpression = element.getQualifiedExpressionForReceiver() ?: return null
        val selector = qualifiedExpression.selectorExpression ?: return null

        val bindingContext = selector.analyze(BodyResolveMode.PARTIAL)
        if (selector.getResolvedCall(bindingContext) == null) return null
        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, qualifiedExpression] ?: return null
        val dataFlowInfo = bindingContext.getDataFlowInfo(element)
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, qualifiedExpression] ?: TypeUtils.NO_EXPECTED_TYPE

        val newQualifiedExpression = JetPsiFactory(element).createExpressionByPattern("$0.$1", toNonQualified(element), selector) as JetQualifiedExpression
        val newBindingContext = newQualifiedExpression.analyzeInContext(resolutionScope, qualifiedExpression, dataFlowInfo = dataFlowInfo, expectedType = expectedType, isStatement = true)
        val newResolvedCall = newQualifiedExpression.selectorExpression.getResolvedCall(newBindingContext) ?: return null
        if (ErrorUtils.isError(newResolvedCall.resultingDescriptor)) return null

        return TextRange(element.instanceReference.endOffset, element.labelQualifier?.startOffset ?: element.endOffset)
    }

    override fun applyTo(element: JetSuperExpression, editor: Editor) {
        element.replace(toNonQualified(element))
    }

    private fun toNonQualified(superExpression: JetSuperExpression): JetSuperExpression {
        val factory = JetPsiFactory(superExpression)
        val labelName = superExpression.getLabelNameAsName()
        return (if (labelName != null)
            factory.createExpressionByPattern("super@$0", labelName)
        else
            factory.createExpression("super")) as JetSuperExpression
    }
}