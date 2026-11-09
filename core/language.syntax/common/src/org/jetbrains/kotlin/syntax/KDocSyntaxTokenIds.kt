/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

public object KDocSyntaxTokenIds {
    public const val START: Int = KotlinSyntaxNodeTypeIds.BLOCK_CODE_FRAGMENT + 1
    public const val END: Int = START + 1
    public const val LEADING_ASTERISK: Int = END + 1
    public const val TEXT: Int = LEADING_ASTERISK + 1
    public const val CODE_BLOCK_TEXT: Int = TEXT + 1
    public const val TAG_NAME: Int = CODE_BLOCK_TEXT + 1
    public const val MARKDOWN_LINK: Int = TAG_NAME + 1
    public const val MARKDOWN_ESCAPED_CHAR: Int = MARKDOWN_LINK + 1
    public const val KDOC_LPAR: Int = MARKDOWN_ESCAPED_CHAR + 1
    public const val KDOC_RPAR: Int = KDOC_LPAR + 1
    public const val CODE_SPAN_TEXT: Int = KDOC_RPAR + 1
    /**
     * Remember to update first const in [KDocSyntaxNodeTypeIds] after adding new tokens here ^^^
     */
}
