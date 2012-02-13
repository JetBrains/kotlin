package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.CompletionTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class CompletionMultifileHandlerTest extends CompletionTestCase {

    public void testTopLevelFunctionImport() {
        doTest();
    }

    public void testTopLevelFunctionInQualifiedExpr() {
        doTest();
    }

    public void testNoParenthesisInImports() {
        doTest();
    }

    public void doTest() {
        String fileName = getTestName(false);
        try {
            configureByFiles(null, fileName + "-1.kt", fileName + "-2.kt");
            complete(2);
            checkResultByFile(fileName + ".kt.after");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/multifile/").getPath() + File.separator;
    }
}
