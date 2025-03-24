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

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.Stack
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.utils.strings.substringWithContext

/*package*/
abstract class AbstractKotlinParsing @JvmOverloads constructor(
    @JvmField protected val myBuilder: SemanticWhitespaceAwarePsiBuilder,
    @JvmField protected val isLazy: Boolean = true
) {
    protected val lastToken: IElementType?
        get() {
            var i = 1
            val currentOffset = myBuilder.getCurrentOffset()
            while (i <= currentOffset && KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-i))) {
                i++
            }
            return myBuilder.rawLookup(-i)
        }

    protected fun mark(): PsiBuilder.Marker {
        return myBuilder.mark()
    }

    protected fun error(message: String) {
        myBuilder.error(message)
    }

    protected fun expect(expectation: KtToken?, message: String, recoverySet: TokenSet? = null): Boolean {
        if (expect(expectation)) {
            return true
        }

        errorWithRecovery(message, recoverySet)

        return false
    }

    protected fun expect(expectation: KtToken?): Boolean {
        if (at(expectation)) {
            advance() // expectation
            return true
        }

        if (expectation === KtTokens.IDENTIFIER && "`" == myBuilder.getTokenText()) {
            advance()
        }

        return false
    }

    protected fun expectNoAdvance(expectation: KtToken?, message: String) {
        if (at(expectation)) {
            advance() // expectation
            return
        }

        error(message)
    }

    protected fun errorWithRecovery(message: String, recoverySet: TokenSet?) {
        val tt = tt()
        if (recoverySet == null ||
            recoverySet.contains(tt) || tt === KtTokens.LBRACE || tt === KtTokens.RBRACE ||
            (recoverySet.contains(KtTokens.EOL_OR_SEMICOLON) && (eof() || tt === KtTokens.SEMICOLON || myBuilder.newlineBeforeCurrentToken()))
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
        return myBuilder.eof()
    }

    protected fun advance() {
        // TODO: how to report errors on bad characters? (Other than highlighting)
        myBuilder.advanceLexer()
    }

    protected fun advance(advanceTokenCount: Int) {
        for (i in 0..<advanceTokenCount) {
            advance() // erroneous token
        }
    }

    protected fun advanceAt(current: IElementType?) {
        assert(_at(current))
        myBuilder.advanceLexer()
    }

    protected val tokenId: Int
        get() {
            val elementType = tt()
            return if (elementType is KtToken) elementType.tokenId else KtTokens.INVALID_Id
        }

    protected fun tt(): IElementType? {
        return myBuilder.tokenType
    }

    /**
     * Side-effect-free version of at()
     */
    protected fun _at(expectation: IElementType?): Boolean {
        val token = tt()
        return tokenMatches(token, expectation)
    }

    private fun tokenMatches(token: IElementType?, expectation: IElementType?): Boolean {
        if (token === expectation) return true
        if (expectation === KtTokens.EOL_OR_SEMICOLON) {
            if (eof()) return true
            if (token === KtTokens.SEMICOLON) return true
            if (myBuilder.newlineBeforeCurrentToken()) return true
        }
        return false
    }

    protected fun at(expectation: IElementType?): Boolean {
        if (_at(expectation)) return true
        val token = tt()
        if (token === KtTokens.IDENTIFIER && expectation is KtKeywordToken) {
            val expectedKeyword = expectation
            if (expectedKeyword.isSoft && expectedKeyword.getValue() == myBuilder.getTokenText()) {
                myBuilder.remapCurrentToken(expectation)
                return true
            }
        }
        if (expectation === KtTokens.IDENTIFIER && token is KtKeywordToken) {
            val keywordToken = token
            if (keywordToken.isSoft) {
                myBuilder.remapCurrentToken(KtTokens.IDENTIFIER)
                return true
            }
        }
        return false
    }

    /**
     * Side-effect-free version of atSet()
     */
    protected fun _atSet(set: TokenSet): Boolean {
        val token = tt()
        if (set.contains(token)) return true
        if (set.contains(KtTokens.EOL_OR_SEMICOLON)) {
            if (eof()) return true
            if (token === KtTokens.SEMICOLON) return true
            if (myBuilder.newlineBeforeCurrentToken()) return true
        }
        return false
    }

    protected fun atSet(set: TokenSet): Boolean {
        if (_atSet(set)) return true
        val token = tt()
        if (token === KtTokens.IDENTIFIER) {
            val keywordToken: KtKeywordToken? = SOFT_KEYWORD_TEXTS.get(myBuilder.getTokenText())
            if (keywordToken != null && set.contains(keywordToken)) {
                myBuilder.remapCurrentToken(keywordToken)
                return true
            }
        } else {
            // We know at this point that <code>set</code> does not contain <code>token</code>
            if (set.contains(KtTokens.IDENTIFIER) && token is KtKeywordToken) {
                if (token.isSoft()) {
                    myBuilder.remapCurrentToken(KtTokens.IDENTIFIER)
                    return true
                }
            }
        }
        return false
    }

    protected fun lookahead(k: Int): IElementType? {
        return myBuilder.lookAhead(k)
    }

    protected fun consumeIf(token: KtToken?): Boolean {
        if (at(token)) {
            advance() // token
            return true
        }
        return false
    }

    // TODO: Migrate to predicates
    protected fun skipUntil(tokenSet: TokenSet) {
        val stopAtEolOrSemi = tokenSet.contains(KtTokens.EOL_OR_SEMICOLON)
        while (!eof() && !tokenSet.contains(tt()) && !(stopAtEolOrSemi && at(KtTokens.EOL_OR_SEMICOLON))) {
            advance()
        }
    }

    protected fun errorUntil(message: String, tokenSet: TokenSet) {
        assert(tokenSet.contains(KtTokens.LBRACE)) { "Cannot include LBRACE into error element!" }
        assert(tokenSet.contains(KtTokens.RBRACE)) { "Cannot include RBRACE into error element!" }
        val error = mark()
        skipUntil(tokenSet)
        error.error(message)
    }

    protected inner class OptionalMarker(actuallyMark: Boolean) {
        private val marker: PsiBuilder.Marker?
        private val offset: Int

        init {
            marker = if (actuallyMark) mark() else null
            offset = myBuilder.getCurrentOffset()
        }

        fun done(elementType: IElementType) {
            if (marker == null) return
            marker.done(elementType)
        }

        fun error(message: String) {
            if (marker == null) return
            if (offset == myBuilder.getCurrentOffset()) {
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
        val opens = Stack<IElementType?>()
        var openAngleBrackets = 0
        var openBraces = 0
        var openParentheses = 0
        var openBrackets = 0
        while (!eof()) {
            if (pattern.processToken(
                    myBuilder.getCurrentOffset(),
                    pattern.isTopLevel(openAngleBrackets, openBrackets, openBraces, openParentheses)
                )
            ) {
                break
            }
            when (this.tokenId) {
                KtTokens.LPAR_Id -> {
                    openParentheses++
                    opens.push(KtTokens.LPAR)
                }
                KtTokens.LT_Id -> {
                    openAngleBrackets++
                    opens.push(KtTokens.LT)
                }
                KtTokens.LBRACE_Id -> {
                    openBraces++
                    opens.push(KtTokens.LBRACE)
                }
                KtTokens.LBRACKET_Id -> {
                    openBrackets++
                    opens.push(KtTokens.LBRACKET)
                }
                KtTokens.RPAR_Id -> {
                    openParentheses--
                    if (opens.isEmpty() || opens.pop() !== KtTokens.LPAR) {
                        if (pattern.handleUnmatchedClosing(KtTokens.RPAR)) {
                            break
                        }
                    }
                }
                KtTokens.GT_Id -> openAngleBrackets--
                KtTokens.RBRACE_Id -> openBraces--
                KtTokens.RBRACKET_Id -> openBrackets--
            }

            advance() // skip token
        }

        currentPosition.rollbackTo()

        return pattern.result()
    }

    protected fun eol(): Boolean {
        return myBuilder.newlineBeforeCurrentToken() || eof()
    }

    abstract fun create(builder: SemanticWhitespaceAwarePsiBuilder): KotlinParsing

    protected fun createTruncatedBuilder(eofPosition: Int): KotlinParsing? {
        return create(TruncatedSemanticWhitespaceAwarePsiBuilder(myBuilder, eofPosition))
    }

    protected inner class At @JvmOverloads constructor(private val lookFor: IElementType?, private val topLevelOnly: Boolean = true) :
        AbstractTokenStreamPredicate() {
        override fun matching(topLevel: Boolean): Boolean {
            return (topLevel || !topLevelOnly) && at(lookFor)
        }
    }

    protected inner class AtSet @JvmOverloads constructor(private val lookFor: TokenSet, private val topLevelOnly: TokenSet = lookFor) :
        AbstractTokenStreamPredicate() {
        override fun matching(topLevel: Boolean): Boolean {
            return (topLevel || !atSet(topLevelOnly)) && atSet(lookFor)
        }
    }

    @TestOnly
    fun currentContext(): String {
        return myBuilder.originalText.substringWithContext(myBuilder.getCurrentOffset(), myBuilder.getCurrentOffset(), 20)
    }

    companion object {
        private val SOFT_KEYWORD_TEXTS: MutableMap<String?, KtKeywordToken?> = HashMap<String?, KtKeywordToken?>()

        init {
            for (type in KtTokens.SOFT_KEYWORDS.getTypes()) {
                val keywordToken = type as KtKeywordToken
                assert(keywordToken.isSoft())
                SOFT_KEYWORD_TEXTS.put(keywordToken.getValue(), keywordToken)
            }
        }

        init {
            for (token in KtTokens.KEYWORDS.getTypes()) {
                assert(token is KtKeywordToken) { "Must be KtKeywordToken: " + token }
                assert(!(token as KtKeywordToken).isSoft()) { "Must not be soft: " + token }
            }
        }

        @JvmStatic
        protected fun errorIf(marker: PsiBuilder.Marker, condition: Boolean, message: String) {
            if (condition) {
                marker.error(message)
            } else {
                marker.drop()
            }
        }

        @JvmStatic
        protected fun closeDeclarationWithCommentBinders(
            marker: PsiBuilder.Marker,
            elementType: IElementType,
            precedingNonDocComments: Boolean
        ) {
            marker.done(elementType)
            marker.setCustomEdgeTokenBinders(
                if (precedingNonDocComments) PrecedingCommentsBinder else PrecedingDocCommentsBinder,
                TrailingCommentsBinder
            )
        }
    }
}
