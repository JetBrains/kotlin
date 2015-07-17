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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.impl.EditorTextProvider
import com.intellij.psi.PsiElement
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Pair
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import org.jetbrains.kotlin.idea.JetFileType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

class KotlinEditorTextProvider : EditorTextProvider {
    override fun getEditorText(elementAtCaret: PsiElement): TextWithImports? {
        val expression = findExpressionInner(elementAtCaret, true) ?: return null

        val expressionText = getElementInfo(expression) { it.getText() }
        return TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expressionText, "", JetFileType.INSTANCE)
    }

    override fun findExpression(elementAtCaret: PsiElement, allowMethodCalls: Boolean): Pair<PsiElement, TextRange>? {
        val expression = findExpressionInner(elementAtCaret, allowMethodCalls) ?: return null

        val expressionRange = getElementInfo(expression) { it.getTextRange() }
        return Pair(expression, expressionRange)
    }

    companion object {

        fun <T> getElementInfo(expr: JetExpression, f: (PsiElement) -> T): T {
            var expressionText = f(expr)
            if (expr is JetProperty) {
                val nameIdentifier = expr.getNameIdentifier()
                if (nameIdentifier != null) {
                    expressionText = f(nameIdentifier)
                }
            }
            return expressionText
        }

        fun findExpressionInner(element: PsiElement, allowMethodCalls: Boolean): JetExpression? {
            if (!isAcceptedAsCodeFragmentContext(element)) return null

            val jetElement = PsiTreeUtil.getParentOfType(element, javaClass<JetElement>())
            if (jetElement == null) return null

            if (jetElement is JetProperty) {
                val nameIdentifier = jetElement.getNameIdentifier()
                if (nameIdentifier == element) {
                    return jetElement
                }
            }

            val parent = jetElement.getParent()
            if (parent == null) return null

            val newExpression = when (parent) {
                is JetThisExpression,
                is JetSuperExpression,
                is JetReferenceExpression -> {
                    val pparent = parent.getParent()
                    when (pparent) {
                        is JetQualifiedExpression -> pparent
                        else -> parent
                    }
                }
                is JetQualifiedExpression -> {
                    if (parent.getReceiverExpression() != jetElement) {
                        parent
                    } else {
                        null
                    }
                }
                is JetOperationExpression -> {
                    if (parent.getOperationReference() == jetElement) {
                        parent
                    } else {
                        null
                    }
                }
                else -> null
            }

            if (!allowMethodCalls && newExpression != null) {
                fun PsiElement.isCall() = this is JetCallExpression || this is JetOperationExpression || this is JetArrayAccessExpression

                if (newExpression.isCall() ||
                        newExpression is JetQualifiedExpression && newExpression.getSelectorExpression()!!.isCall()) {
                    return null
                }
            }

            return when {
                newExpression is JetExpression -> newExpression
                jetElement is JetSimpleNameExpression -> jetElement
                else -> null
            }

        }

        private val NOT_ACCEPTED_AS_CONTEXT_TYPES =
                arrayOf(javaClass<JetUserType>(), javaClass<JetImportDirective>(), javaClass<JetPackageDirective>())

        fun isAcceptedAsCodeFragmentContext(element: PsiElement): Boolean {
            return element.javaClass !in NOT_ACCEPTED_AS_CONTEXT_TYPES &&
                   PsiTreeUtil.getParentOfType(element, *NOT_ACCEPTED_AS_CONTEXT_TYPES) == null
        }
    }
}

