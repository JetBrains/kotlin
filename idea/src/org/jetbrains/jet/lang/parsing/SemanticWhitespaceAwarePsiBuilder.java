package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;

/**
 * @author abreslav
 */
public interface SemanticWhitespaceAwarePsiBuilder extends PsiBuilder {
    boolean newlineBeforeCurrentToken();
    void disableNewlines();
    void enableNewlines();
    void restoreNewlinesState();
}
