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

package org.jetbrains.kotlin.kdoc.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public class KDocLexer extends MergingLexerAdapterBase {
    private final MergeFunction mergeFunction = new KDocLexerMergeFunction();

    public KDocLexer() {
        super(new FlexAdapter(new _KDocLexer(null)));
    }

    @Override
    public MergeFunction getMergeFunction() {
        return mergeFunction;
    }

    private static class KDocLexerMergeFunction implements MergeFunction {
        @Override
        public IElementType merge(IElementType type, Lexer originalLexer) {
            IElementType nextTokenType = originalLexer.getTokenType();
            String nextTokenText = originalLexer.getTokenText();
            if (type == KDocTokens.CODE_BLOCK_TEXT && nextTokenType == KDocTokens.TEXT && nextTokenText.matches("`{3,}|~{3,}")) {
                originalLexer.advance();
                return KDocTokens.TEXT; // Don't treat the trailing line as a part of a code block
            } else if (type == KDocTokens.CODE_BLOCK_TEXT || type == KDocTokens.CODE_SPAN_TEXT || type == KDocTokens.TEXT || type == TokenType.WHITE_SPACE) {
                while (type == originalLexer.getTokenType()) {
                    originalLexer.advance();
                }
            }

            return type;
        }
    }
}
