package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author Nikolay Krasko
 */
public class ImportClassHelperTest extends LightDaemonAnalyzerTestCase {
    public void testDoNotImportIfGeneralExist() {
        configureByFile(getTestName(false) + ".kt");
        ImportClassHelper.addImportDirective("jettesting.data.testFunction", (JetFile) getFile());
        checkResultByFile(getTestName(false) + ".kt.after");
    }

    public void testDoNotImportIfGeneralSpaceExist() {
        configureByFile(getTestName(false) + ".kt");
        ImportClassHelper.addImportDirective("jettesting.data.testFunction", (JetFile) getFile());
        checkResultByFile(getTestName(false) + ".kt.after");
    }

    public void testNoDefaultImport() {
        configureByFile(getTestName(false) + ".kt");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ImportClassHelper.addImportDirective("std.io.println", (JetFile) getFile());
            }
        });
        checkResultByFile(getTestName(false) + ".kt.after");
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/quickfix/importHelper/";
    }
}
