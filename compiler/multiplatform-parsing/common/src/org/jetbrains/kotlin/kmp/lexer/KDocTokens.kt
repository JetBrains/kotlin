/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.KDocTokenId
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KDocTokens : SyntaxElementTypesWithIds() {
    val START: SyntaxElementType = register(KDocTokenId.START, "KDOC_START")
    val END: SyntaxElementType = register(KDocTokenId.END, "KDOC_END")
    val LEADING_ASTERISK: SyntaxElementType = register(KDocTokenId.LEADING_ASTERISK, "KDOC_LEADING_ASTERISK")

    val TEXT: SyntaxElementType = register(KDocTokenId.TEXT, "KDOC_TEXT")
    val CODE_BLOCK_TEXT: SyntaxElementType = register(KDocTokenId.CODE_BLOCK_TEXT, "KDOC_CODE_BLOCK_TEXT")
    val CODE_SPAN_TEXT: SyntaxElementType = register(KDocTokenId.CODE_SPAN_TEXT, "KDOC_CODE_SPAN_TEXT")

    val TAG_NAME: SyntaxElementType = register(KDocTokenId.TAG_NAME, "KDOC_TAG_NAME")
    val MARKDOWN_LINK: SyntaxElementType = register(KDocTokenId.MARKDOWN_LINK, "KDOC_MARKDOWN_LINK")
    val MARKDOWN_ESCAPED_CHAR: SyntaxElementType = register(KDocTokenId.MARKDOWN_ESCAPED_CHAR, "KDOC_MARKDOWN_ESCAPED_CHAR")

    val KDOC_LPAR: SyntaxElementType = register(KDocTokenId.KDOC_LPAR, "KDOC_LPAR")
    val KDOC_RPAR: SyntaxElementType = register(KDocTokenId.KDOC_RPAR, "KDOC_RPAR")
}
