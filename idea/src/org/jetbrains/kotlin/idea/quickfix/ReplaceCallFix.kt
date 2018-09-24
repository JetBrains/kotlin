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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

abstract class ReplaceCallFix(
        expression: KtQualifiedExpression,
        private val operation: String,
        private val notNullNeeded: Boolean = false
) : KotlinQuickFixAction<KtQualifiedExpression>(expression) {

    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.selectorExpression != null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val elvis = elvisOrEmpty(notNullNeeded)
        val newExpression = KtPsiFactory(element).createExpressionByPattern("$0$operation$1$elvis",
                                                                            element.receiverExpression, element.selectorExpression!!)
        val replacement = element.replace(newExpression)
        if (notNullNeeded) {
            replacement.moveCaretToEnd(editor, project)
        }
    }
}

class ReplaceImplicitReceiverCallFix(
        expression: KtExpression,
        private val notNullNeeded: Boolean
) : KotlinQuickFixAction<KtExpression>(expression) {
    override fun getFamilyName() = text

    override fun getText() = "Replace with safe (this?.) call"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val elvis = elvisOrEmpty(notNullNeeded)
        val newExpression = KtPsiFactory(element).createExpressionByPattern("this?.$0$elvis", element)
        val replacement = element.replace(newExpression)
        if (notNullNeeded) {
            replacement.moveCaretToEnd(editor, project)
        }
    }
}

class ReplaceWithSafeCallFix(
        expression: KtDotQualifiedExpression,
        notNullNeeded: Boolean
) : ReplaceCallFix(expression, "?.", notNullNeeded) {

    override fun getText() = "Replace with safe (?.) call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val psiElement = diagnostic.psiElement
            val qualifiedExpression = psiElement.parent as? KtDotQualifiedExpression
            if (qualifiedExpression != null) {
                return ReplaceWithSafeCallFix(qualifiedExpression, qualifiedExpression.shouldHaveNotNullType())
            }
            else {
                if (psiElement !is KtNameReferenceExpression) return null
                if (psiElement.getResolvedCall(psiElement.analyze())?.getImplicitReceiverValue() != null) {
                    val expressionToReplace: KtExpression = psiElement.parent as? KtCallExpression ?: psiElement
                    return ReplaceImplicitReceiverCallFix(expressionToReplace, expressionToReplace.shouldHaveNotNullType())
                }
                return null
            }
        }
    }
}

class ReplaceWithSafeCallForScopeFunctionFix(
        expression: KtDotQualifiedExpression,
        notNullNeeded: Boolean
) : ReplaceCallFix(expression, "?.", notNullNeeded) {

    override fun getText() = "Replace scope function with safe (?.) call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val element = diagnostic.psiElement
            val scopeFunctionLiteral = element.getStrictParentOfType<KtFunctionLiteral>() ?: return null
            val scopeCallExpression = scopeFunctionLiteral.getStrictParentOfType<KtCallExpression>() ?: return null
            val scopeDotQualifiedExpression = scopeCallExpression.getStrictParentOfType<KtDotQualifiedExpression>() ?: return null

            val context = scopeCallExpression.analyze()
            val scopeFunctionLiteralDescriptor = context[BindingContext.FUNCTION, scopeFunctionLiteral] ?: return null
            val scopeFunctionKind = scopeCallExpression.scopeFunctionKind(context) ?: return null

            val internalReceiver = (element.parent as? KtDotQualifiedExpression)?.receiverExpression
            val internalReceiverDescriptor = internalReceiver.getResolvedCall(context)?.candidateDescriptor
            val internalResolvedCall = (element.getParentOfType<KtElement>(strict = false))?.getResolvedCall(context)
                                       ?: return null

            when (scopeFunctionKind) {
                ScopeFunctionKind.WITH_PARAMETER -> {
                    if (internalReceiverDescriptor != scopeFunctionLiteralDescriptor.valueParameters.singleOrNull()) {
                        return null
                    }
                }
                ScopeFunctionKind.WITH_RECEIVER -> {
                    if (internalReceiverDescriptor != scopeFunctionLiteralDescriptor.extensionReceiverParameter &&
                        internalResolvedCall.getImplicitReceiverValue() == null) {
                        return null
                    }
                }
            }

            return ReplaceWithSafeCallForScopeFunctionFix(
                    scopeDotQualifiedExpression, scopeDotQualifiedExpression.shouldHaveNotNullType())
        }

        private fun KtCallExpression.scopeFunctionKind(context: BindingContext): ScopeFunctionKind? {
            val methodName = getResolvedCall(context)?.resultingDescriptor?.fqNameUnsafe?.asString()
            return ScopeFunctionKind.values().firstOrNull { kind -> kind.names.contains(methodName) }
        }

        private enum class ScopeFunctionKind(vararg val names: String) {
            WITH_PARAMETER("kotlin.let", "kotlin.also"),
            WITH_RECEIVER("kotlin.apply", "kotlin.run")
        }
    }
}

class ReplaceWithDotCallFix(expression: KtSafeQualifiedExpression) : ReplaceCallFix(expression, "."), CleanupFix {
    override fun getText() = "Replace with dot call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val qualifiedExpression = diagnostic.psiElement.getParentOfType<KtSafeQualifiedExpression>(strict = false) ?: return null
            return ReplaceWithDotCallFix(qualifiedExpression)
        }
    }
}

