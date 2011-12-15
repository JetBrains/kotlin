package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author svtk
 */
public class JetQuickFixTest extends LightQuickFixTestCase {
    private final String dataPath;
    private final String name;
    private static FilenameFilter quickFixTestsFilter;

    public JetQuickFixTest(String dataPath, String name) {
        this.dataPath = dataPath;
        this.name = name;
    }

    private static void setFilter() {
        final ArrayList<String> appropriateDirs = Lists.newArrayList("classImport", "expressions");
        quickFixTestsFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (appropriateDirs.contains(s)) return true;
                return false;
            }
        };
    }

    public static Test suite() {
        //setFilter(); //to launch only part of tests
        TestSuite suite = new TestSuite();
        FilenameFilter fileNameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (s.startsWith("before")) return true;
                return false;
            }
        };
        JetTestCaseBuilder.NamedTestFactory namedTestFactory = new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetQuickFixTest(dataPath, name);
            }
        };
        File dir = new File(getTestDataPathBase());
        List<String> subDirs = Arrays.asList(quickFixTestsFilter != null ? dir.list(quickFixTestsFilter) : dir.list());
        Collections.sort(subDirs);
        for (String subDirName : subDirs) {
            suite.addTest(JetTestCaseBuilder.suiteForDirectory(getTestDataPathBase(), subDirName, true, fileNameFilter, namedTestFactory));

        }
        return suite;
    }

    public static String getTestDataPathBase() {
        return JetTestCaseBuilder.getHomeDirectory() + "/idea/testData/quickfix/";
    }

    public String getName() {
        return "test" + name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toUpperCase());
    }

    @Override
    protected void runTest() throws Throwable {
        doSingleTest(name.substring("before".length()) + ".kt");
    }

    @Override
    protected String getBasePath() {
        return "/quickfix/" + dataPath;
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
}
