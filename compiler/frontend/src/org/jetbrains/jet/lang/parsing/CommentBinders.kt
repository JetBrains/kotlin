/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.parsing

import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.openapi.util.text.StringUtil

object PrecedingWhitespacesAndCommentsBinder : WhitespacesAndCommentsBinder {

    override fun getEdgePosition(tokens: List<IElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter): Int {
        if (tokens.size() == 0) return 0

        // 1. bind doc comment
        for (idx in tokens.indices.reversed()) {
            if (tokens.get(idx) == JetTokens.DOC_COMMENT) return idx
        }

        // 2. bind plain comments
        var result = tokens.size()
        for (idx in tokens.indices.reversed()) {
            val tokenType = tokens[idx]
            if (tokenType == JetTokens.WHITE_SPACE) {
                if (StringUtil.getLineBreakCount(getter[idx]) > 1) break
            }
            else if (tokenType in JetTokens.COMMENTS && tokenType != JetTokens.DOC_COMMENT) {
                if (atStreamEdge ||
                    idx == 0 ||
                    idx > 0 && tokens[idx - 1] == JetTokens.WHITE_SPACE && StringUtil.containsLineBreak(getter[idx - 1])) {
                    result = idx
                }
            }
            else {
                break
            }
        }

        return result
    }
}

// Binds comments on the same line
object TrailingWhitespacesAndCommentsBinder : WhitespacesAndCommentsBinder {

    override fun getEdgePosition(tokens: List<IElementType>, atStreamEdge: Boolean, getter: WhitespacesAndCommentsBinder.TokenTextGetter): Int {
        if (tokens.isEmpty()) return 0

        var result = 0
        for (idx in tokens.indices) {
            val tokenType = tokens.get(idx)
            if (tokenType == JetTokens.WHITE_SPACE) {
                if (StringUtil.containsLineBreak(getter.get(idx))) break
            }
            else if (tokenType in JetTokens.COMMENTS) {
                result = idx + 1
            }
            else {
                break
            }
        }

        return result
    }
}
