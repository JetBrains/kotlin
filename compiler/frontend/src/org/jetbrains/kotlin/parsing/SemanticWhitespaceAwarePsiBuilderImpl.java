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

package org.jetbrains.kotlin.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.impl.PsiBuilderAdapter;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;

import static org.jetbrains.kotlin.lexer.KtTokens.*;

public class SemanticWhitespaceAwarePsiBuilderImpl extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {
    private final TokenSet complexTokens = TokenSet.create(SAFE_ACCESS, ELVIS, EXCLEXCL);
    private final Stack<Boolean> joinComplexTokens = new Stack<Boolean>();

    private final Stack<Boolean> newlinesEnabled = new Stack<Boolean>();

    private final PsiBuilderImpl delegateImpl;

    public SemanticWhitespaceAwarePsiBuilderImpl(PsiBuilder delegate) {
        super(delegate);
        newlinesEnabled.push(true);
        joinComplexTokens.push(true);

        delegateImpl = findPsiBuilderImpl(delegate);
    }

    @Nullable
    private static PsiBuilderImpl findPsiBuilderImpl(PsiBuilder builder) {
        // This is a hackish workaround for PsiBuilder interface not exposing isWhitespaceOrComment() method
        // We have to unwrap all the adapters to find an Impl inside
        while (true) {
            if (builder instanceof PsiBuilderImpl) {
                return (PsiBuilderImpl) builder;
            }
            if (!(builder instanceof PsiBuilderAdapter)) {
                return null;
            }

            builder = ((PsiBuilderAdapter) builder).getDelegate();
        }
    }

    @Override
    public boolean isWhitespaceOrComment(@NotNull IElementType elementType) {
        assert delegateImpl != null : "PsiBuilderImpl not found";
        return delegateImpl.whitespaceOrComment(elementType);
    }

    @Override
    public boolean newlineBeforeCurrentToken() {
        if (!newlinesEnabled.peek()) return false;

        if (eof()) return true;

        // TODO: maybe, memoize this somehow?
        for (int i = 1; i <= getCurrentOffset(); i++) {
            IElementType previousToken = rawLookup(-i);

            if (previousToken == KtTokens.BLOCK_COMMENT
                    || previousToken == KtTokens.DOC_COMMENT
                    || previousToken == KtTokens.EOL_COMMENT
                    || previousToken == SHEBANG_COMMENT) {
                continue;
            }

            if (previousToken != TokenType.WHITE_SPACE) {
                break;
            }

            int previousTokenStart = rawTokenTypeStart(-i);
            int previousTokenEnd = rawTokenTypeStart(-i + 1);

            assert previousTokenStart >= 0;
            assert previousTokenEnd < getOriginalText().length();

            for (int j = previousTokenStart; j < previousTokenEnd; j++) {
                if (getOriginalText().charAt(j) == '\n') {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void disableNewlines() {
        newlinesEnabled.push(false);
    }

    @Override
    public void enableNewlines() {
        newlinesEnabled.push(true);
    }

    @Override
    public void restoreNewlinesState() {
        assert newlinesEnabled.size() > 1;
        newlinesEnabled.pop();
    }

    private boolean joinComplexTokens() {
        return joinComplexTokens.peek();
    }

    @Override
    public void restoreJoiningComplexTokensState() {
        joinComplexTokens.pop();
    }

    @Override
    public void enableJoiningComplexTokens() {
        joinComplexTokens.push(true);
    }

    @Override
    public void disableJoiningComplexTokens() {
        joinComplexTokens.push(false);
    }

    @Override
    public IElementType getTokenType() {
        if (!joinComplexTokens()) return super.getTokenType();
        return getJoinedTokenType(super.getTokenType(), 1);
    }

    private IElementType getJoinedTokenType(IElementType rawTokenType, int rawLookupSteps) {
        if (rawTokenType == QUEST) {
            IElementType nextRawToken = rawLookup(rawLookupSteps);
            if (nextRawToken == DOT) return SAFE_ACCESS;
            if (nextRawToken == COLON) return ELVIS;
        }
        else if (rawTokenType == EXCL) {
            IElementType nextRawToken = rawLookup(rawLookupSteps);
            if (nextRawToken == EXCL) return EXCLEXCL;
        }
        return rawTokenType;
    }

    @Override
    public void advanceLexer() {
        if (!joinComplexTokens()) {
            super.advanceLexer();
            return;
        }
        IElementType tokenType = getTokenType();
        if (complexTokens.contains(tokenType)) {
            Marker mark = mark();
            super.advanceLexer();
            super.advanceLexer();
            mark.collapse(tokenType);
        }
        else {
            super.advanceLexer();
        }
    }

    @Override
    public String getTokenText() {
        if (!joinComplexTokens()) return super.getTokenText();
        IElementType tokenType = getTokenType();
        if (complexTokens.contains(tokenType)) {
                if (tokenType == ELVIS) return "?:";
                if (tokenType == SAFE_ACCESS) return "?.";
            }
        return super.getTokenText();
    }

    @Override
    public IElementType lookAhead(int steps) {
        if (!joinComplexTokens()) return super.lookAhead(steps);

        if (complexTokens.contains(getTokenType())) {
            return super.lookAhead(steps + 1);
        }
        return getJoinedTokenType(super.lookAhead(steps), 2);
    }
}
