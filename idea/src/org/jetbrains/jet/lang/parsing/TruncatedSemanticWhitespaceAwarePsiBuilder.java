package org.jetbrains.jet.lang.parsing;

import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
public class TruncatedSemanticWhitespaceAwarePsiBuilder extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {

    private final SemanticWhitespaceAwarePsiBuilder myBuilder;
    private final int myEOFPosition;


    public TruncatedSemanticWhitespaceAwarePsiBuilder(SemanticWhitespaceAwarePsiBuilder myBuilder, int eofPosition) {
        super(myBuilder);
        this.myBuilder = myBuilder;
        this.myEOFPosition = eofPosition;
    }

    @Override
    public boolean eolInLastWhitespace() {
        return myBuilder.eolInLastWhitespace();
    }

    @Override
    public void disableEols() {
        myBuilder.disableEols();;
    }

    @Override
    public void enableEols() {
        myBuilder.enableEols();
    }

    @Override
    public void restoreEolsState() {
        myBuilder.restoreEolsState();
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

}
