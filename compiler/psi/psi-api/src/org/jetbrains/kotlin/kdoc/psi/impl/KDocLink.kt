/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtElementImpl
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * A Markdown link inside a KDoc comment that references a declaration by its (possibly qualified) name.
 *
 * It appears either as the [subject][KDocTag.getSubjectLink] of a block tag or as an inline link within the documentation text.
 *
 * ### Example:
 *
 * ```kotlin
 * /**
 *  * Delegates to [kotlin.collections.List.size].
 *  * //           ^____________________________^
 *  */
 * ```
 */
class KDocLink : KtElementImpl {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    /**
     * Returns the link text without the enclosing square brackets (for example, `kotlin.collections.List` for the
     * link `[kotlin.collections.List]`).
     */
    fun getLinkText(): String = getLinkTextRange().substring(text)

    /**
     * Returns the range of the [link text][getLinkText] within this element, excluding the enclosing square brackets if they are present.
     */
    fun getLinkTextRange(): TextRange {
        val text = text
        if (text.startsWith('[') && text.endsWith(']')) {
            return TextRange(1, text.length - 1)
        }
        return TextRange(0, text.length)
    }

    /**
     * If this link is the subject of a tag, returns the tag. Otherwise, returns null.
     */
    fun getTagIfSubject(): KDocTag? {
        val tag = getStrictParentOfType<KDocTag>()
        return if (tag != null && tag.getSubjectLink() == this) tag else null
    }
}
