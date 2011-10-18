/*
 * @author max
 */
package org.jetbrains.jet.plugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JetFileType extends LanguageFileType {
    public static final JetFileType INSTANCE = new JetFileType();
    private final NotNullLazyValue<Icon> myIcon = new NotNullLazyValue<Icon>() {
        @NotNull
        @Override
        protected Icon compute() {
            return IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/kotlin16x16.png");
        }
    };

    private JetFileType() {
        super(JetLanguage.INSTANCE);
    }

    @NotNull
    public String getName() {
        return "jet";
    }

    @NotNull
    public String getDescription() {
        return "Kotlin";
    }

    @NotNull
    public String getDefaultExtension() {
        return "kt";
    }

    public Icon getIcon() {
        return myIcon.getValue();
    }

    @Override
    public boolean isJVMDebuggingSupported() {
        return true;
    }
}
