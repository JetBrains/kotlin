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
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contentRange
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.typeUtil.supertypes

class ConvertTryFinallyToUseCallInspection : IntentionBasedInspection<KtTryExpression>(ConvertTryFinallyToUseCallIntention::class) {
    override fun inspectionTarget(element: KtTryExpression) = element.tryKeyword ?: element.tryBlock
}

class ConvertTryFinallyToUseCallIntention : SelfTargetingOffsetIndependentIntention<KtTryExpression>(
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
            }
            else if (finallyExpressionReceiver is KtThisExpression) {
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

        element.replace(useCallExpression)
    }

    override fun isApplicableTo(element: KtTryExpression): Boolean {
        // Single statement in finally, no catch blocks
        val finallySection = element.finallyBlock ?: return false
        val finallyExpression = finallySection.finalExpression.statements.singleOrNull() ?: return false
        if (element.catchClauses.isNotEmpty()) return false

        val context = element.analyze()
        val resolvedCall = finallyExpression.getResolvedCall(context) ?: return false
        if (resolvedCall.candidateDescriptor.name.asString() != "close") return false
        if (resolvedCall.extensionReceiver != null) return false
        val receiver = resolvedCall.dispatchReceiver ?: return false
        if (receiver.type.supertypes().all {
            it.constructor.declarationDescriptor?.fqNameSafe?.asString().let {
                it != "java.io.Closeable" && it != "java.lang.AutoCloseable"
            }
        }) return false

        return when (receiver) {
            is ExpressionReceiver -> {
                val expression = receiver.expression
                if (expression is KtThisExpression) true
                else {
                    val resourceReference = expression as? KtReferenceExpression ?: return false
                    val resourceDescriptor =
                            context[BindingContext.REFERENCE_TARGET, resourceReference] as? VariableDescriptor ?: return false
                    !resourceDescriptor.isVar
                }
            }
            is ImplicitReceiver -> true
            else -> false
        }
    }
}