/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class AbstractJoinListIntention<TList : KtElement, TElement : KtElement>(
    listClass: Class<TList>,
    elementClass: Class<TElement>,
    text: String
) : AbstractChopListIntention<TList, TElement>(listClass, elementClass, text) {

    override fun isApplicableTo(element: TList): Boolean {
        val elements = element.elements()
        if (elements.isEmpty()) return false
        return hasLineBreakBefore(elements.first()) || elements.any { hasLineBreakAfter(it) }
    }

    override fun applyTo(element: TList, editor: Editor?) {
        val document = editor!!.document
        val elements = element.elements()

        nextBreak(elements.last())?.let { document.deleteString(it.startOffset, it.endOffset) }
        elements.dropLast(1).asReversed().forEach {
            nextBreak(it)?.let { document.replaceString(it.startOffset, it.endOffset, " ") }
        }
        prevBreak(elements.first())?.let { document.deleteString(it.startOffset, it.endOffset) }
    }

}

class JoinParameterListIntention : AbstractJoinListIntention<KtParameterList, KtParameter>(
    KtParameterList::class.java,
    KtParameter::class.java,
    "Put parameters on one line"
) {
    override fun isApplicableTo(element: KtParameterList): Boolean {
        if (element.parent is KtFunctionLiteral) return false
        return super.isApplicableTo(element)
    }
}

class JoinArgumentListIntention : AbstractJoinListIntention<KtValueArgumentList, KtValueArgument>(
    KtValueArgumentList::class.java,
    KtValueArgument::class.java,
    "Put arguments on one line"
)
