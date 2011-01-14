package org.jetbrains.jet.lang.parsing;


import com.intellij.lang.PsiBuilder;
import com.intellij.lang.java.parser.JavaParserUtil.PsiBuilderAdapter;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Stack;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderImpl extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {
    private final Stack<Boolean> newlinesEnabled = new Stack<Boolean>();

    public SemanticWhitespaceAwarePsiBuilderImpl(final PsiBuilder delegate) {
        super(delegate);
        newlinesEnabled.push(true);
    }

    @Override
    public boolean newlineBeforeCurrentToken() {
        if (!newlinesEnabled.peek()) return false;
        if (eof()) return true;
        // TODO: maybe, memoize this somehow?
        for (int i = 1; i <= getCurrentOffset(); i++) {
            IElementType previousToken = rawLookup(-i);
            if (previousToken == JetTokens.BLOCK_COMMENT
                    || previousToken == JetTokens.DOC_COMMENT
                    || previousToken == JetTokens.EOL_COMMENT) {
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
                if (getOriginalText().charAt(j) == '\n') return true;
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

}
