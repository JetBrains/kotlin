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

import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPrefixExpression
import org.jetbrains.jet.lang.psi.JetPostfixExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector
import com.intellij.psi.util.PsiTreeUtil

public class OperatorToFunctionIntention : JetSelfTargetingIntention<JetExpression>("operator.to.function", javaClass()) {
    fun isApplicablePrefix(element: JetPrefixExpression): Boolean {
        return when (element.getOperationReference().getReferencedNameElementType()) {
            JetTokens.PLUS, JetTokens.MINUS, JetTokens.PLUSPLUS, JetTokens.MINUSMINUS, JetTokens.EXCL -> true
            else -> false
        }
    }

    fun isApplicablePostfix(element: JetPostfixExpression): Boolean {
        return when (element.getOperationReference().getReferencedNameElementType()) {
            JetTokens.PLUSPLUS, JetTokens.MINUSMINUS -> true
            else -> false
        }
    }

    fun isApplicableBinary(element: JetBinaryExpression): Boolean {
        return when (element.getOperationReference().getReferencedNameElementType()) {
            JetTokens.PLUS, JetTokens.MINUS, JetTokens.MUL, JetTokens.DIV, JetTokens.PERC, JetTokens.RANGE, JetTokens.IN_KEYWORD, JetTokens.NOT_IN, JetTokens.PLUSEQ, JetTokens.MINUSEQ, JetTokens.MULTEQ, JetTokens.DIVEQ, JetTokens.PERCEQ, JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.GT, JetTokens.LT, JetTokens.GTEQ, JetTokens.LTEQ -> true
            JetTokens.EQ -> element.getLeft() is JetArrayAccessExpression
            else -> false
        }
    }

    fun isApplicableArrayAccess(element: JetArrayAccessExpression): Boolean {
        return true
    }

    fun isApplicableCall(element: JetCallExpression): Boolean {
        return element.getParent() !is JetDotQualifiedExpression && element.getValueArgumentList() != null
    }

    override fun isApplicableTo(element: JetExpression): Boolean {
        return when (element) {
            is JetPrefixExpression -> isApplicablePrefix(element)
            // is JetPostfixExpression -> isApplicablePostfix(element)
            is JetBinaryExpression -> isApplicableBinary(element)
            is JetArrayAccessExpression -> isApplicableArrayAccess(element)
            is JetCallExpression -> isApplicableCall(element)
            else -> false
        }
    }


    fun convertPrefix(element: JetPrefixExpression) {
        val op = element.getOperationReference().getReferencedNameElementType()
        val base = element.getBaseExpression()!!.getText()

        val call = when (op) {
            JetTokens.PLUS -> "plus()"
            JetTokens.MINUS -> "minus()"
            JetTokens.PLUSPLUS -> "inc()"
            JetTokens.MINUSMINUS -> "dec()"
            JetTokens.EXCL -> "not()"
            else -> return
        }

        val transformation = "$base.$call"
        val transformed = JetPsiFactory.createExpression(element.getProject(), transformation)
        element.replace(transformed)
    }

    fun convertBinary(element: JetBinaryExpression) {
        val op = element.getOperationReference().getReferencedNameElementType()
        val left = element.getLeft()!!
        val right = element.getRight()!!
        val leftText = left.getText()
        val rightText = right.getText()

        if (op == JetTokens.EQ) {
            if (left is JetArrayAccessExpression) {
                convertArrayAccess(left as JetArrayAccessExpression)
            }
            return
        }

        val context = AnalyzerFacadeWithCache.getContextForElement(element.getOperationReference())
        val functionCandidate = context[BindingContext.RESOLVED_CALL, element.getOperationReference()]
        val functionName = functionCandidate?.getCandidateDescriptor()?.getName().toString()

        val transformation = when (op) {
            JetTokens.PLUS -> "$leftText.plus($rightText)"
            JetTokens.MINUS -> "$leftText.minus($rightText)"
            JetTokens.MUL -> "$leftText.times($rightText)"
            JetTokens.DIV -> "$leftText.div($rightText)"
            JetTokens.PERC -> "$leftText.mod($rightText)"
            JetTokens.RANGE -> "$leftText.rangeTo($rightText)"
            JetTokens.IN_KEYWORD -> "$rightText.contains($leftText)"
            JetTokens.NOT_IN -> "!$rightText.contains($leftText)"
            JetTokens.PLUSEQ -> if (functionName == "plusAssign") "$leftText.plusAssign($rightText)" else "$leftText = $leftText.plus($rightText)"
            JetTokens.MINUSEQ -> if (functionName == "minusAssign") "$leftText.minusAssign($rightText)" else "$leftText = $leftText.minus($rightText)"
            JetTokens.MULTEQ -> if (functionName == "multAssign") "$leftText.multAssign($rightText)" else "$leftText = $leftText.mult($rightText)"
            JetTokens.DIVEQ -> if (functionName == "divAssign") "$leftText.divAssign($rightText)" else "$leftText = $leftText.div($rightText)"
            JetTokens.PERCEQ -> if (functionName == "modAssign") "$leftText.modAssign($rightText)" else "$leftText = $leftText.mod($rightText)"
            JetTokens.EQEQ -> "$leftText?.equals($rightText) ?: $rightText.identityEquals(null)"
            JetTokens.EXCLEQ -> "!($leftText?.equals($rightText) ?: $rightText.identityEquals(null))"
            JetTokens.GT -> "$leftText.compareTo($rightText) > 0"
            JetTokens.LT -> "$leftText.compareTo($rightText) < 0"
            JetTokens.GTEQ -> "$leftText.compareTo($rightText) >= 0"
            JetTokens.LTEQ -> "$leftText.compareTo($rightText) <= 0"
            else -> return
        }

        val transformed = JetPsiFactory.createExpression(element.getProject(), transformation)

        element.replace(transformed)
    }

    fun convertArrayAccess(element: JetArrayAccessExpression) {
        val parent = element.getParent()
        val array = element.getArrayExpression()!!.getText()
        val indices = element.getIndicesNode()
        val indicesText = indices.getText()!!.trim("[","]")

        val transformation : String
        val replaced : JetElement
        if (parent is JetBinaryExpression && parent.getOperationReference().getReferencedNameElementType() == JetTokens.EQ) {
            // part of an assignment
            val right = parent.getRight()!!.getText()
            transformation = "$array.set($indicesText, $right)"
            replaced = parent
        }
        else {
            transformation = "$array.get($indicesText)"
            replaced = element
        }

        val transformed = JetPsiFactory.createExpression(element.getProject(), transformation)
        replaced.replace(transformed)
    }

    fun convertCall(element: JetCallExpression) {
        val callee = element.getCalleeExpression()!!
        val arguments = element.getValueArgumentList()!!

        val calleeText = callee.getText()
        val argumentsText = arguments.getText()!!.trim("(",")")

        val transformation = "$calleeText.invoke($argumentsText)"
        val transformed = JetPsiFactory.createExpression(element.getProject(), transformation)

        callee.getParent()!!.replace(transformed)
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        return when (element) {
            is JetPrefixExpression -> convertPrefix(element)
            is JetBinaryExpression -> convertBinary(element)
            is JetArrayAccessExpression -> convertArrayAccess(element)
            is JetCallExpression -> convertCall(element)
        }
    }
}
