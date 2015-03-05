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
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.psi.JetThisExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetOperationExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetUserType
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetArrayAccessExpression

class KotlinEditorTextProvider : EditorTextProvider {
    override fun getEditorText(elementAtCaret: PsiElement): TextWithImports? {
        val expression = findExpressionInner(elementAtCaret, true)
        return TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expression?.getText() ?: "", "", JetFileType.INSTANCE)
    }

    override fun findExpression(elementAtCaret: PsiElement, allowMethodCalls: Boolean): Pair<PsiElement, TextRange>? {
        val expression = findExpressionInner(elementAtCaret, allowMethodCalls)
        if (expression == null) return null
        return Pair(expression, expression.getTextRange())
    }

    class object {
        fun findExpressionInner(element: PsiElement, allowMethodCalls: Boolean): JetExpression? {
            if (PsiTreeUtil.getParentOfType(element, javaClass<JetUserType>(), javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) != null) {
                return null
            }

            val jetElement = PsiTreeUtil.getParentOfType(element, javaClass<JetElement>())
            if (jetElement == null) return null

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
                        newExpression is JetQualifiedExpression && newExpression.getSelectorExpression().isCall()) {
                    return null
                }
            }

            return when {
                newExpression is JetExpression -> newExpression
                jetElement is JetSimpleNameExpression -> jetElement
                else -> null
            }

        }
    }
}

