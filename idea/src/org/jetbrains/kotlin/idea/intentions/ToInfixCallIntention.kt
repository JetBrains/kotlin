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

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ToInfixCallIntention : SelfTargetingIntention<KtCallExpression>(KtCallExpression::class.java, "Replace with infix function call") {
    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        val calleeExpr = element.calleeExpression as? KtNameReferenceExpression ?: return false
        if (!calleeExpr.textRange.containsOffset(caretOffset)) return false

        val dotQualified = element.getQualifiedExpressionForSelector() ?: return false

        if (element.typeArgumentList != null) return false

        val argument = element.valueArguments.singleOrNull() ?: return false
        if (argument.isNamed()) return false
        if (argument.getArgumentExpression() == null) return false

        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = element.getResolvedCall(bindingContext) ?: return false
        val function = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return false
        if (!function.isInfix) return false

        // check that receiver has type to filter out calls with package/java class qualifier
        if (bindingContext.getType(dotQualified.receiverExpression) == null) return false

        return true
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val dotQualified = element.parent as KtDotQualifiedExpression
        val receiver = dotQualified.receiverExpression
        val argument = element.valueArguments.single().getArgumentExpression()!!
        val name = element.calleeExpression!!.text

        val newCall = KtPsiFactory(element).createExpressionByPattern("$0 $name $1", receiver, argument)
        dotQualified.replace(newCall)
    }
}
