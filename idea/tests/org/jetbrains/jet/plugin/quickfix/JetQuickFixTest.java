/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.highlighter.IdeErrorMessages;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.testing.ConfigRuntimeUtil;
import org.jetbrains.jet.testing.InTextDirectivesUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * @author svtk
 */
@SuppressWarnings("JUnitTestCaseWithNoTests")
public class JetQuickFixTest extends LightQuickFixTestCase {
    private final String dataPath;
    private final String name;
    private static FilenameFilter quickFixTestsFilter;

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public JetQuickFixTest(String dataPath, String name) {
        this.dataPath = dataPath;
        this.name = name;
    }

    @SuppressWarnings("UnusedDeclaration")
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

        FilenameFilter singleFileNameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith("before") && !JetPsiCheckerMultifileTest.isMultiFileName(s);
            }
        };

        JetTestCaseBuilder.NamedTestFactory singleFileNamedTestFactory = new JetTestCaseBuilder.NamedTestFactory() {
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
            final TestSuite singleFileTestSuite = JetTestCaseBuilder.suiteForDirectory(getTestDataPathBase(), subDirName, true, singleFileNameFilter, singleFileNamedTestFactory);
            if (singleFileTestSuite.countTestCases() != 0) {
                suite.addTest(singleFileTestSuite);
            }
        }
        return suite;
    }

    public static String getTestDataPathBase() {
        return JetTestCaseBuilder.getHomeDirectory() + "/idea/testData/quickfix/";
    }

    @Override
    public String getName() {
        return "test" + name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toUpperCase());
    }

    @Override
    protected void runTest() throws Throwable {
        boolean isWithRuntime = name.endsWith("Runtime");

        if (isWithRuntime) {
            ConfigRuntimeUtil.configureKotlinRuntime(getModule(), getFullJavaJDK());
        }

        try {
            doSingleTest(name.substring("before".length()) + ".kt");
            checkForUnexpectedErrors();
        } finally {
            if (isWithRuntime) {
                ConfigRuntimeUtil.unConfigureKotlinRuntime(getModule(), getProjectJDK());
            }
        }
    }

    private static void checkForUnexpectedErrors() {
        AnalyzeExhaust exhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) getFile());
        Collection<Diagnostic> diagnostics = exhaust.getBindingContext().getDiagnostics();

        if (diagnostics.size() != 0) {
            String[] expectedErrorStrings = InTextDirectivesUtils.findListWithPrefix("// ERROR:", getFile().getText());

            System.out.println(getFile().getText());

            Collection<String> expectedErrors = new HashSet<String>(Arrays.asList(expectedErrorStrings));

            StringBuilder builder = new StringBuilder();
            boolean hasErrors = false;

            for (Diagnostic diagnostic : diagnostics) {
                if (diagnostic.getSeverity() == Severity.ERROR) {
                    String errorText = IdeErrorMessages.RENDERER.render(diagnostic);
                    if (!expectedErrors.contains(errorText)) {
                        hasErrors = true;
                        builder.append("// ERROR: ").append(errorText).append("\n");
                    }
                }
            }

            Assert.assertFalse("There should be no unexpected errors after applying fix (Use \"// ERROR:\" directive): \n" + builder.toString(), hasErrors);
        }
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

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }
}
