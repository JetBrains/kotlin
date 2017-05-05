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

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils

class RemoveExplicitSuperQualifierInspection : IntentionBasedInspection<KtSuperExpression>(
        RemoveExplicitSuperQualifierIntention::class
), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtSuperExpression): ProblemHighlightType =
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveExplicitSuperQualifierIntention : SelfTargetingRangeIntention<KtSuperExpression>(KtSuperExpression::class.java, "Remove explicit supertype qualification") {
    override fun applicabilityRange(element: KtSuperExpression): TextRange? {
        if (element.superTypeQualifier == null) return null

        val qualifiedExpression = element.getQualifiedExpressionForReceiver() ?: return null
        val selector = qualifiedExpression.selectorExpression ?: return null

        val bindingContext = selector.analyze(BodyResolveMode.PARTIAL)
        if (selector.getResolvedCall(bindingContext) == null) return null

        val newQualifiedExpression = KtPsiFactory(element).createExpressionByPattern("$0.$1", toNonQualified(element), selector) as KtQualifiedExpression
        val newBindingContext = newQualifiedExpression.analyzeAsReplacement(qualifiedExpression, bindingContext)
        val newResolvedCall = newQualifiedExpression.selectorExpression.getResolvedCall(newBindingContext) ?: return null
        if (ErrorUtils.isError(newResolvedCall.resultingDescriptor)) return null

        return TextRange(element.instanceReference.endOffset, element.labelQualifier?.startOffset ?: element.endOffset)
    }

    override fun applyTo(element: KtSuperExpression, editor: Editor?) {
        element.replace(toNonQualified(element))
    }

    private fun toNonQualified(superExpression: KtSuperExpression): KtSuperExpression {
        val factory = KtPsiFactory(superExpression)
        val labelName = superExpression.getLabelNameAsName()
        return (if (labelName != null)
            factory.createExpressionByPattern("super@$0", labelName)
        else
            factory.createExpression("super")) as KtSuperExpression
    }
}