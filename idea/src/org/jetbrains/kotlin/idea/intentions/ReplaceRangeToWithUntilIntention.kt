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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

private fun KtExpression.getCallableDescriptor() = getResolvedCall(analyze())?.resultingDescriptor

private fun KtExpression.getArguments() = when (this) {
    is KtBinaryExpression -> this.left to this.right
    is KtDotQualifiedExpression -> this.receiverExpression to this.callExpression?.valueArguments?.singleOrNull()?.getArgumentExpression()
    else -> null
}

private val REGEX_RANGE_TO = """kotlin.(Char|Byte|Short|Int|Long).rangeTo""".toRegex()

class ReplaceRangeToWithUntilIntention : SelfTargetingIntention<KtExpression>(
        KtExpression::class.java,
        "Replace with 'until' function call"
) {

    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (element !is KtBinaryExpression && element !is KtDotQualifiedExpression) return false

        val fqName = element.getCallableDescriptor()?.fqNameUnsafe?.asString() ?: return false
        if (!fqName.matches(REGEX_RANGE_TO)) return false
        return element.getArguments()?.second?.isMinusOne() == true
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        val args = element.getArguments() ?: return
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0 until $1",
                                                                        args.first ?: return,
                                                                        (args.second as? KtBinaryExpression)?.left ?: return))
    }

    private fun KtExpression.isMinusOne(): Boolean {
        if (this !is KtBinaryExpression) return false
        if (operationToken != KtTokens.MINUS) return false

        val right = right as? KtConstantExpression ?: return false
        val context = right.analyze(BodyResolveMode.PARTIAL)
        val constantValue = ConstantExpressionEvaluator.getConstant(right, context)?.toConstantValue(right.getType(context) ?: return false)
        val rightValue = (constantValue?.value as? Number)?.toInt() ?: return false
        return rightValue == 1
    }

}

class ReplaceUntilWithRangeToIntention : SelfTargetingIntention<KtExpression>(KtExpression::class.java, "Replace with '..' operator") {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (element !is KtBinaryExpression && element !is KtDotQualifiedExpression) return false
        val fqName = element.getCallableDescriptor()?.fqNameUnsafe?.asString() ?: return false
        return fqName == "kotlin.ranges.until"
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        val args = element.getArguments() ?: return
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0..$1 - 1", args.first ?: return, args.second ?: return))
    }
}