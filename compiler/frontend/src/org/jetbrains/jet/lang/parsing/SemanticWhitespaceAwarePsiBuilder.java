package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;

/**
 * @author abreslav
 */
public interface SemanticWhitespaceAwarePsiBuilder extends PsiBuilder {
    // TODO: comments go to wrong place when an empty element is created, see IElementType.isLeftBound()

    boolean newlineBeforeCurrentToken();
    void disableNewlines();
    void enableNewlines();
    void restoreNewlinesState();

    void restoreJoiningComplexTokensState();
    void enableJoiningComplexTokens();
    void disableJoiningComplexTokens();
}
