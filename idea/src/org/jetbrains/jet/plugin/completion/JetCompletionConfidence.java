package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionConfidence extends CompletionConfidence {

    @NotNull
    @Override
    public ThreeState shouldFocusLookup(@NotNull CompletionParameters parameters) {
        return ThreeState.YES;
    }
}
