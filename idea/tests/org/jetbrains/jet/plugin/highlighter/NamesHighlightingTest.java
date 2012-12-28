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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.highlighter.JetPsiChecker;

public class NamesHighlightingTest extends LightDaemonAnalyzerTestCase {
    public void testTypesAndAnnotations() throws Exception {
        doTest();
    }

    public void testVariables() throws Exception {
        doTest();
    }

    public void testFunctions() throws Exception {
        doTest();
    }

    public void testVariablesAsFunctions() throws Exception {
        doTest();
    }

    public void testJavaTypes() throws Exception {
        doTest();
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    private void doTest() throws Exception {
        doTest(getTestName(false) + ".kt", false, true);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        JetPsiChecker.setNamesHighlightingTest(true);
    }

    @Override
    public void tearDown() throws Exception {
        JetPsiChecker.setNamesHighlightingTest(false);
        super.tearDown();
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/highlighter/";
    }
}
