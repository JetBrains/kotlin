package org.jetbrains.jet.completion;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author Nikolay Krasko
 */
public class JetInJavaCompletionTest extends JetCompletionMultiTestBase {

    public void testJetClassInJava() throws Exception {
        doFileTest();
    }

    public void testJetSubpackage() throws Exception {
        doFileTest();
    }

    // TODO: fix and uncomment
    public void skiptestTopLevelPackages() throws Exception {
        doFileTest();
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/completion/injava/";
    }

    @Override
    String[] getFileNameList() {
        String fileName = getTestName(false);
        return new String[]{fileName + ".java", fileName + ".kt"};
    }
}
