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
