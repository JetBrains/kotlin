/*
 * @author max
 */
package org.jetbrains.jet.plugin;

import com.intellij.lang.Language;

public class JetLanguage extends Language {
    public static JetLanguage INSTANCE = new JetLanguage();

    private JetLanguage() {
        super("jet");
    }
}
