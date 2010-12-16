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
}
