package org.jetbrains.jet.plugin.highlighter;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

public abstract class AbstractDeprecatedHighlightingTest extends LightDaemonAnalyzerTestCase {

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    protected void doTest(String filePath) throws Exception {
        doTest(filePath, true, false);
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}