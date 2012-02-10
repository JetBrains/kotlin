package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class CompletionHandlerTest extends LightCompletionTestCase {

    public void testNoParamsFunction() {
        doTest();
    }

    public void testParamsFunction() {
        doTest();
    }

    public void testInsertJavaClassImport() {
        doTest();
    }

    public void testPropertiesSetter() {
        doTest();
    }

    public void testSingleBrackets() {
        configureByFile("SingleBrackets.kt");
        type('(');
        checkResultByFile("SingleBrackets.kt.after");
    }

    public void testExistingSingleBrackets() {
        doTest();
    }

    public void doTest() {
        String fileName = getTestName(false);
        try {
            configureByFileNoComplete(fileName + ".kt");
            complete(2);
            checkResultByFile(fileName + ".kt.after");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/").getPath() + File.separator;
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
}
