package org.jetbrains.jet.lang.parsing;


import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespaceSkippedCallback;
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
public class SemanticWitespaceAwarePsiBuilder extends PsiBuilderAdapter {
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
    private int myEOFPosition = -1;

    public SemanticWitespaceAwarePsiBuilder(final PsiBuilder delegate) {
        super(delegate);
        delegate.setWhitespaceSkippedCallback(myWhitespaceSkippedCallback);
    }

    @Override
    public void advanceLexer() {
         myEOLInLastWhitespace = false;
         super.advanceLexer();
     }

    public boolean eolInLastWhitespace() {
        return myEOLInLastWhitespace;
    }

    @Override
    public boolean eof() {
        if (super.eof()) return true;
        return myEOFPosition >= 0 && getCurrentOffset() >= myEOFPosition;
    }

    @Override
    public String getTokenText() {
        if (eof()) return null;
        return super.getTokenText();
    }

    @Override
    public IElementType getTokenType() {
        if (eof()) return null;
        return super.getTokenType();
    }

    public void setEOFPosition(int myEOFPosition) {
        this.myEOFPosition = myEOFPosition;
    }

    public void unSetEOFPosition() {
        this.myEOFPosition = -1;
    }
}
