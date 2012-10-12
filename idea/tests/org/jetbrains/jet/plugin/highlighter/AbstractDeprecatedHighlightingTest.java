package org.jetbrains.jet.plugin.highlighter;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public abstract class AbstractDeprecatedHighlightingTest extends LightDaemonAnalyzerTestCase {

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    protected void doTest(String filePath) throws Exception {
        doTest(getTestName(false) + ".kt", false, true);
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/highlighter/deprecated/";
    }

    public static void main(String[] args) throws IOException {
        String aPackage = "org.jetbrains.jet.plugin.highlighter";
        Class<AbstractDeprecatedHighlightingTest> thisClass = AbstractDeprecatedHighlightingTest.class;
        new TestGenerator(
                "idea/tests/",
                aPackage,
                "DeprecatedHighlightingTestGenerated",
                thisClass,
                Arrays.asList(
                        new SimpleTestClassModel(new File("idea/testData/highlighter/deprecated"), true, "kt", "doTest")
                ),
                thisClass
        ).generateAndSave();
    }

}