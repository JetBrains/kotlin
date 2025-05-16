/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser.utils

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.WhitespacesAndCommentsBinder
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.utils.StringUtil

internal object PrecedingCommentsBinder : WhitespacesAndCommentsBinder {
    override fun getEdgePosition(
        tokens: List<SyntaxElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter
    ): Int {
        if (tokens.isEmpty()) return 0

        // 1. bind doc comment
        for (idx in tokens.indices.reversed()) {
            if (tokens[idx] == KtTokens.DOC_COMMENT) return idx
        }

        // 2. bind plain comments
        var result = tokens.size
        tokens@ for (idx in tokens.indices.reversed()) {
            val tokenType = tokens[idx]
            when (tokenType) {
                KtTokens.WHITE_SPACE -> if (StringUtil.getLineBreakCount(getter.get(idx)) > 1) break@tokens

                in KtTokens.COMMENTS -> {
                    if (idx == 0 || tokens[idx - 1] == KtTokens.WHITE_SPACE && StringUtil.containsLineBreak(getter.get(idx - 1))) {
                        result = idx
                    }
                }

                else -> break@tokens
            }
        }

        return result
    }
}

internal object PrecedingDocCommentsBinder : WhitespacesAndCommentsBinder {
    override fun getEdgePosition(
        tokens: List<SyntaxElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter
    ): Int {
        if (tokens.isEmpty()) return 0

        for (idx in tokens.indices.reversed()) {
            if (tokens[idx] == KtTokens.DOC_COMMENT) return idx
        }

        return tokens.size
    }
}

// Binds comments on the same line
internal object TrailingCommentsBinder : WhitespacesAndCommentsBinder {
    override fun getEdgePosition(
        tokens: List<SyntaxElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter
    ): Int {
        if (tokens.isEmpty()) return 0

        var result = 0
        tokens@ for (idx in tokens.indices) {
            val tokenType = tokens[idx]
            when (tokenType) {
                KtTokens.WHITE_SPACE -> if (StringUtil.containsLineBreak(getter.get(idx))) break@tokens

                KtTokens.EOL_COMMENT, KtTokens.BLOCK_COMMENT -> result = idx + 1

                else -> break@tokens
            }
        }

        return result
    }
}

private class AllCommentsBinder(val isTrailing: Boolean) : WhitespacesAndCommentsBinder {
    override fun getEdgePosition(
        tokens: List<SyntaxElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter
    ): Int {
        if (tokens.isEmpty()) return 0

        val size = tokens.size

        // Skip one whitespace if needed. Expect that there can't be several consecutive whitespaces
        val endToken = tokens[if (isTrailing) size - 1 else 0]
        val shift = if (endToken == KtTokens.WHITE_SPACE) 1 else 0

        return if (isTrailing) size - shift else shift
    }
}

internal val PRECEDING_ALL_COMMENTS_BINDER: WhitespacesAndCommentsBinder = AllCommentsBinder(isTrailing = false)

internal val TRAILING_ALL_COMMENTS_BINDER: WhitespacesAndCommentsBinder = AllCommentsBinder(isTrailing = true)

internal object DoNotBindAnything : WhitespacesAndCommentsBinder {
    override fun getEdgePosition(
        tokens: List<SyntaxElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter
    ): Int {
        return 0
    }
}

internal object BindFirstShebangWithWhitespaceOnly : WhitespacesAndCommentsBinder {
    override fun getEdgePosition(
        tokens: List<SyntaxElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter
    ): Int {
        if (tokens.firstOrNull() == KtTokens.SHEBANG_COMMENT) {
            return if (tokens.getOrNull(1) == KtTokens.WHITE_SPACE) 2 else 1
        }

        return 0
    }
}

internal class BindAll(val isTrailing: Boolean) : WhitespacesAndCommentsBinder {
    override fun getEdgePosition(
        tokens: List<SyntaxElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter
    ): Int {
        return if (!isTrailing) 0 else tokens.size
    }
}

internal val PRECEDING_ALL_BINDER: WhitespacesAndCommentsBinder = BindAll(isTrailing = false)

internal val TRAILING_ALL_BINDER: WhitespacesAndCommentsBinder = BindAll(isTrailing = true)