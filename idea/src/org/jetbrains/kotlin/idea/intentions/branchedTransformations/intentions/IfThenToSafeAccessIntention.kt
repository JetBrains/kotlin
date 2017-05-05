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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*

class IfThenToSafeAccessInspection : IntentionBasedInspection<KtIfExpression>(IfThenToSafeAccessIntention::class) {
    override fun inspectionTarget(element: KtIfExpression) = element.ifKeyword

    override fun problemHighlightType(element: KtIfExpression): ProblemHighlightType =
            if (element.shouldBeTransformed()) ProblemHighlightType.WEAK_WARNING else ProblemHighlightType.INFORMATION
}

class IfThenToSafeAccessIntention : SelfTargetingOffsetIndependentIntention<KtIfExpression>(
        KtIfExpression::class.java, "Replace 'if' expression with safe access expression"
) {

    override fun isApplicableTo(element: KtIfExpression): Boolean {
        val ifThenToSelectData = element.buildSelectTransformationData() ?: return false
        if (!ifThenToSelectData.receiverExpression.isStableVariable(ifThenToSelectData.context)) return false

        return ifThenToSelectData.clausesReplaceableBySafeCall()
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val ifThenToSelectData = element.buildSelectTransformationData() ?: return

        val factory = KtPsiFactory(element)
        val resultExpr = runWriteAction {
            val replacedBaseClause = ifThenToSelectData.replacedBaseClause(factory)
            val newExpr = element.replaced(replacedBaseClause)
            KtPsiUtil.deparenthesize(newExpr)
        }

        if (editor != null) {
            (resultExpr as? KtSafeQualifiedExpression)?.inlineReceiverIfApplicableWithPrompt(editor)
        }
    }

    private fun IfThenToSelectData.clausesReplaceableBySafeCall(): Boolean {
        if (baseClause == null || negatedClause != null && !negatedClause.isNullExpression()) return false
        return baseClause.evaluatesTo(receiverExpression) && condition !is KtBinaryExpression ||
               baseClause.hasFirstReceiverOf(receiverExpression) && !baseClause.hasNullableType(context)
    }
}
