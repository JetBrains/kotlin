package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

/**
 * @author Nikolay Krasko
 */
public class GlobalMemberCompletionContributor extends CompletionContributor {
    public GlobalMemberCompletionContributor() {
        extend(CompletionType.CLASS_NAME, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                   @Override
                   protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, 
                                                 @NotNull CompletionResultSet result) {
                       if (result.getPrefixMatcher().getPrefix().isEmpty()) {
                           return;
                       }

                       final PsiElement position = parameters.getPosition();
                       if (!(position.getContainingFile() instanceof JetFile)) {
                           return;
                       }

//                       final PsiElement parent = position.getParent();
//                       if (parent.getReference() instanceof JetSimpleName)

                       return;
                   }
               });
    }
}
