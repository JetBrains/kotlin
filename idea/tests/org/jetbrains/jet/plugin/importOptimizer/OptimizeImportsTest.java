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

package org.jetbrains.jet.plugin.importOptimizer;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.editor.importOptimizer.JetImportOptimizer;
import org.jetbrains.jet.testing.ConfigRuntimeUtil;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class OptimizeImportsTest extends LightCodeInsightTestCase {

    public void testAlreadyOptimized() throws Exception {
        doTest();
    }

    public void testRemoveImportsIfGeneral() throws Exception {
        doTest();
    }

    public void testSortImports() throws Exception {
        doTest();
    }

    public void testUnusedImports() throws Exception {
        doTestWithKotlinRuntime();
    }

    public void testWithAliases() throws Exception {
        doTest();
    }

    public void doTest() throws Exception {
        configureByFile(fileName());
        invokeFormatFile();
        checkResultByFile(null, checkFileName(), true);
    }

    public void doTestWithKotlinRuntime() {
        try {
            ConfigRuntimeUtil.configureKotlinRuntime(getModule(), getFullJavaJDK());

            configureByFile(fileName());
            invokeFormatFile();

            checkResultByFile(null, checkFileName(), false);
        }
        finally {
            ConfigRuntimeUtil.unConfigureKotlinRuntime(getModule(), getProjectJDK());
        }
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/editor/optimizeImports").getPath() +
               File.separator;
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }

    public String fileName() {
        return getTestName(false) + ".kt";
    }

    public String checkFileName() {
        return getTestName(false) + "_after.kt";
    }

    private static void invokeFormatFile() {
        CommandProcessor.getInstance().executeCommand(
            getProject(), new JetImportOptimizer().processFile(getFile()),
            "Optimize Imports", null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
    }
}