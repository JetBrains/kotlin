package org.jetbrains.jet.completion;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay.Krasko
 */
public class JetBasicCompletionTest extends JetCompletionTestBase {

    public void testBasicAny() {
        doTest();
    }

    public void testPrintTest() {
        doTest();
    }

    public void testBasicInt() {
        doTest();
    }

    public void testBeforeDotInCall() {
        doTest();
    }

    public void testExtendClassName() {
        doTest();
    }

    public void testExtendQualifiedClassName() {
        doTest();
    }

    public void testFromImports() {
        doTest();
    }

    public void testInCallExpression() {
        doTest();
    }

    public void testInEmptyImport() {
        doTest();
    }

    public void testInImport() {
        doTest();
    }

    public void testInMiddleOfNamespace() {
        doTest();
    }

    public void testJavaClassNames() {
        doTest();
    }

    public void testJavaPackage() {
        doTest();
    }

    public void testNamedObject() {
        doTest();
    }

    public void testOverloadFunctions() {
        doTest();
    }

    public void testSubpackageInFun() {
        doTest();
    }

    public void testVariableClassName() {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/basic").getPath() +
               File.separator;
    }
}
