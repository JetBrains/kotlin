package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;

/**
 * @author abreslav
 */
public interface SemanticWhitespaceAwarePsiBuilder extends PsiBuilder {
    // TODO: Wrong name, should be something like "EOL before current token"
    boolean eolInLastWhitespace();
    void disableEols();
    void enableEols();
    void restoreEolsState();
}
