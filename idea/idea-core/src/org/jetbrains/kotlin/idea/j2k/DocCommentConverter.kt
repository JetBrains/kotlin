/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k

import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag

interface DocCommentConverter {
    fun convertDocComment(docComment: PsiDocComment): String
}

object EmptyDocCommentConverter : DocCommentConverter {
    override fun convertDocComment(docComment: PsiDocComment) = docComment.text
}


fun PsiDocTag.content(): String =
    children
        .dropWhile { it?.node?.elementType == JavaDocTokenType.DOC_TAG_NAME }
        .dropWhile { it is PsiWhiteSpace }
        .filterNot { it?.node?.elementType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS }
        .joinToString("") { it.text }