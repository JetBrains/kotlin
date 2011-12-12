package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.LightCompletionTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class FunctionsHandlerTest extends LightCompletionTestCase {

    public void testNoParamsFunction() {
        configureByFile("NoParamsFunction.kt");
        checkResultByFile("NoParamsFunction.kt.after");
    }

    public void testFunctionWithParams() {
        configureByFile("ParamsFunction.kt");
        checkResultByFile("ParamsFunction.kt.after");
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/").getPath() +
               File.separator;
    }
}
