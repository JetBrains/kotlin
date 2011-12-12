package org.jetbrains.jet.completion;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class ExtensionsCompletionTest extends JetCompletionTestBase {

//    public ExtensionsCompletionTest() {
//        this("/completion/basic/extensions", "IrrelevantExtension");
//        // this("/completion/basic/extensions", "InvalidTypeParameters");
//        // this("/completion/basic/extensions", "ExtensionInExtensionThis");
//    }

    protected ExtensionsCompletionTest(@NotNull String path, @NotNull String name) {
        super(path, name);
    }

    @NotNull
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        JetTestCaseBuilder.appendTestsInDirectory(
                PluginTestCaseBase.getTestDataPathBase(), "/completion/basic/extensions", false,
                JetTestCaseBuilder.emptyFilter, new JetTestCaseBuilder.NamedTestFactory() {

            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new ExtensionsCompletionTest(dataPath, name);
            }
        }, suite);

        return suite;
    }
}
