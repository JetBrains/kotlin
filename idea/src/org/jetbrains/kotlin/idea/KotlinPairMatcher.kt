/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KotlinPairMatcher implements PairedBraceMatcher {
    private final BracePair[] pairs = new BracePair[]{
            new BracePair(KtTokens.LPAR, KtTokens.RPAR, false),
            new BracePair(KtTokens.LONG_TEMPLATE_ENTRY_START, KtTokens.LONG_TEMPLATE_ENTRY_END, false),
            new BracePair(KtTokens.LBRACE, KtTokens.RBRACE, true),
            new BracePair(KtTokens.LBRACKET, KtTokens.RBRACKET, false),
    };

    @Override
    public BracePair[] getPairs() {
        return pairs;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        if (lbraceType.equals(KtTokens.LONG_TEMPLATE_ENTRY_START)) {
            // KotlinTypedHandler insert paired brace in this case
            return false;
        }

        return KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType)
               || contextType == KtTokens.SEMICOLON
               || contextType == KtTokens.COMMA
               || contextType == KtTokens.RPAR
               || contextType == KtTokens.RBRACKET
               || contextType == KtTokens.RBRACE
               || contextType == KtTokens.LBRACE
               || contextType == KtTokens.LONG_TEMPLATE_ENTRY_END;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }

}
