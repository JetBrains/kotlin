package org.jetbrains.jet.completion;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author Nikolay Krasko
 */
public class JetMultifileBasicCompletionTest extends JetCompletionMultiTestBase {

    public void testTopLevelFunction() throws Exception {
        doFileTest(2);
    }

    public void testExtensionFunction() throws Exception {
        // TODO: fix and uncomment
        // doFileTest();
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/completion/basic/multifile/";
    }

    @Override
    String[] getFileNameList() {
        String fileName = getTestName(false);
        return new String[]{fileName + "-1.kt", fileName + "-2.kt"};
    }
}
