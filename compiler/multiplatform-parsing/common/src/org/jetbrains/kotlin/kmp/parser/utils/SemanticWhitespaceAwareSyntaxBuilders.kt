/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser.utils

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.element.SyntaxTokenTypes
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import fleet.com.intellij.platform.syntax.syntaxElementTypeSetOf
import fleet.com.intellij.platform.syntax.util.parser.SyntaxTreeBuilderAdapter
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.utils.Stack

internal interface SemanticWhitespaceAwareSyntaxBuilder : SyntaxTreeBuilder {
    fun newlineBeforeCurrentToken(): Boolean
    fun disableNewlines()
    fun enableNewlines()
    fun restoreNewlinesState()

    fun restoreJoiningComplexTokensState()
    fun enableJoiningComplexTokens()
    fun disableJoiningComplexTokens()

    override fun isWhitespaceOrComment(elementType: SyntaxElementType): Boolean
}

internal class SemanticWhitespaceAwareSyntaxBuilderImpl(val delegate: SyntaxTreeBuilder) : SyntaxTreeBuilderAdapter(delegate),
    SemanticWhitespaceAwareSyntaxBuilder {
    companion object {
        private val complexTokens = syntaxElementTypeSetOf(KtTokens.SAFE_ACCESS, KtTokens.ELVIS, KtTokens.EXCLEXCL)
        private val newlineBeforeCurrentTokenIgnoredSet =
            syntaxElementTypeSetOf(KtTokens.BLOCK_COMMENT, KtTokens.DOC_COMMENT, KtTokens.EOL_COMMENT, KtTokens.SHEBANG_COMMENT)
    }

    private val joinComplexTokensStack = Stack<Boolean>().apply { push(true) }
    private val newlinesEnabledStack = Stack<Boolean>().apply { push(true) }

    override fun isWhitespaceOrComment(elementType: SyntaxElementType): Boolean {
        return delegate.isWhitespaceOrComment(elementType)
    }

    override fun newlineBeforeCurrentToken(): Boolean {
        if (!newlinesEnabledStack.peek()) return false

        if (eof()) return true

        // TODO: maybe, memoize this somehow?
        for (i in 1..currentOffset) {
            val previousToken = rawLookup(-i)

            if (newlineBeforeCurrentTokenIgnoredSet.contains(previousToken)) {
                continue
            }

            if (previousToken !== SyntaxTokenTypes.WHITE_SPACE) {
                break
            }

            val previousTokenStart = rawTokenTypeStart(-i)
            val previousTokenEnd = rawTokenTypeStart(-i + 1)

            require(previousTokenStart >= 0)
            require(previousTokenEnd < text.length)

            for (j in previousTokenStart..<previousTokenEnd) {
                if (text[j] == '\n') {
                    return true
                }
            }
        }

        return false
    }

    override fun disableNewlines() {
        newlinesEnabledStack.push(false)
    }

    override fun enableNewlines() {
        newlinesEnabledStack.push(true)
    }

    override fun restoreNewlinesState() {
        require(newlinesEnabledStack.size > 1)
        newlinesEnabledStack.pop()
    }

    private fun joinComplexTokens(): Boolean {
        return joinComplexTokensStack.peek()
    }

    override fun restoreJoiningComplexTokensState() {
        joinComplexTokensStack.pop()
    }

    override fun enableJoiningComplexTokens() {
        joinComplexTokensStack.push(true)
    }

    override fun disableJoiningComplexTokens() {
        joinComplexTokensStack.push(false)
    }

    override val tokenType: SyntaxElementType?
        get() {
            if (!joinComplexTokens()) return super.tokenType
            return getJoinedTokenType(super.tokenType, 1)
        }

    private fun getJoinedTokenType(rawTokenType: SyntaxElementType?, rawLookupSteps: Int): SyntaxElementType? {
        if (rawTokenType === KtTokens.QUEST) {
            val nextRawToken = rawLookup(rawLookupSteps)
            if (nextRawToken === KtTokens.DOT) return KtTokens.SAFE_ACCESS
            if (nextRawToken === KtTokens.COLON) return KtTokens.ELVIS
        } else if (rawTokenType === KtTokens.EXCL) {
            val nextRawToken = rawLookup(rawLookupSteps)
            if (nextRawToken === KtTokens.EXCL) return KtTokens.EXCLEXCL
        }
        return rawTokenType
    }

    override fun advanceLexer() {
        if (!joinComplexTokens()) {
            super.advanceLexer()
            return
        }
        val tokenType = tokenType
        if (complexTokens.contains(tokenType)) {
            val mark = mark()
            super.advanceLexer()
            super.advanceLexer()
            mark.collapse(tokenType!!)
        } else {
            super.advanceLexer()
        }
    }

    override val tokenText: String?
        get() {
            if (!joinComplexTokens()) return super.tokenText
            val tokenType = tokenType
            if (complexTokens.contains(tokenType)) {
                if (tokenType === KtTokens.ELVIS) return "?:"
                if (tokenType === KtTokens.SAFE_ACCESS) return "?."
            }
            return super.tokenText
        }

    override fun lookAhead(steps: Int): SyntaxElementType? {
        if (!joinComplexTokens()) return super.lookAhead(steps)

        if (complexTokens.contains(tokenType)) {
            return super.lookAhead(steps + 1)
        }
        return getJoinedTokenType(super.lookAhead(steps), 2)
    }
}

internal class SemanticWhitespaceAwareSyntaxBuilderForByClause(val builder: SemanticWhitespaceAwareSyntaxBuilder) : SemanticWhitespaceAwareSyntaxBuilder by builder {
    var stackSize: Int = 0
        private set

    override fun disableNewlines() {
        builder.disableNewlines()
        stackSize++
    }

    override fun enableNewlines() {
        builder.enableNewlines()
        stackSize++
    }

    override fun restoreNewlinesState() {
        builder.restoreNewlinesState()
        stackSize--
    }
}

internal class TruncatedSemanticWhitespaceAwareSyntaxBuilder(val builder: SemanticWhitespaceAwareSyntaxBuilder, private val eofPosition: Int) :
    SemanticWhitespaceAwareSyntaxBuilder by builder {
    override fun eof(): Boolean {
        return builder.eof() || isOffsetBeyondEof(currentOffset)
    }

    override val tokenText: String?
        get() {
            if (eof()) return null
            return builder.tokenText
        }

    override val tokenType: SyntaxElementType?
        get() {
            if (eof()) return null
            return builder.tokenType
        }

    override fun lookAhead(steps: Int): SyntaxElementType? {
        if (eof()) return null

        val rawLookAheadSteps = rawLookAhead(steps)
        if (isOffsetBeyondEof(rawTokenTypeStart(rawLookAheadSteps))) return null

        return builder.rawLookup(rawLookAheadSteps)
    }

    private fun rawLookAhead(steps: Int): Int {
        // This code reproduces the behavior of SyntaxTreeBuilder.lookAhead(), but returns a number of raw steps instead of a token type
        // This is required for implementing truncated builder behavior
        var steps = steps
        var cur = 0
        while (steps > 0) {
            cur++

            var rawTokenType = rawLookup(cur)
            while (rawTokenType != null && isWhitespaceOrComment(rawTokenType)) {
                cur++
                rawTokenType = rawLookup(cur)
            }

            steps--
        }
        return cur
    }

    private fun isOffsetBeyondEof(offsetFromCurrent: Int): Boolean {
        return eofPosition in 0..offsetFromCurrent
    }
}