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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.ReplaceRangeToWithUntilIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
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

class ReplaceRangeToWithUntilInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)

                if (expression !is KtBinaryExpression && expression !is KtDotQualifiedExpression) return

                val fqName = expression.getCallableDescriptor()?.fqNameUnsafe?.asString() ?: return
                if (!fqName.matches(REGEX_RANGE_TO)) return

                if (expression.getArguments()?.second?.isMinusOne() != true) return

                holder.registerProblem(
                        expression,
                        "'rangeTo' or the '..' call can be replaced with 'until'",
                        ProblemHighlightType.WEAK_WARNING,
                        IntentionWrapper(ReplaceRangeToWithUntilIntention(), expression.containingKtFile)
                )
            }
        }
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