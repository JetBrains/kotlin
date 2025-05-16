/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser.utils

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.SyntaxElementTypeSet
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.utils.Stack

internal abstract class AbstractKotlinParsing(
    protected val builder: SemanticWhitespaceAwareSyntaxBuilder,
    protected val isLazy: Boolean = true
) {
    companion object {
        private const val DEBUG_MODE: Boolean = true

        internal fun errorIf(marker: SyntaxTreeBuilder.Marker, condition: Boolean, message: String) {
            if (condition) {
                marker.error(message)
            } else {
                marker.drop()
            }
        }

        internal fun closeDeclarationWithCommentBinders(
            marker: SyntaxTreeBuilder.Marker,
            elementType: SyntaxElementType,
            precedingNonDocComments: Boolean
        ) {
            marker.done(elementType)
            marker.setCustomEdgeTokenBinders(
                if (precedingNonDocComments) PrecedingCommentsBinder else PrecedingDocCommentsBinder,
                TrailingCommentsBinder
            )
        }
    }

    protected val lastToken: SyntaxElementType?
        get() {
            var index = 1
            val currentOffset = builder.currentOffset
            while (index <= currentOffset && KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-index))) {
                index++
            }
            return builder.rawLookup(-index)
        }

    protected fun mark(): SyntaxTreeBuilder.Marker {
        return builder.mark()
    }

    protected fun error(message: String) {
        builder.error(message)
    }

    protected fun expect(expectation: SyntaxElementType, message: String, recoverySet: SyntaxElementTypeSet? = null): Boolean {
        if (at(expectation)) {
            advance() // expectation
            return true
        }

        if (expectation === KtTokens.IDENTIFIER && "`" == builder.tokenText) {
            advance()
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    protected fun expectIdentifierWithRemap(message: String, recoverySet: SyntaxElementTypeSet? = null): Boolean {
        if (expectWithRemap(KtTokens.IDENTIFIER)) {
            return true
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    protected fun expectWithRemap(expectation: SyntaxElementType): Boolean {
        if (atWithRemap(expectation)) {
            advance() // expectation
            return true
        }

        if (expectation === KtTokens.IDENTIFIER && "`" == builder.tokenText) {
            advance()
        }

        return false
    }

    protected fun errorWithRecovery(message: String, recoverySet: SyntaxElementTypeSet?) {
        val tt = tt()
        if (recoverySet == null ||
            recoverySet.contains(tt) || tt === KtTokens.LBRACE || tt === KtTokens.RBRACE ||
            (recoverySet.contains(KtTokens.EOL_OR_SEMICOLON) &&
                    (eof() || tt === KtTokens.SEMICOLON || builder.newlineBeforeCurrentToken()))
        ) {
            error(message)
        } else {
            errorAndAdvance(message)
        }
    }

    protected fun errorAndAdvance(message: String, advanceTokenCount: Int = 1) {
        val err = mark()
        advance(advanceTokenCount)
        err.error(message)
    }

    protected fun eof(): Boolean {
        return builder.eof()
    }

    protected fun advance() {
        // TODO: how to report errors on bad characters? (Other than highlighting)
        builder.advanceLexer()
    }

    protected fun advance(advanceTokenCount: Int) {
        for (i in 0..<advanceTokenCount) {
            advance() // erroneous token
        }
    }

    protected val tokenId: Int
        get() = KtTokens.getElementTypeId(tt())

    protected fun tt(): SyntaxElementType? {
        return builder.tokenType
    }

    /**
     * Use with caution since it has a side effect and can change tokens in the stream (remap soft keywords and modifiers)
     */
    protected fun atWithRemap(expectation: SyntaxElementType): Boolean {
        if (atInternal(expectation)) return true
        if (tt() === KtTokens.IDENTIFIER) {
            if (DEBUG_MODE && expectation !in KtTokens.SOFT_KEYWORDS_AND_MODIFIERS) {
                error("Expectation is '${expectation}' but should be '${KtTokens.IDENTIFIER}' or soft keyword(modifier). Use '${::at.name}' call instead.")
            }

            if (expectation.toString() == builder.tokenText) {
                builder.remapCurrentToken(expectation)
                return true
            }
        }
        if (expectation === KtTokens.IDENTIFIER) {
            if (KtTokens.isSoftKeywordOrModifier(builder.tokenText)) {
                builder.remapCurrentToken(KtTokens.IDENTIFIER)
                return true
            }
        } else if (DEBUG_MODE && expectation !in KtTokens.SOFT_KEYWORDS_AND_MODIFIERS) {
            error("Expectation is '${expectation}' but should be '${KtTokens.IDENTIFIER}' or soft keyword(modifier). Use '${::at.name}' call instead.")
        }
        return false
    }

    /**
     * Side-effect-free version of at()
     */
    protected fun at(expectation: SyntaxElementType): Boolean {
        if (DEBUG_MODE && (expectation == KtTokens.IDENTIFIER || expectation in KtTokens.SOFT_KEYWORDS_AND_MODIFIERS)) {
            error("Expectation is '${expectation}' but should be hard keyword or token. Use '${::atWithRemap.name}' call instead.")
        }
        return atInternal(expectation)
    }

    private fun atInternal(expectation: SyntaxElementType): Boolean {
        val token = tt()
        if (token === expectation) return true
        if (expectation === KtTokens.EOL_OR_SEMICOLON) {
            if (eof()) return true
            if (token === KtTokens.SEMICOLON) return true
            if (builder.newlineBeforeCurrentToken()) return true
        }
        return false
    }

    protected fun atSetWithRemap(set: SyntaxElementTypeSet): Boolean {
        if (atSet(set)) return true
        if (tt() === KtTokens.IDENTIFIER) {
            val softKeywordToken = KtTokens.getSoftKeywordOrModifier(builder.tokenText)
            if (softKeywordToken?.let { set.contains(it) } == true) {
                builder.remapCurrentToken(softKeywordToken)
                return true
            }
        } else {
            // We know at this point that `set` does not contain `token`
            if (set.contains(KtTokens.IDENTIFIER) && KtTokens.isSoftKeywordOrModifier(builder.tokenText)) {
                builder.remapCurrentToken(KtTokens.IDENTIFIER)
                return true
            }
        }
        return false
    }

    /**
     * Side-effect-free version of atSet()
     */
    protected fun atSet(set: SyntaxElementTypeSet): Boolean {
        val token = tt()
        if (set.contains(token)) return true
        if (set.contains(KtTokens.EOL_OR_SEMICOLON)) {
            if (eof()) return true
            if (token === KtTokens.SEMICOLON) return true
            if (builder.newlineBeforeCurrentToken()) return true
        }
        return false
    }

    protected fun lookahead(k: Int): SyntaxElementType? {
        return builder.lookAhead(k)
    }

    protected fun consumeIfWithRemap(token: SyntaxElementType): Boolean {
        return if (atWithRemap(token)) {
            advance() // token
            true
        } else {
            false
        }
    }

    protected fun consumeIfSemicolon(): Boolean {
        return if (at(KtTokens.SEMICOLON)) {
            advance() // token
            true
        } else {
            false
        }
    }

    // TODO: Migrate to predicates
    protected fun skipUntil(elementTypeSet: SyntaxElementTypeSet) {
        val stopAtEolOrSemi = elementTypeSet.contains(KtTokens.EOL_OR_SEMICOLON)
        while (!eof() && !elementTypeSet.contains(tt()) && !(stopAtEolOrSemi && at(KtTokens.EOL_OR_SEMICOLON))) {
            advance()
        }
    }

    protected fun errorUntil(message: String, elementTypeSet: SyntaxElementTypeSet) {
        require(elementTypeSet.contains(KtTokens.LBRACE)) { "Cannot include LBRACE into error element!" }
        require(elementTypeSet.contains(KtTokens.RBRACE)) { "Cannot include RBRACE into error element!" }
        val error = mark()
        skipUntil(elementTypeSet)
        error.error(message)
    }

    protected inner class OptionalMarker(actuallyMark: Boolean) {
        private val marker: SyntaxTreeBuilder.Marker? = if (actuallyMark) mark() else null
        private val offset: Int = builder.currentOffset

        fun done(elementType: SyntaxElementType) {
            if (marker == null) return
            marker.done(elementType)
        }

        fun error(message: String) {
            if (marker == null) return
            if (offset == builder.currentOffset) {
                marker.drop() // no empty errors
            } else {
                marker.error(message)
            }
        }

        fun drop() {
            if (marker == null) return
            marker.drop()
        }
    }

    protected fun matchTokenStreamPredicate(pattern: TokenStreamPattern): Int {
        val currentPosition = mark()
        val opens = Stack<SyntaxElementType>()
        var openAngleBrackets = 0
        var openBraces = 0
        var openParentheses = 0
        var openBrackets = 0
        while (!eof()) {
            if (pattern.processToken(
                    builder.currentOffset,
                    pattern.isTopLevel(openAngleBrackets, openBrackets, openBraces, openParentheses)
                )
            ) {
                break
            }
            when (this.tokenId) {
                KtTokens.LPAR_ID -> {
                    openParentheses++
                    opens.push(KtTokens.LPAR)
                }
                KtTokens.LT_ID -> {
                    openAngleBrackets++
                    opens.push(KtTokens.LT)
                }
                KtTokens.LBRACE_ID -> {
                    openBraces++
                    opens.push(KtTokens.LBRACE)
                }
                KtTokens.LBRACKET_ID -> {
                    openBrackets++
                    opens.push(KtTokens.LBRACKET)
                }
                KtTokens.RPAR_ID -> {
                    openParentheses--
                    if (opens.isEmpty() || opens.pop() !== KtTokens.LPAR) {
                        if (pattern.handleUnmatchedClosing(KtTokens.RPAR)) {
                            break
                        }
                    }
                }
                KtTokens.GT_ID -> openAngleBrackets--
                KtTokens.RBRACE_ID -> openBraces--
                KtTokens.RBRACKET_ID -> openBrackets--
            }

            advance() // skip token
        }

        currentPosition.rollbackTo()

        return pattern.result()
    }

    protected fun eol(): Boolean {
        return builder.newlineBeforeCurrentToken() || eof()
    }

    abstract fun create(builder: SemanticWhitespaceAwareSyntaxBuilder): KotlinParsing

    protected fun createTruncatedBuilder(eofPosition: Int): KotlinParsing {
        return create(TruncatedSemanticWhitespaceAwareSyntaxBuilder(builder, eofPosition))
    }

    protected inner class AtSet(private val lookFor: SyntaxElementTypeSet, private val topLevelOnly: SyntaxElementTypeSet = lookFor) :
        AbstractTokenStreamPredicate() {
        override fun matching(topLevel: Boolean): Boolean {
            return (topLevel || !atSetWithRemap(topLevelOnly)) && atSetWithRemap(lookFor)
        }
    }
}
