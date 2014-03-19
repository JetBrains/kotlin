package org.jetbrains.jet.formatter;

public class JetScriptTypingIndentationTest extends JetTypingIndentationTestBase {
    public void testScriptAfterFun() {
        doFileNewlineTest();
    }

    public void testScriptAfterImport() {
        doFileNewlineTest();
    }

    public void testScriptAfterExpression() {
        doFileNewlineTest();
    }

    public void testScriptInsideFun() {
        doFileNewlineTest();
    }

    @Override
    public String getBeforeFileName() {
        return getTestName(false) + ".kts";
    }

    @Override
    public String getAfterFileName() {
        return getTestName(false) + "_after.kts";
    }

}
