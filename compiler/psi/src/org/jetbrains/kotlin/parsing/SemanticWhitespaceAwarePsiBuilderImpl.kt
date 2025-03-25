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
import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.lexer.KtTokens

class SemanticWhitespaceAwarePsiBuilderImpl(delegate: PsiBuilder) : PsiBuilderAdapter(delegate), SemanticWhitespaceAwarePsiBuilder {
    private val complexTokens = TokenSet.create(KtTokens.SAFE_ACCESS, KtTokens.ELVIS, KtTokens.EXCLEXCL)
    private val joinComplexTokens = Stack<Boolean?>()

    private val newlinesEnabled = Stack<Boolean?>()

    private val delegateImpl: PsiBuilderImpl?

    init {
        newlinesEnabled.push(true)
        joinComplexTokens.push(true)

        delegateImpl = findPsiBuilderImpl(delegate)
    }

    override fun isWhitespaceOrComment(elementType: IElementType): Boolean {
        checkNotNull(delegateImpl) { "PsiBuilderImpl not found" }
        return delegateImpl.whitespaceOrComment(elementType)
    }

    override fun newlineBeforeCurrentToken(): Boolean {
        if (!newlinesEnabled.peek()!!) return false

        if (eof()) return true

        // TODO: maybe, memoize this somehow?
        for (i in 1..getCurrentOffset()) {
            val previousToken = rawLookup(-i)

            if (previousToken === KtTokens.BLOCK_COMMENT || previousToken === KtTokens.DOC_COMMENT || previousToken === KtTokens.EOL_COMMENT || previousToken === KtTokens.SHEBANG_COMMENT) {
                continue
            }

            if (previousToken !== TokenType.WHITE_SPACE) {
                break
            }

            val previousTokenStart = rawTokenTypeStart(-i)
            val previousTokenEnd = rawTokenTypeStart(-i + 1)

            assert(previousTokenStart >= 0)
            assert(previousTokenEnd < getOriginalText().length)

            for (j in previousTokenStart..<previousTokenEnd) {
                if (getOriginalText().get(j) == '\n') {
                    return true
                }
            }
        }

        return false
    }

    override fun disableNewlines() {
        newlinesEnabled.push(false)
    }

    override fun enableNewlines() {
        newlinesEnabled.push(true)
    }

    override fun restoreNewlinesState() {
        assert(newlinesEnabled.size > 1)
        newlinesEnabled.pop()
    }

    private fun joinComplexTokens(): Boolean {
        return joinComplexTokens.peek()!!
    }

    override fun restoreJoiningComplexTokensState() {
        joinComplexTokens.pop()
    }

    override fun enableJoiningComplexTokens() {
        joinComplexTokens.push(true)
    }

    override fun disableJoiningComplexTokens() {
        joinComplexTokens.push(false)
    }

    override fun getTokenType(): IElementType? {
        if (!joinComplexTokens()) return super.getTokenType()
        return getJoinedTokenType(super.getTokenType(), 1)
    }

    private fun getJoinedTokenType(rawTokenType: IElementType?, rawLookupSteps: Int): IElementType? {
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
        val tokenType = getTokenType()
        if (complexTokens.contains(tokenType)) {
            val mark = mark()
            super.advanceLexer()
            super.advanceLexer()
            mark.collapse(tokenType!!)
        } else {
            super.advanceLexer()
        }
    }

    override fun getTokenText(): String? {
        if (!joinComplexTokens()) return super.getTokenText()
        val tokenType = getTokenType()
        if (complexTokens.contains(tokenType)) {
            if (tokenType === KtTokens.ELVIS) return "?:"
            if (tokenType === KtTokens.SAFE_ACCESS) return "?."
        }
        return super.getTokenText()
    }

    override fun lookAhead(steps: Int): IElementType? {
        if (!joinComplexTokens()) return super.lookAhead(steps)

        if (complexTokens.contains(getTokenType())) {
            return super.lookAhead(steps + 1)
        }
        return getJoinedTokenType(super.lookAhead(steps), 2)
    }

    companion object {
        private fun findPsiBuilderImpl(builder: PsiBuilder?): PsiBuilderImpl? {
            // This is a hackish workaround for PsiBuilder interface not exposing isWhitespaceOrComment() method
            // We have to unwrap all the adapters to find an Impl inside
            var builder = builder
            while (true) {
                if (builder is PsiBuilderImpl) {
                    return builder
                }
                if (builder !is PsiBuilderAdapter) {
                    return null
                }

                builder = builder.getDelegate()
            }
        }
    }
}
