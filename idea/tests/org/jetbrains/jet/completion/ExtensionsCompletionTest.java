package org.jetbrains.jet.completion;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class ExtensionsCompletionTest extends JetCompletionTestBase {

    public void testExtensionInExtendedClass() {
        doTest();
    }

    public void testExtensionInExtendedClassThis() {
        doTest();
    }

    public void testExtensionInExtension() {
        doTest();
    }

    public void testExtensionInExtensionThis() {
        doTest();
    }

    public void testInvalidTypeParameters() {
        doTest();
    }

    public void testIrrelevantExtension() {
        doTest();
    }

    public void testJavaTypeExtension() {
        doTest();
    }

    public void testKotlinGenericTypeExtension() {
        doTest();
    }

    public void testKotlinTypeExtension() {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/basic/extensions").getPath() +
               File.separator;
    }
}
