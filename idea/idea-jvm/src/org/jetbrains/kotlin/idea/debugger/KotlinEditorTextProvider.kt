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

import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.EditorTextProvider
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils

class KotlinEditorTextProvider : EditorTextProvider {
    override fun getEditorText(elementAtCaret: PsiElement): TextWithImports? {
        val expression = findExpressionInner(elementAtCaret, true) ?: return null

        val expressionText = getElementInfo(expression) { it.text }
        return TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expressionText, "", KotlinFileType.INSTANCE)
    }

    override fun findExpression(elementAtCaret: PsiElement, allowMethodCalls: Boolean): Pair<PsiElement, TextRange>? {
        val expression = findExpressionInner(elementAtCaret, allowMethodCalls) ?: return null

        val expressionRange = getElementInfo(expression) { it.textRange }
        return Pair(expression, expressionRange)
    }

    companion object {

        fun <T> getElementInfo(expr: KtExpression, f: (PsiElement) -> T): T {
            var expressionText = f(expr)
            if (expr is KtProperty) {
                val nameIdentifier = expr.nameIdentifier
                if (nameIdentifier != null) {
                    expressionText = f(nameIdentifier)
                }
            }
            return expressionText
        }

        fun findExpressionInner(element: PsiElement, allowMethodCalls: Boolean): KtExpression? {
            if (!isAcceptedAsCodeFragmentContext(element)) return null

            val ktElement = PsiTreeUtil.getParentOfType(element, KtElement::class.java) ?: return null

            if (ktElement is KtProperty) {
                val nameIdentifier = ktElement.nameIdentifier
                if (nameIdentifier == element) {
                    return ktElement
                }
            }

            val parent = ktElement.parent

            val newExpression = when (parent) {
                is KtThisExpression -> parent
                is KtSuperExpression -> {
                    val pparent = parent.parent
                    when (pparent) {
                        is KtQualifiedExpression -> pparent
                        else -> parent
                    }
                }
                is KtReferenceExpression -> {
                    val pparent = parent.parent
                    if (pparent is KtQualifiedExpression && pparent.selectorExpression == parent) {
                        pparent
                    }
                    else {
                        parent
                    }
                }
                is KtQualifiedExpression -> {
                    if (parent.receiverExpression != ktElement) {
                        parent
                    }
                    else {
                        null
                    }
                }
                is KtOperationExpression -> {
                    if (parent.operationReference == ktElement) {
                        parent
                    }
                    else {
                        null
                    }
                }
                else -> null
            }

            if (!allowMethodCalls && newExpression != null) {
                fun PsiElement.isCall() = this is KtCallExpression || this is KtOperationExpression || this is KtArrayAccessExpression

                if (newExpression.isCall() || newExpression is KtQualifiedExpression && newExpression.selectorExpression!!.isCall()) {
                    return null
                }
            }

            return when {
                newExpression is KtExpression -> newExpression
                ktElement is KtSimpleNameExpression -> {
                    val context = ktElement.analyze()
                    val qualifier = context[BindingContext.QUALIFIER, ktElement]
                    if (qualifier != null && !DescriptorUtils.isObject(qualifier.descriptor)) {
                        null
                    }
                    else {
                        ktElement
                    }
                }
                else -> null
            }

        }

        private val NOT_ACCEPTED_AS_CONTEXT_TYPES =
                arrayOf(KtUserType::class.java, KtImportDirective::class.java, KtPackageDirective::class.java, KtValueArgumentName::class.java)

        fun isAcceptedAsCodeFragmentContext(element: PsiElement): Boolean {
            return !NOT_ACCEPTED_AS_CONTEXT_TYPES.contains(element::class.java as Class<*>) &&
                   PsiTreeUtil.getParentOfType(element, *NOT_ACCEPTED_AS_CONTEXT_TYPES) == null
        }
    }
}