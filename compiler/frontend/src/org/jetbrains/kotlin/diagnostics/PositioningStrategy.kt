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

package org.jetbrains.kotlin.diagnostics

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

open class PositioningStrategy<in E : PsiElement> {
    open fun markDiagnostic(diagnostic: ParametrizedDiagnostic<out E>): List<TextRange> {
        return mark(diagnostic.psiElement)
    }

    open fun mark(element: E): List<TextRange> {
        return markElement(element)
    }

    open fun isValid(element: E): Boolean {
        return !hasSyntaxErrors(element)
    }
}

fun markElement(element: PsiElement): List<TextRange> {
    return listOf(TextRange(getStartOffset(element), getEndOffset(element)))
}

fun markNode(node: ASTNode): List<TextRange> {
    return markElement(node.psi)
}

fun markRange(range: TextRange): List<TextRange> {
    return listOf(range)
}

fun markRange(from: PsiElement, to: PsiElement): List<TextRange> {
    return markRange(TextRange(getStartOffset(from), getEndOffset(to)))
}

private fun getStartOffset(element: PsiElement): Int {
    var child = element.firstChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.nextSibling
        }
        if (child != null) {
            return getStartOffset(child)
        }
    }
    return element.startOffset
}

private fun getEndOffset(element: PsiElement): Int {
    var child = element.lastChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.prevSibling
        }
        if (child != null) {
            return getEndOffset(child)
        }
    }
    return element.endOffset
}

fun hasSyntaxErrors(psiElement: PsiElement): Boolean {
    if (psiElement is PsiErrorElement) return true

    val children = psiElement.children
    return children.isNotEmpty() && hasSyntaxErrors(children.last())
}

