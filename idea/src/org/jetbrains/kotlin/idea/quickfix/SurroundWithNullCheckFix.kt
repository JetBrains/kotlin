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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory

class SurroundWithNullCheckFix(
        expression: KtExpression,
        val nullableExpression: KtExpression
) : KotlinQuickFixAction<KtExpression>(expression) {

    override fun getFamilyName() = text

    override fun getText() = "Surround with null check"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val factory = KtPsiFactory(element)
        val surrounded = factory.createExpressionByPattern("if ($0 != null) { $1 }", nullableExpression, element)
        element.replace(surrounded)
    }

    companion object : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement
            val expression = element.getParentOfTypesAndPredicate(false, KtExpression::class.java) {
                it is KtDeclaration ||
                it.parent is KtBlockExpression ||
                it.parent?.parent is KtIfExpression ||
                it.parent?.parent is KtWhenExpression ||
                it.parent?.parent is KtLoopExpression
            } ?: return null
            if (expression is KtDeclaration) return null

            val parent = element.parent
            val nullableExpression = when (parent) {
                is KtDotQualifiedExpression -> parent.receiverExpression
                is KtBinaryExpression -> parent.left
                is KtCallExpression -> parent.calleeExpression
                else -> return null
            } as? KtReferenceExpression ?: return null

            if (!nullableExpression.isPredictable()) return null

            return SurroundWithNullCheckFix(expression, nullableExpression)
        }
    }
}

private fun KtExpression.isPredictable(): Boolean {
    val context = this.analyze()
    val nullableType = this.getType(context) ?: return false
    val containingDescriptor = this.getResolutionScope(context, this.getResolutionFacade()).ownerDescriptor
    return DataFlowValueFactory.createDataFlowValue(this, nullableType, context, containingDescriptor).isPredictable
}
