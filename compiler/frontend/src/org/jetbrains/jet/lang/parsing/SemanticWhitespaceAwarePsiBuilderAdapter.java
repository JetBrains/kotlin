package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.impl.PsiBuilderAdapter;

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
    public boolean newlineBeforeCurrentToken() {
        return myBuilder.newlineBeforeCurrentToken();
    }

    @Override
    public void disableNewlines() {
        myBuilder.disableNewlines();
    }

    @Override
    public void enableNewlines() {
        myBuilder.enableNewlines();
    }

    @Override
    public void restoreNewlinesState() {
        myBuilder.restoreNewlinesState();
    }

}
