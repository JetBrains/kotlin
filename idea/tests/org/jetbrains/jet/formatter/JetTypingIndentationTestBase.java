package org.jetbrains.jet.formatter;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.SettingsConfigurator;

import java.io.File;

public abstract class JetTypingIndentationTestBase extends LightCodeInsightTestCase {

    public void doFileNewlineTest() {
        doNewlineTest(getBeforeFileName(), getAfterFileName());
    }

    public String getBeforeFileName() {
        return getTestName(false) + ".kt";
    }

    public String getAfterFileName() {
        return getTestName(false) + "_after.kt";
    }

    public String getInvertedAfterFileName() {
        return getTestName(false) + "_after_inv.kt";
    }

    public void doFileSettingNewLineTest() throws Exception {
        String originalFileText = FileUtil.loadFile(new File(getTestDataPath(), getBeforeFileName()), true);

        SettingsConfigurator configurator = JetFormatSettingsUtil.createConfigurator(originalFileText);

        configurator.configureSettings();
        doNewlineTest(getBeforeFileName(), getAfterFileName());

        configurator.configureInvertedSettings();
        doNewlineTest(getBeforeFileName(), getInvertedAfterFileName());

        getSettings().clearCodeStyleSettings();
    }

    private void doNewlineTest(String beforeFileName, String afterFileName) {
        configureByFile(beforeFileName);
        type('\n');
        checkResultByFile(afterFileName);
    }

    public static CodeStyleSettings getSettings() {
        return CodeStyleSettingsManager.getSettings(getProject());
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        String testRelativeDir = "indentationOnNewline";
        return new File(PluginTestCaseBase.getTestDataPathBase(), testRelativeDir).getPath() +
               File.separator;
    }
}
