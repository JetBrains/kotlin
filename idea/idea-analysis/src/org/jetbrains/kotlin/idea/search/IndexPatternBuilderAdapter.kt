/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens

abstract class IndexPatternBuilderAdapter : IndexPatternBuilder {
    override fun getCommentStartDelta(tokenType: IElementType, tokenText: CharSequence): Int {
        return when (tokenType) {
            KtTokens.EOL_COMMENT -> 2
            KtTokens.BLOCK_COMMENT -> 2
            KtTokens.DOC_COMMENT -> 3
            else -> 0
        }
    }

    override fun getCharsAllowedInContinuationPrefix(tokenType: IElementType): String {
        return when (tokenType) {
            KtTokens.BLOCK_COMMENT -> "*"
            KtTokens.DOC_COMMENT -> "*"
            else -> ""
        }
    }
}