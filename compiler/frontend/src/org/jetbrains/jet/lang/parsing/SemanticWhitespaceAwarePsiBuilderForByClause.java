package org.jetbrains.jet.lang.parsing;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderForByClause extends SemanticWhitespaceAwarePsiBuilderAdapter {

    private int stackSize = 0;

    public SemanticWhitespaceAwarePsiBuilderForByClause(SemanticWhitespaceAwarePsiBuilder builder) {
        super(builder);
    }

    @Override
    public void disableNewlines() {
        super.disableNewlines();
        stackSize++;
    }

    @Override
    public void enableNewlines() {
        super.enableNewlines();
        stackSize++;
    }

    @Override
    public void restoreNewlinesState() {
        super.restoreNewlinesState();
        stackSize--;
    }

    public int getStackSize() {
        return stackSize;
    }
}
