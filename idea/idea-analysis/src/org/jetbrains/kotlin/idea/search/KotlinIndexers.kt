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

package org.jetbrains.kotlin.idea.search

import com.intellij.lexer.Lexer
import com.intellij.psi.TokenType
import com.intellij.psi.impl.cache.impl.BaseFilterLexer
import com.intellij.psi.impl.cache.impl.IdAndToDoScannerBasedOnFilterLexer
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer
import com.intellij.psi.impl.cache.impl.todo.LexerBasedTodoIndexer
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.idea.search.usagesSearch.ALL_SEARCHABLE_OPERATIONS
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.JetLexer
import org.jetbrains.kotlin.lexer.JetTokens
import java.util.*

class KotlinFilterLexer(table: OccurrenceConsumer): BaseFilterLexer(JetLexer(), table) {
    private val codeTokens = TokenSet.orSet(
            TokenSet.create(*ALL_SEARCHABLE_OPERATIONS.toTypedArray()),
            TokenSet.create(JetTokens.IDENTIFIER, JetTokens.FIELD_IDENTIFIER)
    )

    private val commentTokens = TokenSet.orSet(JetTokens.COMMENTS, TokenSet.create(KDocTokens.KDOC))

    private val skipTokens = TokenSet.create(
            TokenType.WHITE_SPACE, JetTokens.RPAR, JetTokens.LBRACE, JetTokens.RBRACE,
            JetTokens.RBRACKET, JetTokens.SEMICOLON, JetTokens.COMMA, JetTokens.DOT
    )

    private val previousTokens = LinkedList<IElementType>()

    override fun advance() {
        fun isMultiDeclarationPosition(): Boolean {
            return previousTokens.firstOrNull() == JetTokens.VAL_KEYWORD
                   || previousTokens.firstOrNull() == JetTokens.VAR_KEYWORD
                   || previousTokens.size() == 2 && previousTokens[0] == JetTokens.LPAR && previousTokens[1] == JetTokens.FOR_KEYWORD
        }

        val tokenType = myDelegate.getTokenType()

        when (tokenType) {
            in codeTokens -> addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())
            JetTokens.LPAR -> if (isMultiDeclarationPosition()) addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())
            in JetTokens.STRINGS -> scanWordsInToken(UsageSearchContext.IN_STRINGS + UsageSearchContext.IN_FOREIGN_LANGUAGES, false, true)
            in commentTokens -> {
                scanWordsInToken(UsageSearchContext.IN_COMMENTS.toInt(), false, false)
                advanceTodoItemCountsInToken()
            }
            !in skipTokens -> scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT.toInt(), false, false)
        }

        if (tokenType != TokenType.WHITE_SPACE) {
            previousTokens.addFirst(tokenType)

            if (previousTokens.size() > 2) {
                previousTokens.removeLast()
            }
        }

        myDelegate.advance()
    }
}

class KotlinIdIndexer: LexerBasedIdIndexer() {
    override fun createLexer(consumer: OccurrenceConsumer): Lexer = KotlinFilterLexer(consumer)
}

class KotlinTodoIndexer: LexerBasedTodoIndexer(), IdAndToDoScannerBasedOnFilterLexer {
    override fun createLexer(consumer: OccurrenceConsumer): Lexer = KotlinFilterLexer(consumer)
}
