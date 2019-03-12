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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class AbstractChopListIntention<TList : KtElement, TElement : KtElement>(
    private val listClass: Class<TList>,
    private val elementClass: Class<TElement>,
    text: String
) : SelfTargetingOffsetIndependentIntention<TList>(listClass, text) {

    override fun isApplicableTo(element: TList): Boolean {
        val elements = element.elements()
        if (elements.size <= 1) return false
        if (elements.dropLast(1).all { hasLineBreakAfter(it) }) return false
        return true
    }

    override fun applyTo(element: TList, editor: Editor?) {
        val project = element.project
        val document = editor!!.document
        val startOffset = element.startOffset

        val elements = element.elements()
        if (!hasLineBreakAfter(elements.last())) {
            element.allChildren.lastOrNull { it.node.elementType == KtTokens.RPAR }?.startOffset?.let { document.insertString(it, "\n") }
        }

        for (e in elements.asReversed()) {
            if (!hasLineBreakBefore(e)) {
                document.insertString(e.startOffset, "\n")
            }
        }

        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitDocument(document)
        val psiFile = documentManager.getPsiFile(document) ?: return
        val newList = PsiTreeUtil.getParentOfType(psiFile.findElementAt(startOffset) ?: return, listClass) ?: return
        CodeStyleManager.getInstance(project).adjustLineIndent(psiFile, newList.textRange)
    }

    protected fun hasLineBreakAfter(element: TElement): Boolean {
        return nextBreak(element) != null
    }

    protected fun nextBreak(element: TElement): PsiWhiteSpace? {
        return element
            .siblings(withItself = false)
            .takeWhile { !elementClass.isInstance(it) }
            .firstOrNull { it is PsiWhiteSpace && it.textContains('\n') } as? PsiWhiteSpace
    }

    protected fun hasLineBreakBefore(element: TElement): Boolean {
        return prevBreak(element) != null
    }

    protected fun prevBreak(element: TElement): PsiWhiteSpace? {
        return element
            .siblings(withItself = false, forward = false)
            .takeWhile { !elementClass.isInstance(it) }
            .firstOrNull { it is PsiWhiteSpace && it.textContains('\n') } as? PsiWhiteSpace
    }

    protected fun TList.elements(): List<TElement> {
        return allChildren
            .filter { elementClass.isInstance(it) }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as TElement
            }
            .toList()
    }
}

class ChopParameterListIntention : AbstractChopListIntention<KtParameterList, KtParameter>(
    KtParameterList::class.java,
    KtParameter::class.java,
    "Put parameters on separate lines"
) {
    override fun isApplicableTo(element: KtParameterList): Boolean {
        if (element.parent is KtFunctionLiteral) return false
        return super.isApplicableTo(element)
    }
}

class ChopArgumentListIntention : AbstractChopListIntention<KtValueArgumentList, KtValueArgument>(
    KtValueArgumentList::class.java,
    KtValueArgument::class.java,
    "Put arguments on separate lines"
)