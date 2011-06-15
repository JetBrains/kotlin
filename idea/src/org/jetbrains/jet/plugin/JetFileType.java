/*
 * @author max
 */
package org.jetbrains.jet.plugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.util.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JetFileType extends LanguageFileType {
    public static final JetFileType INSTANCE = new JetFileType();

    private JetFileType() {
        super(JetLanguage.INSTANCE);
    }

    @NotNull
    public String getName() {
        return "jet";
    }

    @NotNull
    public String getDescription() {
        return "Jet Language";
    }

    @NotNull
    public String getDefaultExtension() {
        return "jet";
    }

    public Icon getIcon() {
        return Icons.PROJECT_ICON;
    }
}
