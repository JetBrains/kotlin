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

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStableSimpleExpression
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRow
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isNullabilityMismatch

class SurroundWithNullCheckFix(
    expression: KtExpression,
    nullableExpression: KtExpression
) : KotlinQuickFixAction<KtExpression>(expression), HighPriorityAction {
    private val nullableExpressionPointer = nullableExpression.createSmartPointer()

    override fun getFamilyName() = text

    override fun getText() = "Surround with null check"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val nullableExpression = nullableExpressionPointer.element ?: return
        val factory = KtPsiFactory(element)
        val surrounded = factory.createExpressionByPattern("if ($0 != null) { $1 }", nullableExpression, element)
        element.replace(surrounded)
    }

    companion object : KotlinSingleIntentionActionFactory() {

        private fun KtExpression.hasAcceptableParent() = with(parent) {
            this is KtBlockExpression || this.parent is KtIfExpression ||
                    this is KtWhenEntry || this.parent is KtLoopExpression
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement
            val expressionParent = element.getParentOfType<KtExpression>(strict = element is KtOperationReferenceExpression) ?: return null
            val context = expressionParent.analyze(BodyResolveMode.PARTIAL_WITH_CFA)

            val parent = element.parent
            val nullableExpression =
                when (parent) {
                    is KtDotQualifiedExpression -> parent.receiverExpression
                    is KtBinaryExpression -> parent.left
                    is KtCallExpression -> parent.calleeExpression
                    else -> return null
                } as? KtReferenceExpression ?: return null

            if (!nullableExpression.isStableSimpleExpression(context)) return null

            val expressionTarget = expressionParent.getParentOfTypesAndPredicate(
                strict = false, parentClasses = *arrayOf(KtExpression::class.java)
            ) {
                !it.isUsedAsExpression(context) && it.hasAcceptableParent()
            } ?: return null
            // Surround declaration (even of local variable) with null check is generally a bad idea
            if (expressionTarget is KtDeclaration) return null

            return SurroundWithNullCheckFix(expressionTarget, nullableExpression)
        }
    }

    object IteratorOnNullableFactory : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val nullableExpression = diagnostic.psiElement as? KtReferenceExpression ?: return null
            val forExpression = nullableExpression.parent.parent as? KtForExpression ?: return null
            if (forExpression.parent !is KtBlockExpression) return null

            if (!nullableExpression.isStableSimpleExpression()) return null

            return SurroundWithNullCheckFix(forExpression, nullableExpression)
        }
    }

    object TypeMismatchFactory : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeMismatch = Errors.TYPE_MISMATCH.cast(diagnostic)
            val nullableExpression = typeMismatch.psiElement as? KtReferenceExpression ?: return null
            val parent = nullableExpression.parent
            val root = when (parent) {
                is KtValueArgument -> {
                    val call = parent.getParentOfType<KtCallExpression>(true) ?: return null
                    call.getLastParentOfTypeInRow<KtQualifiedExpression>() ?: call
                }
                is KtBinaryExpression -> {
                    if (parent.right != nullableExpression) return null
                    parent
                }
                else -> return null
            }
            if (root.parent !is KtBlockExpression) return null
            if (!isNullabilityMismatch(expected = typeMismatch.a, actual = typeMismatch.b)) return null
            if (!nullableExpression.isStableSimpleExpression()) return null
            return SurroundWithNullCheckFix(root, nullableExpression)
        }
    }
}
