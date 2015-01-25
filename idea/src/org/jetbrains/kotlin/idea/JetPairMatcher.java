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

package org.jetbrains.kotlin.idea;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.JetTokens;

public class JetPairMatcher implements PairedBraceMatcher {
    private final BracePair[] pairs = new BracePair[]{
            new BracePair(JetTokens.LPAR, JetTokens.RPAR, false),
            new BracePair(JetTokens.LONG_TEMPLATE_ENTRY_START, JetTokens.LONG_TEMPLATE_ENTRY_END, false),
            new BracePair(JetTokens.LBRACE, JetTokens.RBRACE, true),
            new BracePair(JetTokens.LBRACKET, JetTokens.RBRACKET, false),
    };

    @Override
    public BracePair[] getPairs() {
        return pairs;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        if (lbraceType.equals(JetTokens.LONG_TEMPLATE_ENTRY_START)) {
            // KotlinTypedHandler insert paired brace in this case
            return false;
        }

        return  JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType)
                || contextType == JetTokens.SEMICOLON
                || contextType == JetTokens.COMMA
                || contextType == JetTokens.RPAR
                || contextType == JetTokens.RBRACKET
                || contextType == JetTokens.RBRACE
                || contextType == JetTokens.LBRACE
                || contextType == JetTokens.LONG_TEMPLATE_ENTRY_END;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }

}
