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

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.JetTokens

public class KotlinQuoteHandler : QuoteHandler {
    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        val tokenType = iterator.getTokenType()

        if (tokenType == JetTokens.CHARACTER_LITERAL) {
            val start = iterator.getStart()
            val end = iterator.getEnd()
            return end - start >= 1 && offset == end - 1
        }
        else if (tokenType == JetTokens.CLOSING_QUOTE) {
            return true
        }
        return false
    }

    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        val tokenType = iterator.getTokenType()

        if (tokenType == JetTokens.OPEN_QUOTE) {
            val start = iterator.getStart()
            return offset == start
        }
        return false
    }

    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        return true
    }

    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean {
        val tokenType = iterator.getTokenType()
        return tokenType == JetTokens.REGULAR_STRING_PART || tokenType == JetTokens.OPEN_QUOTE || tokenType == JetTokens.CLOSING_QUOTE || tokenType == JetTokens.SHORT_TEMPLATE_ENTRY_START || tokenType == JetTokens.LONG_TEMPLATE_ENTRY_END || tokenType == JetTokens.LONG_TEMPLATE_ENTRY_START
    }
}
