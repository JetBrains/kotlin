/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contentRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.typeUtil.supertypes

@Suppress("DEPRECATION")
class ConvertTryFinallyToUseCallInspection : IntentionBasedInspection<KtTryExpression>(ConvertTryFinallyToUseCallIntention::class) {
    override fun inspectionTarget(element: KtTryExpression) = element.tryKeyword ?: element.tryBlock
}

class ConvertTryFinallyToUseCallIntention : SelfTargetingRangeIntention<KtTryExpression>(
    KtTryExpression::class.java, "Convert try-finally to .use()"
) {
    override fun applyTo(element: KtTryExpression, editor: Editor?) {
        val finallySection = element.finallyBlock!!
        val finallyExpression = finallySection.finalExpression.statements.single()
        val finallyExpressionReceiver = (finallyExpression as? KtQualifiedExpression)?.receiverExpression
        val resourceReference = finallyExpressionReceiver as? KtNameReferenceExpression
        val resourceName = resourceReference?.getReferencedNameAsName()

        val factory = KtPsiFactory(element)

        val useCallExpression = factory.buildExpression {
            if (resourceName != null) {
                appendName(resourceName)
                appendFixedText(".")
            } else if (finallyExpressionReceiver is KtThisExpression) {
                appendFixedText(finallyExpressionReceiver.text)
                appendFixedText(".")
            }
            appendFixedText("use {")

            if (resourceName != null) {
                appendName(resourceName)
                appendFixedText("->")
            }
            appendFixedText("\n")

            appendChildRange(element.tryBlock.contentRange())
            appendFixedText("\n}")
        }

        val call = when (val result = element.replace(useCallExpression) as KtExpression) {
            is KtQualifiedExpression -> result.selectorExpression as? KtCallExpression ?: return
            is KtCallExpression -> result
            else -> return
        }
        val lambda = call.lambdaArguments.firstOrNull() ?: return
        val lambdaParameter = lambda.getLambdaExpression()?.valueParameters?.firstOrNull() ?: return
        editor?.selectionModel?.setSelection(lambdaParameter.startOffset, lambdaParameter.endOffset)
    }

    override fun applicabilityRange(element: KtTryExpression): TextRange? {
        // Single statement in finally, no catch blocks
        val finallySection = element.finallyBlock ?: return null
        val finallyExpression = finallySection.finalExpression.statements.singleOrNull() ?: return null
        if (element.catchClauses.isNotEmpty()) return null

        val context = element.analyze()
        val resolvedCall = finallyExpression.getResolvedCall(context) ?: return null
        if (resolvedCall.candidateDescriptor.name.asString() != "close") return null
        if (resolvedCall.extensionReceiver != null) return null
        val receiver = resolvedCall.dispatchReceiver ?: return null
        if (receiver.type.supertypes().all {
                it.constructor.declarationDescriptor?.fqNameSafe?.asString().let { s ->
                    s != "java.io.Closeable" && s != "java.lang.AutoCloseable"
                }
            }) return null

        when (receiver) {
            is ExpressionReceiver -> {
                val expression = receiver.expression
                if (expression !is KtThisExpression) {
                    val resourceReference = expression as? KtReferenceExpression ?: return null
                    val resourceDescriptor =
                        context[BindingContext.REFERENCE_TARGET, resourceReference] as? VariableDescriptor ?: return null
                    if (resourceDescriptor.isVar) return null
                }
            }
            is ImplicitReceiver -> {
            }
            else -> return null
        }

        return TextRange(element.startOffset, element.tryBlock.lBrace?.endOffset ?: element.endOffset)
    }
}