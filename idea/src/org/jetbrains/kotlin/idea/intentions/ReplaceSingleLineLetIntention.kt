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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceSingleLineLetInspection : IntentionBasedInspection<KtCallExpression>(ReplaceSingleLineLetIntention::class) {
    override fun inspectionTarget(element: KtCallExpression) = element.calleeExpression
}

class ReplaceSingleLineLetIntention : SelfTargetingOffsetIndependentIntention<KtCallExpression>(
    KtCallExpression::class.java,
    "Remove redundant '.let' call"
) {
    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val lambdaExpression = element.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return
        val bodyExpression = lambdaExpression.bodyExpression?.children?.singleOrNull() ?: return
        when (bodyExpression) {
            is KtDotQualifiedExpression -> bodyExpression.applyTo(element)
            is KtBinaryExpression -> bodyExpression.applyTo(element)
            is KtCallExpression -> bodyExpression.applyTo(element, lambdaExpression.functionLiteral, editor)
        }
    }

    private fun KtBinaryExpression.applyTo(element: KtCallExpression) {
        val left = left ?: return
        val factory = KtPsiFactory(element.project)
        val parent = element.parent
        when (parent) {
            is KtQualifiedExpression -> {
                val receiver = parent.receiverExpression
                val newLeft = when (left) {
                    is KtDotQualifiedExpression -> left.replaceFirstReceiver(factory, receiver, parent is KtSafeQualifiedExpression)
                    else -> receiver
                }
                val newExpression = factory.createExpressionByPattern("$0 $1 $2", newLeft, operationReference, right!!)
                parent.replace(newExpression)
            }
            else -> {
                val newLeft = when (left) {
                    is KtDotQualifiedExpression -> left.deleteFirstReceiver()
                    else -> factory.createThisExpression()
                }
                val newExpression = factory.createExpressionByPattern("$0 $1 $2", newLeft, operationReference, right!!)
                element.replace(newExpression)
            }
        }
    }

    private fun KtDotQualifiedExpression.applyTo(element: KtCallExpression) {
        val parent = element.parent
        when (parent) {
            is KtQualifiedExpression -> {
                val factory = KtPsiFactory(element.project)
                val receiver = parent.receiverExpression
                parent.replace(replaceFirstReceiver(factory, receiver, parent is KtSafeQualifiedExpression))
            }
            else -> {
                element.replace(deleteFirstReceiver())
            }
        }
    }

    private fun KtCallExpression.applyTo(element: KtCallExpression, functionLiteral: KtFunctionLiteral, editor: Editor?) {
        val parent = element.parent as? KtQualifiedExpression
        val reference = functionLiteral.valueParameterReferences(this).firstOrNull()
        val replaced = if (parent != null) {
            reference?.replace(parent.receiverExpression)
            parent.replaced(this)
        } else {
            reference?.replace(KtPsiFactory(this).createThisExpression())
            element.replaced(this)
        }
        editor?.caretModel?.moveToOffset(replaced.startOffset)
    }

    override fun isApplicableTo(element: KtCallExpression): Boolean {
        if (!element.isLetMethodCall()) return false
        val lambdaExpression = element.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return false
        val parameterName = lambdaExpression.getParameterName() ?: return false
        val bodyExpression = lambdaExpression.bodyExpression?.children?.singleOrNull() ?: return false

        return when (bodyExpression) {
            is KtBinaryExpression -> element.parent !is KtSafeQualifiedExpression && bodyExpression.isApplicable(parameterName)
            is KtDotQualifiedExpression -> bodyExpression.isApplicable(parameterName)
            is KtCallExpression -> element.parent !is KtSafeQualifiedExpression
                    && lambdaExpression.functionLiteral.valueParameterReferences(bodyExpression).count() <= 1
            else -> false
        }
    }

    private fun KtBinaryExpression.isApplicable(parameterName: String, isTopLevel: Boolean = true): Boolean {
        val left = left ?: return false
        if (isTopLevel) {
            when (left) {
                is KtNameReferenceExpression -> if (left.text != parameterName) return false
                is KtDotQualifiedExpression -> if (!left.isApplicable(parameterName)) return false
                else -> return false
            }
        } else {
            if (!left.isApplicable(parameterName)) return false
        }

        val right = right ?: return false
        return right.isApplicable(parameterName)
    }

    private fun KtExpression.isApplicable(parameterName: String): Boolean = when (this) {
        is KtNameReferenceExpression -> text != parameterName
        is KtDotQualifiedExpression -> !hasLambdaExpression() && !nameUsed(parameterName)
        is KtBinaryExpression -> isApplicable(parameterName, isTopLevel = false)
        is KtCallExpression -> isApplicable(parameterName)
        is KtConstantExpression -> true
        else -> false
    }

    private fun KtCallExpression.isApplicable(parameterName: String): Boolean = valueArguments.all {
        val argumentExpression = it.getArgumentExpression() ?: return@all false
        argumentExpression.isApplicable(parameterName)
    }

    private fun KtDotQualifiedExpression.isApplicable(parameterName: String) =
        !hasLambdaExpression() && getLeftMostReceiverExpression().let { receiver ->
            receiver is KtNameReferenceExpression &&
                    receiver.getReferencedName() == parameterName &&
                    !nameUsed(parameterName, except = receiver)
        }

    private fun KtDotQualifiedExpression.hasLambdaExpression() = selectorExpression?.anyDescendantOfType<KtLambdaExpression>() ?: false

    private fun KtCallExpression.isLetMethodCall() = calleeExpression?.text == "let" && isMethodCall("kotlin.let")

    private fun KtLambdaExpression.getParameterName(): String? {
        val parameters = valueParameters
        if (parameters.size > 1) return null
        return if (parameters.size == 1) parameters[0].text else "it"
    }

    private fun KtExpression.nameUsed(name: String, except: KtNameReferenceExpression? = null): Boolean =
        anyDescendantOfType<KtNameReferenceExpression> { it != except && it.getReferencedName() == name }

    private fun KtFunctionLiteral.valueParameterReferences(callExpression: KtCallExpression): List<KtReferenceExpression> {
        val context = analyze(BodyResolveMode.PARTIAL)
        val descriptor = context[BindingContext.FUNCTION, this]?.valueParameters?.singleOrNull() ?: return emptyList()
        val name = descriptor.name.asString()
        return callExpression.valueArguments.flatMap { arg ->
            arg.collectDescendantsOfType<KtReferenceExpression>().filter {
                it.text == name && it.getResolvedCall(context)?.resultingDescriptor == descriptor
            }
        }
    }
}