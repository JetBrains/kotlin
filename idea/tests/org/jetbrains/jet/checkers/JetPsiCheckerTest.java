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

package org.jetbrains.jet.checkers;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author abreslav
 */
public class JetPsiCheckerTest extends LightDaemonAnalyzerTestCase {
    private boolean checkInfos = false;
    private final String myDataPath;
    private final String myName;

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
        JetTestCaseBuilder.appendTestsInDirectory(PluginTestCaseBase.getTestDataPathBase(), "/checker/", false, JetTestCaseBuilder.emptyFilter, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetPsiCheckerTest(dataPath, name);
            }
        }, suite);
        JetTestCaseBuilder.appendTestsInDirectory(PluginTestCaseBase.getTestDataPathBase(), "/checker/regression/", false, JetTestCaseBuilder.emptyFilter, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetPsiCheckerTest(dataPath, name);
            }
        }, suite);
        JetTestCaseBuilder.appendTestsInDirectory(PluginTestCaseBase.getTestDataPathBase(), "/checker/infos/", false, JetTestCaseBuilder.emptyFilter, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetPsiCheckerTest(dataPath, name).setCheckInfos(true);
            }
        }, suite);
        return suite;
    }
}
