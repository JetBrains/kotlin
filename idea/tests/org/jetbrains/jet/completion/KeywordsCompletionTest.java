package org.jetbrains.jet.completion;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * Test auto completion messages
 *
 * @author Nikolay.Krasko
 */
public class KeywordsCompletionTest extends JetCompletionTestBase {

    protected KeywordsCompletionTest(@NotNull String path, @NotNull String name) {
        super(path, name);
    }

    @NotNull
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        JetTestCaseBuilder.appendTestsInDirectory(
                PluginTestCaseBase.getTestDataPathBase(), "/completion/keywords/", false,
                JetTestCaseBuilder.emptyFilter, new JetTestCaseBuilder.NamedTestFactory() {


            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new KeywordsCompletionTest(dataPath, name);
            }
        }, suite);

        return suite;
    }
}
