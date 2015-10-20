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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

public class OperatorToFunctionIntention : JetSelfTargetingIntention<KtExpression>(javaClass(), "Replace overloaded operator with function call") {
    companion object {
        private fun isApplicablePrefix(element: KtPrefixExpression, caretOffset: Int): Boolean {
            val opRef = element.getOperationReference()
            if (!opRef.getTextRange().containsOffset(caretOffset)) return false
            return when (opRef.getReferencedNameElementType()) {
                KtTokens.PLUS, KtTokens.MINUS, KtTokens.PLUSPLUS, KtTokens.MINUSMINUS, KtTokens.EXCL -> true
                else -> false
            }
        }

        private fun isApplicablePostfix(element: KtPostfixExpression, caretOffset: Int): Boolean {
            val opRef = element.getOperationReference()
            if (!opRef.getTextRange().containsOffset(caretOffset)) return false
            if (element.getBaseExpression() == null) return false
            return when (opRef.getReferencedNameElementType()) {
                KtTokens.PLUSPLUS, KtTokens.MINUSMINUS -> true
                else -> false
            }
        }

        private fun isApplicableBinary(element: KtBinaryExpression, caretOffset: Int): Boolean {
            val opRef = element.getOperationReference()
            if (!opRef.getTextRange().containsOffset(caretOffset)) return false
            return when (opRef.getReferencedNameElementType()) {
                KtTokens.PLUS, KtTokens.MINUS, KtTokens.MUL, KtTokens.DIV, KtTokens.PERC, KtTokens.RANGE, KtTokens.IN_KEYWORD, KtTokens.NOT_IN, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ, KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.GT, KtTokens.LT, KtTokens.GTEQ, KtTokens.LTEQ -> true
                KtTokens.EQ -> element.getLeft() is KtArrayAccessExpression
                else -> false
            }
        }

        private fun isApplicableArrayAccess(element: KtArrayAccessExpression, caretOffset: Int): Boolean {
            val lbracket = element.getLeftBracket() ?: return false
            val rbracket = element.getRightBracket() ?: return false
            return lbracket.getTextRange().containsOffset(caretOffset) || rbracket.getTextRange().containsOffset(caretOffset)
        }

        private fun isApplicableCall(element: KtCallExpression, caretOffset: Int): Boolean {
            val lbrace = (element.getValueArgumentList()?.getLeftParenthesis()
                          ?: element.getFunctionLiteralArguments().firstOrNull()?.getFunctionLiteral()?.getLeftCurlyBrace()
                          ?: return false) as PsiElement
            if (!lbrace.getTextRange().containsOffset(caretOffset)) return false

            val resolvedCall = element.getResolvedCall(element.analyze())
            val descriptor = resolvedCall?.getResultingDescriptor()
            if (descriptor is FunctionDescriptor && descriptor.getName() == OperatorNameConventions.INVOKE) {
                if (element.getParent() is KtDotQualifiedExpression &&
                    element.getCalleeExpression()?.getText() == OperatorNameConventions.INVOKE.asString()) return false
                return element.getValueArgumentList() != null || element.getFunctionLiteralArguments().isNotEmpty()
            }
            return false
        }

        private fun convertPrefix(element: KtPrefixExpression): KtExpression {
            val op = element.getOperationReference().getReferencedNameElementType()
            val base = element.getBaseExpression()!!.getText()

            val call = when (op) {
                KtTokens.PLUS -> "plus()"
                KtTokens.MINUS -> "minus()"
                KtTokens.PLUSPLUS -> "inc()"
                KtTokens.MINUSMINUS -> "dec()"
                KtTokens.EXCL -> "not()"
                else -> return element
            }

            val transformation = "$base.$call"
            val transformed = KtPsiFactory(element).createExpression(transformation)
            return element.replace(transformed) as KtExpression
        }

        private fun convertPostFix(element: KtPostfixExpression): KtExpression {
            val op = element.getOperationReference().getReferencedNameElementType()
            val base = element.getBaseExpression()!!.getText()

            val call = when (op) {
                KtTokens.PLUSPLUS -> "inc()"
                KtTokens.MINUSMINUS -> "dec()"
                else -> return element
            }

            val transformation = "$base.$call"
            val transformed = KtPsiFactory(element).createExpression(transformation)
            return element.replace(transformed) as KtExpression
        }

        private fun convertBinary(element: KtBinaryExpression): KtExpression {
            val op = element.getOperationReference().getReferencedNameElementType()
            val left = element.getLeft()!!
            val right = element.getRight()!!
            val leftText = left.getText()
            val rightText = right.getText()

            if (op == KtTokens.EQ) {
                if (left is KtArrayAccessExpression) {
                    convertArrayAccess(left)
                }
                return element
            }

            val context = element.analyze(BodyResolveMode.PARTIAL)
            val functionCandidate = element.getResolvedCall(context)
            val functionName = functionCandidate?.getCandidateDescriptor()?.getName().toString()
            val elemType = context.getType(left)

            val transformation = when (op) {
                KtTokens.PLUS -> "$leftText.plus($rightText)"
                KtTokens.MINUS -> "$leftText.minus($rightText)"
                KtTokens.MUL -> "$leftText.times($rightText)"
                KtTokens.DIV -> "$leftText.div($rightText)"
                KtTokens.PERC -> "$leftText.mod($rightText)"
                KtTokens.RANGE -> "$leftText.rangeTo($rightText)"
                KtTokens.IN_KEYWORD -> "$rightText.contains($leftText)"
                KtTokens.NOT_IN -> "!$rightText.contains($leftText)"
                KtTokens.PLUSEQ -> if (functionName == "plusAssign") "$leftText.plusAssign($rightText)" else "$leftText = $leftText.plus($rightText)"
                KtTokens.MINUSEQ -> if (functionName == "minusAssign") "$leftText.minusAssign($rightText)" else "$leftText = $leftText.minus($rightText)"
                KtTokens.MULTEQ -> if (functionName == "multAssign") "$leftText.multAssign($rightText)" else "$leftText = $leftText.mult($rightText)"
                KtTokens.DIVEQ -> if (functionName == "divAssign") "$leftText.divAssign($rightText)" else "$leftText = $leftText.div($rightText)"
                KtTokens.PERCEQ -> if (functionName == "modAssign") "$leftText.modAssign($rightText)" else "$leftText = $leftText.mod($rightText)"
                KtTokens.EQEQ -> if (elemType?.isMarkedNullable() ?: true) "$leftText?.equals($rightText) ?: $rightText.identityEquals(null)" else "$leftText.equals($rightText)"
                KtTokens.EXCLEQ -> if (elemType?.isMarkedNullable() ?: true) "!($leftText?.equals($rightText) ?: $rightText.identityEquals(null))" else "!$leftText.equals($rightText)"
                KtTokens.GT -> "$leftText.compareTo($rightText) > 0"
                KtTokens.LT -> "$leftText.compareTo($rightText) < 0"
                KtTokens.GTEQ -> "$leftText.compareTo($rightText) >= 0"
                KtTokens.LTEQ -> "$leftText.compareTo($rightText) <= 0"
                else -> return element
            }

            val transformed = KtPsiFactory(element).createExpression(transformation)

            return element.replace(transformed) as KtExpression
        }

        private fun convertArrayAccess(element: KtArrayAccessExpression): KtExpression {
            val parent = element.getParent()
            val array = element.getArrayExpression()!!.getText()
            val indices = element.getIndicesNode()
            val indicesText = indices.getText()?.removeSurrounding("[","]") ?: throw AssertionError("Indices node of ArrayExpression shouldn't be null: JetArrayAccessExpression = ${element.getText()}")

            val transformation : String
            val replaced : KtElement
            if (parent is KtBinaryExpression && parent.getOperationReference().getReferencedNameElementType() == KtTokens.EQ) {
                // part of an assignment
                val right = parent.getRight()!!.getText()
                transformation = "$array.set($indicesText, $right)"
                replaced = parent
            }
            else {
                transformation = "$array.get($indicesText)"
                replaced = element
            }

            val transformed = KtPsiFactory(element).createExpression(transformation)
            return replaced.replace(transformed) as KtExpression
        }

        private fun convertCall(element: KtCallExpression): KtExpression {
            val callee = element.getCalleeExpression()!!
            val arguments = element.getValueArgumentList()
            val argumentString = arguments?.getText()?.removeSurrounding("(", ")")
            val funcLitArgs = element.getFunctionLiteralArguments()
            val calleeText = callee.getText()
            val transformation = "$calleeText.${OperatorNameConventions.INVOKE.asString()}" +
                                 (if (argumentString == null) "" else "($argumentString)")
            val transformed = KtPsiFactory(element).createExpression(transformation)
            funcLitArgs.forEach { transformed.add(it) }
            return callee.getParent()!!.replace(transformed) as KtExpression
        }

        public fun convert(element: KtExpression): Pair<KtExpression, KtSimpleNameExpression> {
            val result = when (element) {
                is KtPrefixExpression -> convertPrefix(element)
                is KtPostfixExpression -> convertPostFix(element)
                is KtBinaryExpression -> convertBinary(element)
                is KtArrayAccessExpression -> convertArrayAccess(element)
                is KtCallExpression -> convertCall(element)
                else -> throw IllegalArgumentException(element.toString())
            }

            val callName = findCallName(result)
                           ?: error("No call name found in ${result.text}")
            return result to callName
        }

        private fun findCallName(result: KtExpression): KtSimpleNameExpression? {
            return when (result) {
                is KtBinaryExpression -> {
                    if (KtPsiUtil.isAssignment(result))
                        findCallName(result.getRight()!!)
                    else
                        findCallName(result.getLeft()!!)
                }

                is KtUnaryExpression -> findCallName(result.getBaseExpression()!!)

                else -> result.getQualifiedElementSelector() as KtSimpleNameExpression?
            }
        }
    }

    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        return when (element) {
            is KtPrefixExpression -> isApplicablePrefix(element, caretOffset)
            is KtPostfixExpression -> isApplicablePostfix(element, caretOffset)
            is KtBinaryExpression -> isApplicableBinary(element, caretOffset)
            is KtArrayAccessExpression -> isApplicableArrayAccess(element, caretOffset)
            is KtCallExpression -> isApplicableCall(element, caretOffset)
            else -> false
        }
    }

    override fun applyTo(element: KtExpression, editor: Editor) {
        convert(element)
    }
}
