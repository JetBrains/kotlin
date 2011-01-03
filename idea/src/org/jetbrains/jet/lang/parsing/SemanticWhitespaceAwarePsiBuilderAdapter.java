package org.jetbrains.jet.lang.parsing;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderAdapter extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder {

    private final SemanticWhitespaceAwarePsiBuilder myBuilder;


    public SemanticWhitespaceAwarePsiBuilderAdapter(SemanticWhitespaceAwarePsiBuilder builder) {
        super(builder);
        this.myBuilder = builder;
    }

    @Override
    public boolean eolInLastWhitespace() {
        return myBuilder.eolInLastWhitespace();
    }

    @Override
    public void disableEols() {
        myBuilder.disableEols();
    }

    @Override
    public void enableEols() {
        myBuilder.enableEols();
    }

    @Override
    public void restoreEolsState() {
        myBuilder.restoreEolsState();
    }

}
