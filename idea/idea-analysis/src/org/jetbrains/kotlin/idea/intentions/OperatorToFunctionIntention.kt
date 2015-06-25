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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class OperatorToFunctionIntention : JetSelfTargetingIntention<JetExpression>(javaClass(), "Replace overloaded operator with function call") {
    companion object {
        private fun isApplicablePrefix(element: JetPrefixExpression, caretOffset: Int): Boolean {
            val opRef = element.getOperationReference()
            if (!opRef.getTextRange().containsOffset(caretOffset)) return false
            return when (opRef.getReferencedNameElementType()) {
                JetTokens.PLUS, JetTokens.MINUS, JetTokens.PLUSPLUS, JetTokens.MINUSMINUS, JetTokens.EXCL -> true
                else -> false
            }
        }

        private fun isApplicablePostfix(element: JetPostfixExpression, caretOffset: Int): Boolean {
            val opRef = element.getOperationReference()
            if (!opRef.getTextRange().containsOffset(caretOffset)) return false
            if (element.getBaseExpression() == null) return false
            return when (opRef.getReferencedNameElementType()) {
                JetTokens.PLUSPLUS, JetTokens.MINUSMINUS -> true
                else -> false
            }
        }

        private fun isApplicableBinary(element: JetBinaryExpression, caretOffset: Int): Boolean {
            val opRef = element.getOperationReference()
            if (!opRef.getTextRange().containsOffset(caretOffset)) return false
            return when (opRef.getReferencedNameElementType()) {
                JetTokens.PLUS, JetTokens.MINUS, JetTokens.MUL, JetTokens.DIV, JetTokens.PERC, JetTokens.RANGE, JetTokens.IN_KEYWORD, JetTokens.NOT_IN, JetTokens.PLUSEQ, JetTokens.MINUSEQ, JetTokens.MULTEQ, JetTokens.DIVEQ, JetTokens.PERCEQ, JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.GT, JetTokens.LT, JetTokens.GTEQ, JetTokens.LTEQ -> true
                JetTokens.EQ -> element.getLeft() is JetArrayAccessExpression
                else -> false
            }
        }

        private fun isApplicableArrayAccess(element: JetArrayAccessExpression, caretOffset: Int): Boolean {
            val lbracket = element.getLeftBracket() ?: return false
            val rbracket = element.getRightBracket() ?: return false
            return lbracket.getTextRange().containsOffset(caretOffset) || rbracket.getTextRange().containsOffset(caretOffset)
        }

        private fun isApplicableCall(element: JetCallExpression, caretOffset: Int): Boolean {
            val lbrace = (element.getValueArgumentList()?.getLeftParenthesis()
                          ?: element.getFunctionLiteralArguments().firstOrNull()?.getFunctionLiteral()?.getLeftCurlyBrace()
                          ?: return false) as PsiElement
            if (!lbrace.getTextRange().containsOffset(caretOffset)) return false

            val resolvedCall = element.getResolvedCall(element.analyze())
            val descriptor = resolvedCall?.getResultingDescriptor()
            if (descriptor is FunctionDescriptor && descriptor.getName() == OperatorConventions.INVOKE) {
                if (element.getParent() is JetDotQualifiedExpression &&
                    element.getCalleeExpression()?.getText() == OperatorConventions.INVOKE.asString()) return false
                return element.getValueArgumentList() != null || element.getFunctionLiteralArguments().isNotEmpty()
            }
            return false
        }

        private fun convertPrefix(element: JetPrefixExpression): JetExpression {
            val op = element.getOperationReference().getReferencedNameElementType()
            val base = element.getBaseExpression()!!.getText()

            val call = when (op) {
                JetTokens.PLUS -> "plus()"
                JetTokens.MINUS -> "minus()"
                JetTokens.PLUSPLUS -> "inc()"
                JetTokens.MINUSMINUS -> "dec()"
                JetTokens.EXCL -> "not()"
                else -> return element
            }

            val transformation = "$base.$call"
            val transformed = JetPsiFactory(element).createExpression(transformation)
            return element.replace(transformed) as JetExpression
        }

        private fun convertPostFix(element: JetPostfixExpression): JetExpression {
            val op = element.getOperationReference().getReferencedNameElementType()
            val base = element.getBaseExpression()!!.getText()

            val call = when (op) {
                JetTokens.PLUSPLUS -> "inc()"
                JetTokens.MINUSMINUS -> "dec()"
                else -> return element
            }

            val transformation = "$base.$call"
            val transformed = JetPsiFactory(element).createExpression(transformation)
            return element.replace(transformed) as JetExpression
        }

        private fun convertBinary(element: JetBinaryExpression): JetExpression {
            val op = element.getOperationReference().getReferencedNameElementType()
            val left = element.getLeft()!!
            val right = element.getRight()!!
            val leftText = left.getText()
            val rightText = right.getText()

            if (op == JetTokens.EQ) {
                if (left is JetArrayAccessExpression) {
                    convertArrayAccess(left)
                }
                return element
            }

            val context = element.analyze()
            val functionCandidate = element.getResolvedCall(context)
            val functionName = functionCandidate?.getCandidateDescriptor()?.getName().toString()
            val elemType = context.getType(left)

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
                JetTokens.EQEQ -> if (elemType?.isMarkedNullable() ?: true) "$leftText?.equals($rightText) ?: $rightText.identityEquals(null)" else "$leftText.equals($rightText)"
                JetTokens.EXCLEQ -> if (elemType?.isMarkedNullable() ?: true) "!($leftText?.equals($rightText) ?: $rightText.identityEquals(null))" else "!$leftText.equals($rightText)"
                JetTokens.GT -> "$leftText.compareTo($rightText) > 0"
                JetTokens.LT -> "$leftText.compareTo($rightText) < 0"
                JetTokens.GTEQ -> "$leftText.compareTo($rightText) >= 0"
                JetTokens.LTEQ -> "$leftText.compareTo($rightText) <= 0"
                else -> return element
            }

            val transformed = JetPsiFactory(element).createExpression(transformation)

            return element.replace(transformed) as JetExpression
        }

        private fun convertArrayAccess(element: JetArrayAccessExpression): JetExpression {
            val parent = element.getParent()
            val array = element.getArrayExpression()!!.getText()
            val indices = element.getIndicesNode()
            val indicesText = indices.getText()?.removeSurrounding("[","]") ?: throw AssertionError("Indices node of ArrayExpression shouldn't be null: JetArrayAccessExpression = ${element.getText()}")

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

            val transformed = JetPsiFactory(element).createExpression(transformation)
            return replaced.replace(transformed) as JetExpression
        }

        private fun convertCall(element: JetCallExpression): JetExpression {
            val callee = element.getCalleeExpression()!!
            val arguments = element.getValueArgumentList()
            val argumentString = arguments?.getText()?.removeSurrounding("(", ")")
            val funcLitArgs = element.getFunctionLiteralArguments()
            val calleeText = callee.getText()
            val transformation = "$calleeText.${OperatorConventions.INVOKE.asString()}" +
                                 (if (argumentString == null) "" else "($argumentString)")
            val transformed = JetPsiFactory(element).createExpression(transformation)
            funcLitArgs.forEach { transformed.add(it) }
            return callee.getParent()!!.replace(transformed) as JetExpression
        }

        public fun convert(element: JetExpression): JetExpression {
            return when (element) {
                is JetPrefixExpression -> convertPrefix(element)
                is JetPostfixExpression -> convertPostFix(element)
                is JetBinaryExpression -> convertBinary(element)
                is JetArrayAccessExpression -> convertArrayAccess(element)
                is JetCallExpression -> convertCall(element)
                else -> element
            }
        }
    }

    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        return when (element) {
            is JetPrefixExpression -> isApplicablePrefix(element, caretOffset)
            is JetPostfixExpression -> isApplicablePostfix(element, caretOffset)
            is JetBinaryExpression -> isApplicableBinary(element, caretOffset)
            is JetArrayAccessExpression -> isApplicableArrayAccess(element, caretOffset)
            is JetCallExpression -> isApplicableCall(element, caretOffset)
            else -> false
        }
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        convert(element)
    }
}
