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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class ReplaceMathMaxWithCoerceAtLeastIntention() : SelfTargetingOffsetIndependentIntention<KtCallExpression>(
        KtCallExpression::class.java, "Replace Math.max with coerceAtLeast") {

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val target = element.getStrictParentOfType<KtDotQualifiedExpression>() ?: element
        val valueArguments = element.valueArguments
        val newExpression = KtPsiFactory(element).createExpression("${valueArguments[0].text}.coerceAtLeast(${valueArguments[1].text})")
        target.replaced(newExpression)
    }

    override fun isApplicableTo(element: KtCallExpression) = isMaxMethod(element)

    private fun isMaxMethod(element: KtCallExpression) =
            element.calleeExpression?.text == "max" && element.valueArguments.size == 2 && element.isMethodCall("java.lang.Math.max")

    private fun KtCallExpression.isMethodCall(fqMethodName: String): Boolean {
        val resolvedCall = this.getResolvedCall(this.analyze()) ?: return false
        return resolvedCall.resultingDescriptor.fqNameUnsafe.asString() == fqMethodName
    }

}