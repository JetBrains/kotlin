package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.CompletionTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class JavaCompletionHandlerTest extends CompletionTestCase {

    public void testClassAutoImport() {
        doTest();
    }

    public void doTest() {
        String fileName = getTestName(false);
        try {
            configureByFiles(null, fileName + ".java", fileName + ".kt");
            complete(2);
            checkResultByFile(fileName + ".after.java");
        } catch (@SuppressWarnings("CaughtExceptionImmediatelyRethrown") AssertionError assertionError) {
            throw assertionError;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/injava/handlers/").getPath() + File.separator;
    }
}