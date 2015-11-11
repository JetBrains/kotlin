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

package org.jetbrains.kotlin.idea.quickfix.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

class InvokeOnExtensionFunctionWithExplicitReceiverFix(qualifiedExpression: KtDotQualifiedExpression) : KotlinQuickFixAction<KtDotQualifiedExpression>(qualifiedExpression), CleanupFix {
    override fun getFamilyName() = "Surround callee with parenthesis"
    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val callExpression = element.selectorExpression as KtCallExpression
        val newCallee = KtPsiFactory(file).createExpressionByPattern("($0.$1)", element.receiverExpression, callExpression.calleeExpression!!)
        val newCallExpression = element.replaced(callExpression)
        newCallExpression.calleeExpression!!.replace(newCallee)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val callee = Errors.INVOKE_ON_EXTENSION_FUNCTION_WITH_EXPLICIT_DISPATCH_RECEIVER.cast(diagnostic).psiElement as? KtNameReferenceExpression ?: return null
            val callExpression = callee.parent as? KtCallExpression ?: return null
            val qualifiedExpression = callExpression.getQualifiedExpressionForSelector() as? KtDotQualifiedExpression ?: return null
            return InvokeOnExtensionFunctionWithExplicitReceiverFix(qualifiedExpression)
        }
    }
}