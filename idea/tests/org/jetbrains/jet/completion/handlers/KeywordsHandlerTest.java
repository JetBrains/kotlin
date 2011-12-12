package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.LightCompletionTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public class KeywordsHandlerTest extends LightCompletionTestCase {

    public void testSpaceAfter() throws IOException {
        configureFromFileText("Test.kt", "protecte<caret>");
        complete();
        checkResultByText("protected <caret>");
    }

    public void testNoSpaceAfter() throws IOException {
        configureFromFileText("Test.kt", "nul<caret>");
        complete();
        checkResultByText("null<caret>");
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/").getPath() +
               File.separator;
    }
}
