package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;

/**
 * @author abreslav
 */
public interface SemanticWhitespaceAwarePsiBuilder extends PsiBuilder {
    boolean eolInLastWhitespace();
    void disableEols();
    void enableEols();
    void restoreEolsState();
}
