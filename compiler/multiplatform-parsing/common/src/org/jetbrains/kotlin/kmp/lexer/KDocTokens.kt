/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import fleet.com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KDocTokens : SyntaxElementTypesWithIds() {
    const val START_ID = NO_ID + 1
    const val END_ID = START_ID + 1
    const val LEADING_ASTERISK_ID = END_ID + 1
    const val TEXT_ID = LEADING_ASTERISK_ID + 1
    const val CODE_BLOCK_TEXT_ID = TEXT_ID + 1
    const val TAG_NAME_ID = CODE_BLOCK_TEXT_ID + 1
    const val MARKDOWN_LINK_ID = TAG_NAME_ID + 1
    const val MARKDOWN_ESCAPED_CHAR_ID = MARKDOWN_LINK_ID + 1
    const val KDOC_LPAR_ID = MARKDOWN_ESCAPED_CHAR_ID + 1
    const val KDOC_RPAR_ID = KDOC_LPAR_ID + 1

    val START: SyntaxElementType = registerElementType(START_ID, "KDOC_START")
    val END: SyntaxElementType = registerElementType(END_ID, "KDOC_END")
    val LEADING_ASTERISK: SyntaxElementType = registerElementType(LEADING_ASTERISK_ID, "KDOC_LEADING_ASTERISK")

    val TEXT: SyntaxElementType = registerElementType(TEXT_ID, "KDOC_TEXT")
    val CODE_BLOCK_TEXT: SyntaxElementType = registerElementType(CODE_BLOCK_TEXT_ID, "KDOC_CODE_BLOCK_TEXT")

    val TAG_NAME: SyntaxElementType = registerElementType(TAG_NAME_ID, "KDOC_TAG_NAME")
    val MARKDOWN_LINK: SyntaxElementType = registerElementType(MARKDOWN_LINK_ID, "KDOC_MARKDOWN_LINK")
    val MARKDOWN_ESCAPED_CHAR: SyntaxElementType = registerElementType(MARKDOWN_ESCAPED_CHAR_ID, "KDOC_MARKDOWN_ESCAPED_CHAR")

    val KDOC_LPAR: SyntaxElementType = registerElementType(KDOC_LPAR_ID, "KDOC_LPAR")
    val KDOC_RPAR: SyntaxElementType = registerElementType(KDOC_RPAR_ID, "KDOC_RPAR")
}