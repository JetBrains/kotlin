/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package org.jetbrains.jet.plugin;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

public class JetPairMatcher implements PairedBraceMatcher {
    private final BracePair[] pairs = new BracePair[]{
            new BracePair(JetTokens.LPAR, JetTokens.RPAR, false),
            new BracePair(JetTokens.LONG_TEMPLATE_ENTRY_START, JetTokens.LONG_TEMPLATE_ENTRY_END, false),
            new BracePair(JetTokens.LBRACE, JetTokens.RBRACE, true),
            new BracePair(JetTokens.LBRACKET, JetTokens.RBRACKET, false)
    };

    public BracePair[] getPairs() {
        return pairs;
    }

    public boolean isPairedBracesAllowedBeforeType(@NotNull final IElementType lbraceType, @Nullable final IElementType contextType) {
        return JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType)
                || contextType == JetTokens.SEMICOLON
                || contextType == JetTokens.COMMA
                || contextType == JetTokens.RPAR
                || contextType == JetTokens.RBRACKET
                || contextType == JetTokens.RBRACE
                || contextType == JetTokens.LBRACE;
    }

    public int getCodeConstructStart(final PsiFile file, final int openingBraceOffset) {
        return openingBraceOffset;
    }

}
