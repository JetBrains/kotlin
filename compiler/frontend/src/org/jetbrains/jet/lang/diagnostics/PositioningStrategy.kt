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

package org.jetbrains.jet.lang.diagnostics

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

import java.util.Collections

public open class PositioningStrategy<E : PsiElement> {
    public open fun markDiagnostic(diagnostic: ParametrizedDiagnostic<out E>): List<TextRange> {
        return mark(diagnostic.getPsiElement())
    }

    protected open fun mark(element: E): List<TextRange> {
        return markElement(element)
    }

    public open fun isValid(element: E): Boolean {
        return !hasSyntaxErrors(element)
    }

    class object {

        protected fun markElement(element: PsiElement): List<TextRange> {
            return listOf(TextRange(getStartOffset(element), getEndOffset(element)))
        }

        protected fun markNode(node: ASTNode): List<TextRange> {
            return markElement(node.getPsi())
        }

        protected fun markRange(range: TextRange): List<TextRange> {
            return listOf(range)
        }

        protected fun markRange(from: PsiElement, to: PsiElement): List<TextRange> {
            return markRange(TextRange(getStartOffset(from), getEndOffset(to)))
        }

        private fun getStartOffset(element: PsiElement): Int {
            var child: PsiElement? = element.getFirstChild()
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child!!.getNextSibling()
                }
                if (child != null) {
                    return getStartOffset(child)
                }
            }
            return element.getTextRange().getStartOffset()
        }

        private fun getEndOffset(element: PsiElement): Int {
            var child: PsiElement? = element.getLastChild()
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child!!.getPrevSibling()
                }
                if (child != null) {
                    return getEndOffset(child)
                }
            }
            return element.getTextRange().getEndOffset()
        }

        protected fun hasSyntaxErrors(psiElement: PsiElement): Boolean {
            if (psiElement is PsiErrorElement) return true

            val children = psiElement.getChildren()
            if (children.size > 0 && hasSyntaxErrors(children[children.size - 1])) return true

            return false
        }
    }
}
