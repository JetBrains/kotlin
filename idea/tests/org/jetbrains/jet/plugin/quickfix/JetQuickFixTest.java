/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.ConfigLibraryUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public class JetQuickFixTest extends LightQuickFixTestCase {
    private final String dataPath;
    private final String name;

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public JetQuickFixTest(String dataPath, String name) {
        this.dataPath = dataPath;
        this.name = name;
    }

    public static Test suite() {
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
        List<String> subDirs = Arrays.asList(dir.list());
        Collections.sort(subDirs);
        for (String subDirName : subDirs) {
            final TestSuite singleFileTestSuite = JetTestCaseBuilder
                    .suiteForDirectory(getTestDataPathBase(), subDirName, true, singleFileNameFilter, singleFileNamedTestFactory);
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
            ConfigLibraryUtil.configureKotlinRuntime(getModule(), getFullJavaJDK());
        }

        try {
            doSingleTest(name.substring("before".length()) + ".kt");
            checkAvailableActionsAreExpected();
            checkForUnexpectedErrors();
        }
        finally {
            if (isWithRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntime(getModule(), getProjectJDK());
            }
        }
    }

    public void checkAvailableActionsAreExpected() {
        List<IntentionAction> actions = getAvailableActions();
        final Pair<String, Boolean> pair = parseActionHintImpl(getFile(), getEditor().getDocument().getText());
        if (!pair.getSecond()) {
            // Action shouldn't be found. Check that other actions are expected and thus tested action isn't there under another name.
            QuickFixActionsUtils.checkAvailableActionsAreExpected((JetFile) getFile(), actions);
        }
    }

    public static void checkForUnexpectedErrors() {
        QuickFixActionsUtils.checkForUnexpectedErrors((JetFile) getFile());
    }

    @Override
    protected String getBasePath() {
        return "/quickfix/" + dataPath;
    }

    @NotNull
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
