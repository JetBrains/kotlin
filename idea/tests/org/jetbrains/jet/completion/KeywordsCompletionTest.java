package org.jetbrains.jet.completion;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * Test auto completion messages
 *
 * @author Nikolay.Krasko
 */
public class KeywordsCompletionTest extends JetCompletionTestBase {

    public void testAfterClassProperty() {
        doTest();
    }

    public void testAfterDot() {
        doTest();
    }

    public void testAfterSpaceAndDot() {
        doTest();
    }

    public void testclassObject() {
        doTest();
    }

    public void testInBlockComment() {
        doTest();
    }

    public void testInChar() {
        doTest();
    }

    public void testInClassBeforeFun() {
        doTest();
    }

    public void testInClassProperty() {
        doTest();
    }

    public void testInClassScope() {
        doTest();
    }

    public void testInFunctionScope() {
        doTest();
    }

    public void testInParametersList() {
        doTest();
    }

    public void testInMethodParametersList() {
        doTest();
    }

    public void testInString() {
        doTest();
    }

    public void testInTopProperty() {
        doTest();
    }

    public void testInTopScopeAfterPackage() {
        doTest();
    }

    public void testInTypeScope() {
        doTest();
    }

    public void testLineComment() {
        doTest();
    }

    public void testPropertySetterGetter() {
        doTest();
    }

    public void testTopScope() {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/keywords").getPath() +
               File.separator;
    }
}
