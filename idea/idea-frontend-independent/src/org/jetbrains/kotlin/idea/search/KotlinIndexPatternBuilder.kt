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
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile

class KotlinIndexPatternBuilder : IndexPatternBuilderAdapter() {
    private companion object {
        private val TODO_COMMENT_TOKENS = TokenSet.orSet(KtTokens.COMMENTS, TokenSet.create(KDocTokens.KDOC))
    }

    override fun getCommentTokenSet(file: PsiFile): TokenSet? {
        return if (file is KtFile) TODO_COMMENT_TOKENS else null
    }

    override fun getIndexingLexer(file: PsiFile): Lexer? {
        return if (file is KtFile) KotlinLexer() else null
    }

    override fun getCommentStartDelta(tokenType: IElementType?): Int = 0

    override fun getCommentEndDelta(tokenType: IElementType?): Int = when (tokenType) {
        KtTokens.BLOCK_COMMENT -> "*/".length
        else -> 0
    }
}
