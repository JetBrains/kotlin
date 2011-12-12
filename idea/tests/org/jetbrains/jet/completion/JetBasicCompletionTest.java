package org.jetbrains.jet.completion;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay.Krasko
 */
public class JetBasicCompletionTest extends JetCompletionTestBase {

    protected JetBasicCompletionTest(@NotNull String path, @NotNull String name) {
        super(path, name);
    }

    @NotNull
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        JetTestCaseBuilder.appendTestsInDirectory(
                PluginTestCaseBase.getTestDataPathBase(), "/completion/basic/", false,
                JetTestCaseBuilder.emptyFilter, new JetTestCaseBuilder.NamedTestFactory() {

            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetBasicCompletionTest(dataPath, name);
            }
        }, suite);

        return suite;
    }
}
