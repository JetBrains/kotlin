/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KDocTokens : SyntaxElementTypesWithIds() {
    const val START_ID: Int = NO_ID + 1
    const val END_ID: Int = START_ID + 1
    const val LEADING_ASTERISK_ID: Int = END_ID + 1
    const val TEXT_ID: Int = LEADING_ASTERISK_ID + 1
    const val CODE_BLOCK_TEXT_ID: Int = TEXT_ID + 1
    const val TAG_NAME_ID: Int = CODE_BLOCK_TEXT_ID + 1
    const val MARKDOWN_LINK_ID: Int = TAG_NAME_ID + 1
    const val MARKDOWN_ESCAPED_CHAR_ID: Int = MARKDOWN_LINK_ID + 1
    const val KDOC_LPAR_ID: Int = MARKDOWN_ESCAPED_CHAR_ID + 1
    const val KDOC_RPAR_ID: Int = KDOC_LPAR_ID + 1
    // Remember to update the first ID constant in `KtTokens` after adding a new token

    val START: SyntaxElementType = register(START_ID, "KDOC_START")
    val END: SyntaxElementType = register(END_ID, "KDOC_END")
    val LEADING_ASTERISK: SyntaxElementType = register(LEADING_ASTERISK_ID, "KDOC_LEADING_ASTERISK")

    val TEXT: SyntaxElementType = register(TEXT_ID, "KDOC_TEXT")
    val CODE_BLOCK_TEXT: SyntaxElementType = register(CODE_BLOCK_TEXT_ID, "KDOC_CODE_BLOCK_TEXT")

    val TAG_NAME: SyntaxElementType = register(TAG_NAME_ID, "KDOC_TAG_NAME")
    val MARKDOWN_LINK: SyntaxElementType = register(MARKDOWN_LINK_ID, "KDOC_MARKDOWN_LINK")
    val MARKDOWN_ESCAPED_CHAR: SyntaxElementType = register(MARKDOWN_ESCAPED_CHAR_ID, "KDOC_MARKDOWN_ESCAPED_CHAR")

    val KDOC_LPAR: SyntaxElementType = register(KDOC_LPAR_ID, "KDOC_LPAR")
    val KDOC_RPAR: SyntaxElementType = register(KDOC_RPAR_ID, "KDOC_RPAR")
}