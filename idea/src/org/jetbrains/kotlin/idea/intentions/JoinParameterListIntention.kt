/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class AbstractJoinListIntention<TList : KtElement, TElement : KtElement>(
    listClass: Class<TList>,
    elementClass: Class<TElement>,
    textGetter: () -> String
) : AbstractChopListIntention<TList, TElement>(listClass, elementClass, textGetter) {
    override fun isApplicableTo(element: TList, caretOffset: Int): Boolean {
        val elements = element.elements()
        if (elements.isEmpty()) return false
        if (!isApplicableCaretOffset(caretOffset, element)) return false
        return (hasLineBreakBefore(elements.first()) || elements.any { hasLineBreakAfter(it) })
                && element.allChildren.none { it is PsiComment && it.node.elementType == KtTokens.EOL_COMMENT }
    }

    override fun applyTo(element: TList, editor: Editor?) {
        val document = editor?.document ?: return
        val elements = element.elements()
        val pointer = element.createSmartPointer()
        nextBreak(elements.last())?.let { document.deleteString(it.startOffset, it.endOffset) }
        elements.dropLast(1).asReversed().forEach { tElement ->
            nextBreak(tElement)?.let { document.replaceString(it.startOffset, it.endOffset, " ") }
        }

        prevBreak(elements.first())?.let { document.deleteString(it.startOffset, it.endOffset) }

        val project = element.project
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitDocument(document)
        pointer.element?.let { CodeStyleManager.getInstance(project).reformat(it) }
    }

}

class JoinParameterListIntention : AbstractJoinListIntention<KtParameterList, KtParameter>(
    KtParameterList::class.java,
    KtParameter::class.java,
    KotlinBundle.lazyMessage("put.parameters.on.one.line")
) {
    override fun isApplicableTo(element: KtParameterList, caretOffset: Int): Boolean {
        if (element.parent is KtFunctionLiteral) return false
        return super.isApplicableTo(element, caretOffset)
    }
}

class JoinArgumentListIntention : AbstractJoinListIntention<KtValueArgumentList, KtValueArgument>(
    KtValueArgumentList::class.java,
    KtValueArgument::class.java,
    KotlinBundle.lazyMessage("put.arguments.on.one.line")
)
