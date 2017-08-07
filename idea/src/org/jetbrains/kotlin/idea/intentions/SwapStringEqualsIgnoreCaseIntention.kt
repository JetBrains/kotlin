/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class SwapStringEqualsIgnoreCaseIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(KtDotQualifiedExpression::class.java, "Flip 'equals'"), LowPriorityAction {

    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        val descriptor = element.getCallableDescriptor() ?: return null

        val fqName: FqName = descriptor.fqNameOrNull() ?: return null
        if (fqName.asString() != "kotlin.text.equals") return null

        val valueParameters = descriptor.valueParameters.takeIf { it.size == 2 } ?: return null
        if (!KotlinBuiltIns.isStringOrNullableString(valueParameters[0].type)) return null
        if (!KotlinBuiltIns.isBoolean(valueParameters[1].type)) return null

        return element.callExpression?.calleeExpression?.textRange
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val callExpression = element.callExpression ?: return
        val offset = (editor?.caretModel?.offset ?: 0) - (callExpression.calleeExpression?.startOffset ?: 0)
        val receiverExpression = element.receiverExpression
        val valueArguments = callExpression.valueArguments.takeIf { it.size == 2 } ?: return
        val newElement = KtPsiFactory(element).createExpressionByPattern(
                "$0.equals($1, $2)",
                valueArguments[0].getArgumentExpression()!!,
                receiverExpression,
                valueArguments[1].text
        )
        val replacedElement = element.replaced(newElement) as? KtDotQualifiedExpression
        replacedElement?.callExpression?.calleeExpression?.startOffset?.let {
            editor?.moveCaret(it + offset)
        }
    }

}