package org.jetbrains.jet.formatter;

import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class JetTypingIndentationTest extends LightCodeInsightTestCase {

    public void testConsecutiveCallsAfterDot() {
        doFileNewlineTest();
    }

    public void testDoInFun() {
        doFileNewlineTest();
    }

    // TODO
    public void enabletestEmptyParameters() {
        doFileNewlineTest();
    }

    public void testFor() {
        doFileNewlineTest();
    }

    // TODO
    public void enabletestFunctionBlock() {
        doFileNewlineTest();
    }

    public void testIf() {
        doFileNewlineTest();
    }

    // TODO
    public void enabletestNotFirstParameter() {
        doFileNewlineTest();
    }

    public void testWhile() {
        doFileNewlineTest();
    }

    public void doFileNewlineTest() {
        configureByFile(getTestName(false) + ".kt");
        type('\n');
        checkResultByFile(getTestName(false) + ".after.kt");
    }

    @Override
    protected String getTestDataPath() {

        final String testRelativeDir = "formatter/IndentationOnNewline";
        return new File(PluginTestCaseBase.getTestDataPathBase(), testRelativeDir).getPath() +
               File.separator;
    }
}
