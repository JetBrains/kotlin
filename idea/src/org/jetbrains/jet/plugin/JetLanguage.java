/*
 * @author max
 */
package org.jetbrains.jet.plugin;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import org.jetbrains.annotations.NotNull;

public class JetLanguage extends Language {
    public static JetLanguage INSTANCE = new JetLanguage();

    private JetLanguage() {
        super("jet");

        SyntaxHighlighterFactory.LANGUAGE_FACTORY.addExplicitExtension(this, new SingleLazyInstanceSyntaxHighlighterFactory() {
          @NotNull
          protected SyntaxHighlighter createHighlighter() {
            return new JetHighlighter();
          }
        });
    }
}
