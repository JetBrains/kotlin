/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.parsing

import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.JetTokens
import com.intellij.openapi.util.text.StringUtil

object PrecedingCommentsBinder : WhitespacesAndCommentsBinder {

    override fun getEdgePosition(tokens: List<IElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter): Int {
        if (tokens.isEmpty()) return 0

        // 1. bind doc comment
        for (idx in tokens.indices.reversed()) {
            if (tokens[idx] == JetTokens.DOC_COMMENT) return idx
        }

        // 2. bind plain comments
        var result = tokens.size()
        @tokens for (idx in tokens.indices.reversed()) {
            val tokenType = tokens[idx]
            when (tokenType) {
                JetTokens.WHITE_SPACE -> if (StringUtil.getLineBreakCount(getter[idx]) > 1) break@tokens

                in JetTokens.COMMENTS -> {
                    if (idx == 0 || tokens[idx - 1] == JetTokens.WHITE_SPACE && StringUtil.containsLineBreak(getter[idx - 1])) {
                        result = idx
                    }
                }

                else ->  break@tokens
            }
        }

        return result
    }
}

object PrecedingDocCommentsBinder : WhitespacesAndCommentsBinder {

    override fun getEdgePosition(tokens: List<IElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter): Int {
        if (tokens.isEmpty()) return 0

        for (idx in tokens.indices.reversed()) {
            if (tokens[idx] == JetTokens.DOC_COMMENT) return idx
        }

        return tokens.size()
    }
}

// Binds comments on the same line
object TrailingCommentsBinder : WhitespacesAndCommentsBinder {

    override fun getEdgePosition(tokens: List<IElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter): Int {
        if (tokens.isEmpty()) return 0

        var result = 0
        @tokens for (idx in tokens.indices) {
            val tokenType = tokens[idx]
            when (tokenType) {
                JetTokens.WHITE_SPACE -> if (StringUtil.containsLineBreak(getter[idx])) break@tokens

                JetTokens.EOL_COMMENT, JetTokens.BLOCK_COMMENT -> result = idx + 1

                else -> break@tokens
            }
        }

        return result
    }
}
