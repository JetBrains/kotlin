/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

object KDocTokenId {
    const val START: SyntaxId = KtNodeTypeId.BLOCK_CODE_FRAGMENT + 1
    const val END: SyntaxId = START + 1
    const val LEADING_ASTERISK: SyntaxId = END + 1
    const val TEXT: SyntaxId = LEADING_ASTERISK + 1
    const val CODE_BLOCK_TEXT: SyntaxId = TEXT + 1
    const val TAG_NAME: SyntaxId = CODE_BLOCK_TEXT + 1
    const val MARKDOWN_LINK: SyntaxId = TAG_NAME + 1
    const val MARKDOWN_ESCAPED_CHAR: SyntaxId = MARKDOWN_LINK + 1
    const val KDOC_LPAR: SyntaxId = MARKDOWN_ESCAPED_CHAR + 1
    const val KDOC_RPAR: SyntaxId = KDOC_LPAR + 1
    const val CODE_SPAN_TEXT: SyntaxId = KDOC_RPAR + 1
    /**
     * Remember to update first const in [KDocNodeTypeId] after adding new tokens here ^^^
     */
}
