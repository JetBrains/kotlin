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

import com.google.common.collect.ImmutableSet
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
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.util.*

val KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT: Short = 0x20

private val ALL_SEARCHABLE_OPERATIONS: ImmutableSet<KtToken> = ImmutableSet
        .builder<KtToken>()
        .addAll(OperatorConventions.UNARY_OPERATION_NAMES.keys)
        .addAll(OperatorConventions.BINARY_OPERATION_NAMES.keys)
        .addAll(OperatorConventions.ASSIGNMENT_OPERATIONS.keys)
        .addAll(OperatorConventions.COMPARISON_OPERATIONS)
        .addAll(OperatorConventions.EQUALS_OPERATIONS)
        .addAll(OperatorConventions.IN_OPERATIONS)
        .add(KtTokens.LBRACKET)
        .add(KtTokens.BY_KEYWORD)
        .build()

class KotlinFilterLexer(private val occurrenceConsumer: OccurrenceConsumer): BaseFilterLexer(KotlinLexer(), occurrenceConsumer) {
    private val codeTokens = TokenSet.orSet(
            TokenSet.create(*ALL_SEARCHABLE_OPERATIONS.toTypedArray()),
            TokenSet.create(KtTokens.IDENTIFIER)
    )

    private val commentTokens = TokenSet.orSet(KtTokens.COMMENTS, TokenSet.create(KDocTokens.KDOC))

    private val MAX_PREV_TOKENS = 2
    private val prevTokens = ArrayDeque<IElementType>(MAX_PREV_TOKENS)
    private var prevTokenStart = -1
    private var prevTokenEnd = -1

    override fun advance() {
        val tokenType = myDelegate.tokenType

        when (tokenType) {
            KtTokens.EQ -> {
                if (prevTokens.peekFirst() == KtTokens.IDENTIFIER) {
                    val prevPrev = prevTokens.elementAtOrNull(1)
                    if (prevPrev == KtTokens.COMMA || prevPrev == KtTokens.LPAR) {
                        occurrenceConsumer.addOccurrence(bufferSequence, null, prevTokenStart, prevTokenEnd, KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT.toInt())
                    }
                }
            }

            KtTokens.LPAR -> {
                if (isMultiDeclarationPosition()) {
                    addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())
                }
            }

            KtTokens.IDENTIFIER -> {
                 if (myDelegate.tokenText.startsWith("`")) {
                     scanWordsInToken(UsageSearchContext.IN_CODE.toInt(), false, false)
                 }
                 else {
                     addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())
                 }
            }

            in codeTokens -> addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())

            in KtTokens.STRINGS -> scanWordsInToken(UsageSearchContext.IN_STRINGS + UsageSearchContext.IN_FOREIGN_LANGUAGES, false, true)

            in commentTokens -> {
                scanWordsInToken(UsageSearchContext.IN_COMMENTS.toInt(), false, false)
                advanceTodoItemCountsInToken()
            }
        }

        if (tokenType != TokenType.WHITE_SPACE && tokenType !in commentTokens) {
            if (prevTokens.size == MAX_PREV_TOKENS) {
                prevTokens.removeLast()
            }
            prevTokens.addFirst(tokenType)
            prevTokenStart = tokenStart
            prevTokenEnd = tokenEnd
        }

        myDelegate.advance()
    }

    private fun isMultiDeclarationPosition(): Boolean {
        val first = prevTokens.peekFirst()
        if (first == KtTokens.VAL_KEYWORD || first == KtTokens.VAR_KEYWORD) return true
        return first == KtTokens.LPAR && prevTokens.elementAtOrNull(1) == KtTokens.FOR_KEYWORD
    }
}

class KotlinIdIndexer: LexerBasedIdIndexer() {
    override fun createLexer(consumer: OccurrenceConsumer): Lexer = KotlinFilterLexer(consumer)

    override fun getVersion() = 3
}

class KotlinTodoIndexer: LexerBasedTodoIndexer(), IdAndToDoScannerBasedOnFilterLexer {
    override fun createLexer(consumer: OccurrenceConsumer): Lexer = KotlinFilterLexer(consumer)
}
