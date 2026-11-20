/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds
import org.jetbrains.kotlin.syntax.KDocSyntaxTokenIds

@ApiStatus.Experimental
object KDocTokens : SyntaxElementTypesWithIds() {
    val START: SyntaxElementType = register(KDocSyntaxTokenIds.START, "KDOC_START")
    val END: SyntaxElementType = register(KDocSyntaxTokenIds.END, "KDOC_END")
    val LEADING_ASTERISK: SyntaxElementType = register(KDocSyntaxTokenIds.LEADING_ASTERISK, "KDOC_LEADING_ASTERISK")

    val TEXT: SyntaxElementType = register(KDocSyntaxTokenIds.TEXT, "KDOC_TEXT")
    val CODE_BLOCK_TEXT: SyntaxElementType = register(KDocSyntaxTokenIds.CODE_BLOCK_TEXT, "KDOC_CODE_BLOCK_TEXT")
    val CODE_SPAN_TEXT: SyntaxElementType = register(KDocSyntaxTokenIds.CODE_SPAN_TEXT, "KDOC_CODE_SPAN_TEXT")

    val TAG_NAME: SyntaxElementType = register(KDocSyntaxTokenIds.TAG_NAME, "KDOC_TAG_NAME")
    val MARKDOWN_LINK: SyntaxElementType = register(KDocSyntaxTokenIds.MARKDOWN_LINK, "KDOC_MARKDOWN_LINK")
    val MARKDOWN_ESCAPED_CHAR: SyntaxElementType = register(KDocSyntaxTokenIds.MARKDOWN_ESCAPED_CHAR, "KDOC_MARKDOWN_ESCAPED_CHAR")

    val KDOC_LPAR: SyntaxElementType = register(KDocSyntaxTokenIds.KDOC_LPAR, "KDOC_LPAR")
    val KDOC_RPAR: SyntaxElementType = register(KDocSyntaxTokenIds.KDOC_RPAR, "KDOC_RPAR")
}
