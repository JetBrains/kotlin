/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.isNullable

class RedundantNotNullExtensionReceiverOfInlineInspection : AbstractKotlinInspection() {

    private fun ReceiverValue?.isThisExpressionReceiver(): Boolean =
        this is ExpressionReceiver && this.expression is KtThisExpression

    private fun ResolvedCall<*>?.usesNotNullThisReceiverIn(expression: KtExpression, thisReceiverValue: ReceiverValue): Boolean {
        if (this == null || expression.parent is KtThisExpression || call.isSafeCall()) {
            return false
        }
        val descriptor = resultingDescriptor
        val extensionReceiverType = descriptor?.extensionReceiverParameter?.type
        return if (extensionReceiverType != null) {
            !extensionReceiverType.isNullable() && (extensionReceiver == thisReceiverValue || extensionReceiver.isThisExpressionReceiver())
        } else {
            dispatchReceiver == thisReceiverValue || dispatchReceiver.isThisExpressionReceiver()
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        namedFunctionVisitor(fun(function) {

            val receiverTypeReference = function.receiverTypeReference ?: return
            if (!function.hasModifier(KtTokens.INLINE_KEYWORD) || !function.hasBody()) return
            if (!function.languageVersionSettings.supportsFeature(LanguageFeature.NullabilityAssertionOnExtensionReceiver)) {
                return
            }

            val context = function.analyzeWithContent()
            val functionDescriptor = context[BindingContext.FUNCTION, function] ?: return
            val receiverParameter = functionDescriptor.extensionReceiverParameter ?: return
            val receiverValue = receiverParameter.value
            val receiverType = receiverParameter.type
            if (receiverType.isNullable()) return
            if ((receiverType.constructor.declarationDescriptor as? ClassDescriptor)?.isCompanionObject == true) return

            if (function.anyDescendantOfType<KtExpression> {
                    when (it) {
                        is KtNameReferenceExpression -> {
                            val resolvedCall = it.getResolvedCall(context)
                            resolvedCall.usesNotNullThisReceiverIn(it, receiverValue)
                        }
                        is KtThisExpression -> {
                            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, it]
                            it.parent is KtCallExpression || expectedType != null && !expectedType.isNullable()
                        }
                        is KtBinaryExpressionWithTypeRHS -> {
                            val type = context[BindingContext.TYPE, it.right]
                            it.left is KtThisExpression && type != null && !type.isNullable()
                                    && it.operationReference.getReferencedNameElementType() == KtTokens.AS_KEYWORD
                        }
                        is KtForExpression -> {
                            it.loopRange is KtThisExpression
                        }
                        is KtBinaryExpression -> {
                            if (it.operationToken == KtTokens.EQEQ ||
                                it.operationToken == KtTokens.EQEQEQ ||
                                it.operationToken == KtTokens.EXCLEQ ||
                                it.operationToken == KtTokens.EXCLEQEQEQ
                            ) {
                                false
                            } else {
                                val resolvedCall = it.operationReference.getResolvedCall(context)
                                resolvedCall.usesNotNullThisReceiverIn(it, receiverValue)
                            }
                        }
                        is KtOperationReferenceExpression -> {
                            if (it.parent is KtBinaryExpression) {
                                false
                            } else {
                                val resolvedCall = it.getResolvedCall(context)
                                resolvedCall.usesNotNullThisReceiverIn(it, receiverValue)
                            }
                        }
                        is KtArrayAccessExpression -> {
                            val resolvedCall = it.getResolvedCall(context)
                            resolvedCall.usesNotNullThisReceiverIn(it, receiverValue)
                        }
                        else -> false
                    }
                }) return

            holder.registerProblem(
                receiverTypeReference,
                "This type probably can be changed to nullable",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        })
}