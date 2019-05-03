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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils

class InsertExplicitTypeArgumentsIntention :
    SelfTargetingRangeIntention<KtCallExpression>(KtCallExpression::class.java, "Add explicit type arguments"), LowPriorityAction {
    override fun applicabilityRange(element: KtCallExpression): TextRange? {
        return if (isApplicableTo(element, element.analyze())) element.calleeExpression?.textRange else null
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) = applyTo(element)

    companion object {
        fun isApplicableTo(element: KtCallElement, bindingContext: BindingContext = element.analyze(BodyResolveMode.PARTIAL)): Boolean {
            if (element.typeArguments.isNotEmpty()) return false
            if (element.calleeExpression == null) return false

            val resolvedCall = element.getResolvedCall(bindingContext) ?: return false
            val typeArgs = resolvedCall.typeArguments
            return typeArgs.isNotEmpty() && typeArgs.values.none { ErrorUtils.containsErrorType(it) || it is CapturedType }
        }

        fun applyTo(element: KtCallElement, shortenReferences: Boolean = true) {
            val argumentList = createTypeArguments(element, element.analyze()) ?: return

            val callee = element.calleeExpression ?: return
            val newArgumentList = element.addAfter(argumentList, callee) as KtTypeArgumentList

            if (shortenReferences) {
                ShortenReferences.DEFAULT.process(newArgumentList)
            }
        }

        fun createTypeArguments(element: KtCallElement, bindingContext: BindingContext): KtTypeArgumentList? {
            val resolvedCall = element.getResolvedCall(bindingContext) ?: return null

            val args = resolvedCall.typeArguments
            val types = resolvedCall.candidateDescriptor.typeParameters

            val text = types.joinToString(", ", "<", ">") {
                IdeDescriptorRenderers.SOURCE_CODE.renderType(args.getValue(it))
            }

            return KtPsiFactory(element).createTypeArguments(text)
        }
    }
}
