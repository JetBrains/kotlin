package org.jetbrains.jet.lang.parsing;


import com.intellij.lang.PsiBuilder;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import java.util.Stack;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderImpl extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {
    private final Stack<Boolean> eolsEnabled = new Stack<Boolean>();

    public SemanticWhitespaceAwarePsiBuilderImpl(final PsiBuilder delegate) {
        super(delegate);
        eolsEnabled.push(true);
    }

    @Override
    public boolean eolInLastWhitespace() {
        if (!eolsEnabled.peek()) return false;
        if (eof()) return true;
        IElementType previousToken = rawLookup(-1);
        if (previousToken == TokenType.WHITE_SPACE) {
            int previousTokenStart = rawTokenTypeStart(-1);
            int previousTokenEnd = rawTokenTypeStart(0);
            assert previousTokenStart >= 0;
            assert previousTokenEnd < getOriginalText().length();
            for (int i = previousTokenStart; i < previousTokenEnd; i++) {
                if (getOriginalText().charAt(i) == '\n') return true;
            }
        }
        return false;
    }

    @Override
    public void disableEols() {
        eolsEnabled.push(false);
    }

    @Override
    public void enableEols() {
        eolsEnabled.push(true);
    }

    @Override
    public void restoreEolsState() {
        assert eolsEnabled.size() > 1;
        eolsEnabled.pop();
    }

}
