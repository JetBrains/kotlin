package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * A keyword contributor for Kotlin
 * TODO: add different context for different keywords
 *
 * @author Nikolay Krasko
 */
public class JetKeywordCompletionContributor extends CompletionContributor {

    private static class JetKeywordCompletionProvider extends CompletionProvider<CompletionParameters> {
        
        private final static String[] COMPLETE_KEYWORD = new String[] {
                "namespace", "as", "type", "class", "this", "super", "val", "var", "fun", "for", "null", "true",
                "false", "is", "in", "throw", "return", "break", "continue", "object", "if", "try", "else", "while",
                "do", "when", "trait", "This"
        };
        
        private final static String[] COMPLETE_SOFT_KEYWORDS = new String[] {
                "import", "where", "by", "get", "set", "abstract", "enum", "open", "annotation", "override", "private",
                "public", "internal", "protected", "catch", "out", "vararg", "inline", "finally", "final", "ref"
        };

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      ProcessingContext context,
                                      @NotNull CompletionResultSet result) {

            for (String keyword : COMPLETE_KEYWORD) {
                result.addElement(LookupElementBuilder.create(keyword).setBold());
            }

            for (String softKeyword : COMPLETE_SOFT_KEYWORDS) {
                result.addElement(LookupElementBuilder.create(softKeyword));
            }
        }
    }

    public JetKeywordCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new JetKeywordCompletionProvider());
    }
}
