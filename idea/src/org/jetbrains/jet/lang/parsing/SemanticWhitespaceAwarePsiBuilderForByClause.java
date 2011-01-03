package org.jetbrains.jet.lang.parsing;

import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderForByClause extends SemanticWhitespaceAwarePsiBuilderAdapter {

    private int stackSize = 0;

    public SemanticWhitespaceAwarePsiBuilderForByClause(SemanticWhitespaceAwarePsiBuilder builder) {
        super(builder);
    }

    @Override
    public void disableEols() {
        super.disableEols();
        stackSize++;
    }

    @Override
    public void enableEols() {
        super.enableEols();
        stackSize++;
    }

    @Override
    public void restoreEolsState() {
        super.restoreEolsState();
        stackSize--;
    }

    public int getStackSize() {
        return stackSize;
    }
}
