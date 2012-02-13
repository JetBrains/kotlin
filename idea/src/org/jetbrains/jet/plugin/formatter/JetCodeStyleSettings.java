package org.jetbrains.jet.plugin.formatter;

import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class JetCodeStyleSettings extends CustomCodeStyleSettings {
    public static JetCodeStyleSettings getInstance(Project project) {
        return CodeStyleSettingsManager.getSettings(project).getCustomSettings(JetCodeStyleSettings.class);
    }

    public JetCodeStyleSettings(CodeStyleSettings container) {
        super("JetCodeStyleSettings", container);
    }
}