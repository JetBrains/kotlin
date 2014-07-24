/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.resolve.CompileTimeConstantUtils
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.psiUtil.copied
import org.jetbrains.jet.lang.psi.psiUtil.replaced

public class SimplifyBooleanWithConstantsIntention : JetSelfTargetingIntention<JetBinaryExpression>(
        "simplify.boolean.with.constants", javaClass()) {

    private var topParent : JetBinaryExpression? = null

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        topParent = PsiTreeUtil.getTopmostParentOfType(element, javaClass<JetBinaryExpression>()) ?: element
        return areThereExpressionsToBeSimplified(topParent)
    }

    private fun areThereExpressionsToBeSimplified(element: JetExpression?) : Boolean {
        if (element == null) return false
        when (element) {
            is JetParenthesizedExpression -> return areThereExpressionsToBeSimplified(element.getExpression())
            is JetBinaryExpression -> {
                val op = element.getOperationToken()
                if ((op == JetTokens.ANDAND || op == JetTokens.OROR) &&
                       (areThereExpressionsToBeSimplified(element.getLeft()) ||
                       areThereExpressionsToBeSimplified(element.getRight()))) return true
            }
        }
        return element.canBeReducedToBooleanConstant(null)
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        // we know from isApplicableTo that topParent is not null
        val simplified = toSimplifiedExpression(topParent!!)
        if (simplified is JetParenthesizedExpression) {
            val expr = simplified.getExpression()
            if (expr != null) {
                // this extra check is for the case where there are empty parentheses ()
                topParent!!.replace(expr)
                return
            }
        }
        topParent!!.replace(simplified)
    }

    private fun toSimplifiedExpression(element: JetExpression): JetExpression {
        val psiFactory = JetPsiFactory(element)
        if (element.canBeReducedToTrue())
            return psiFactory.createExpression("true")
        if (element.canBeReducedToFalse())
            return psiFactory.createExpression("false")
        when (element) {
            is JetParenthesizedExpression -> {
                val expr = element.getExpression()
                if (expr == null) return element
                val innerSimplified = toSimplifiedExpression(expr)
                if (innerSimplified is JetBinaryExpression) {
                    val simpText = innerSimplified.getText() ?: return element.copied()
                    // wrap in new parentheses to keep the user's original format
                    return psiFactory.createExpression("($simpText)")
                }
                // if we now have a simpleName, constant, or parenthesized we don't need parentheses
                return innerSimplified
            }
            is JetBinaryExpression -> {
                val left = element.getLeft()
                val right = element.getRight()
                val op = element.getOperationToken()
                if (left == null || right == null || op == null || (op != JetTokens.ANDAND && op != JetTokens.OROR))
                    return element.copied()

                val simpleLeft = simplifyExpression(left)
                val simpleRight = simplifyExpression(right)
                if (simpleLeft.canBeReducedToTrue() || simpleLeft.canBeReducedToFalse())
                    return toSimplifiedBooleanBinaryExpressionWithConstantOperand(simpleLeft, simpleRight, op)
                if (simpleRight.canBeReducedToTrue() || simpleRight.canBeReducedToFalse())
                    return toSimplifiedBooleanBinaryExpressionWithConstantOperand(simpleRight, simpleLeft, op)

                val opText = element.getOperationReference().getText()
                if (opText == null) return element.copied()
                return psiFactory.createBinaryExpression(simpleLeft, opText, simpleRight)
            }
            else -> return element.copied()
        }
    }

    private fun toSimplifiedBooleanBinaryExpressionWithConstantOperand(
            booleanConstantOperand: JetExpression,
            otherOperand: JetExpression,
            operation: IElementType
    ): JetExpression {
        assert(booleanConstantOperand.canBeReducedToBooleanConstant(null), "should only be called when we know it can be reduced")
        val psiFactory = JetPsiFactory(otherOperand)
        if (booleanConstantOperand.canBeReducedToTrue() && operation == JetTokens.OROR)
            return psiFactory.createExpression("true")
        if (booleanConstantOperand.canBeReducedToFalse() && operation == JetTokens.ANDAND)
            return psiFactory.createExpression("false")
        return toSimplifiedExpression(otherOperand)
    }

    private fun simplifyExpression(element: JetExpression): JetExpression {
        return element.replaced(toSimplifiedExpression(element))
    }

    private fun JetExpression.canBeReducedToBooleanConstant(constant: Boolean?): Boolean {
        val bindingContext = AnalyzerFacadeWithCache.getContextForElement(this)
        val trace = DelegatingBindingTrace(bindingContext, "trace for constant check")
        return CompileTimeConstantUtils.canBeReducedToBooleanConstant(this, trace, constant)
    }

    private fun JetExpression.canBeReducedToTrue(): Boolean {
        return this.canBeReducedToBooleanConstant(true)
    }

    private fun JetExpression.canBeReducedToFalse(): Boolean {
        return this.canBeReducedToBooleanConstant(false)
    }
}
