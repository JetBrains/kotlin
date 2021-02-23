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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

object ReplaceWithSafeCallFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val psiElement = diagnostic.psiElement
        val qualifiedExpression = psiElement.parent as? KtDotQualifiedExpression
        if (qualifiedExpression != null) {
            return ReplaceWithSafeCallFix(qualifiedExpression, qualifiedExpression.shouldHaveNotNullType())
        } else {
            if (psiElement !is KtNameReferenceExpression) return null
            if (psiElement.getResolvedCall(psiElement.analyze())?.getImplicitReceiverValue() != null) {
                val expressionToReplace: KtExpression = psiElement.parent as? KtCallExpression ?: psiElement
                return ReplaceImplicitReceiverCallFix(expressionToReplace, expressionToReplace.shouldHaveNotNullType())
            }
            return null
        }
    }
}

object ReplaceWithSafeCallForScopeFunctionFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
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
                    internalResolvedCall.getImplicitReceiverValue() == null
                ) {
                    return null
                }
            }
        }

        return ReplaceWithSafeCallForScopeFunctionFix(
            scopeDotQualifiedExpression, scopeDotQualifiedExpression.shouldHaveNotNullType()
        )
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

