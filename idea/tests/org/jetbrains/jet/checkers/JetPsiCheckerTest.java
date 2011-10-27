package org.jetbrains.jet.checkers;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.quickfix.PluginTestCaseBase;

import java.io.File;

/**
 * @author abreslav
 */
public class JetPsiCheckerTest extends LightDaemonAnalyzerTestCase {
    private boolean checkInfos = false;
    private String myDataPath;
    private String myName;

    public JetPsiCheckerTest(String dataPath, String name) {
        myDataPath = dataPath;
        myName = name;
    }

    @Override
    protected void runTest() throws Throwable {
        doTest(getTestFilePath(), true, checkInfos);
    }

    @NotNull
    protected String getTestFilePath() {
        return myDataPath + File.separator + myName + ".jet";
    }

    public final JetPsiCheckerTest setCheckInfos(boolean checkInfos) {
        this.checkInfos = checkInfos;
        return this;
    }
    
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @Override
    public String getName() {
        return "test" + myName;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(PluginTestCaseBase.getTestDataPathBase(), "/checker/", false, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetPsiCheckerTest(dataPath, name);
            }
        }));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(PluginTestCaseBase.getTestDataPathBase(), "/checker/regression/", false, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetPsiCheckerTest(dataPath, name);
            }
        }));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(PluginTestCaseBase.getTestDataPathBase(), "/checker/infos/", false, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetPsiCheckerTest(dataPath, name).setCheckInfos(true);
            }
        }));
        return suite;
    }
}
