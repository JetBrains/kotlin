package org.jetbrains.jet.lang.parsing;


import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespaceSkippedCallback;
import com.intellij.psi.tree.IElementType;

import java.util.Stack;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderImpl extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {
    private final WhitespaceSkippedCallback myWhitespaceSkippedCallback = new WhitespaceSkippedCallback() {
        public void onSkip(IElementType type, int start, int end) {
            CharSequence whitespace = getOriginalText();
            for (int i = start; i < end; i++) {
                char c = whitespace.charAt(i);
                if (c == '\n') {
                    myEOLInLastWhitespace = true;
                    break;
                }
            }
        }
    };

    private boolean myEOLInLastWhitespace;
    private final Stack<Boolean> myEOLsEnabled = new Stack<Boolean>();

    public SemanticWhitespaceAwarePsiBuilderImpl(final PsiBuilder delegate) {
        super(delegate);
        delegate.setWhitespaceSkippedCallback(myWhitespaceSkippedCallback);
        myEOLsEnabled.push(true);
    }

    @Override
    public void advanceLexer() {
         myEOLInLastWhitespace = false;
         super.advanceLexer();
     }

    @Override
    public boolean eolInLastWhitespace() {
        return myEOLsEnabled.peek() && (myEOLInLastWhitespace || eof());
    }

    @Override
    public void disableEols() {
        myEOLsEnabled.push(false);
    }

    @Override
    public void enableEols() {
        myEOLsEnabled.push(true);
    }

    @Override
    public void restoreEolsState() {
        assert myEOLsEnabled.size() > 1;
        myEOLsEnabled.pop();
    }

    // TODO: Overhead
    @Override
    public Marker mark() {
        return new MarkerAdapter(super.mark()) {
            private final boolean eolInLastWhitespace = eolInLastWhitespace();

            @Override
            public void rollbackTo() {
                super.rollbackTo();
                myEOLInLastWhitespace = eolInLastWhitespace;
            }
        };
    }
}
