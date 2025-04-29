/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import fleet.com.intellij.platform.syntax.SyntaxElementType

object KDocTokens {
    val START: SyntaxElementType = SyntaxElementType("KDOC_START")
    val END: SyntaxElementType = SyntaxElementType("KDOC_END")
    val LEADING_ASTERISK: SyntaxElementType = SyntaxElementType("KDOC_LEADING_ASTERISK")

    val TEXT: SyntaxElementType = SyntaxElementType("KDOC_TEXT")
    val CODE_BLOCK_TEXT: SyntaxElementType = SyntaxElementType("KDOC_CODE_BLOCK_TEXT")

    val TAG_NAME: SyntaxElementType = SyntaxElementType("KDOC_TAG_NAME")
    val MARKDOWN_LINK: SyntaxElementType = SyntaxElementType("KDOC_MARKDOWN_LINK")
    val MARKDOWN_ESCAPED_CHAR: SyntaxElementType = SyntaxElementType("KDOC_MARKDOWN_ESCAPED_CHAR")

    val KDOC_LPAR: SyntaxElementType = SyntaxElementType("KDOC_LPAR")
    val KDOC_RPAR: SyntaxElementType = SyntaxElementType("KDOC_RPAR")
}