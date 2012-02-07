package org.jetbrains.jet.completion;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author Nikolay Krasko
 */
public class JetInJavaCompletionTest extends JetCompletionMultiTestBase {

    public void testJetClassInJava() {
        doFileTest();
    }

    public void testJetSubpackage() {
        doFileTest();
    }

    public void testClassFromNamespace() {
        doFileTest();
    }

    public void testJetFunction() {
        doFileTest();
    }

    public void testTraitInJava() {
        doFileTest();
    }

    public void testJetEnums() {
        doFileTest();
    }

    public void testJetEnumFields() {
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
