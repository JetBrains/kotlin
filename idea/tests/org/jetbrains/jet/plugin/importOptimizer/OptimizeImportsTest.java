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

package org.jetbrains.jet.plugin.importOptimizer;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.editor.importOptimizer.JetImportOptimizer;

import java.io.File;

public class OptimizeImportsTest extends JetLightCodeInsightFixtureTestCase {
    public void testAlreadyOptimized() throws Exception {
        doTest();
    }

    public void testDefaultJsImports() throws Exception {
        doTest();
    }

    public void testRemoveImportsIfGeneral() throws Exception {
        doTest();
    }

    public void testRemoveImportsIfGeneralBefore() throws Exception {
        doTest();
    }

    public void testDuplicatedImports() throws Exception {
        doTest();
    }

    public void testUnusedImports() throws Exception {
        doTest();
    }

    public void testWithAliases() throws Exception {
        doTest();
    }

    public void testKt2488EnumEntry() throws Exception {
        doTest();
    }

    public void testKt1850FullQualified() throws Exception {
        doTest();
    }

    public void testKt1850InnerClass() throws Exception {
        doTest();
    }

    public void testPartiallyQualified() throws Exception {
        doTest();
    }

    public void testKt2709() throws Exception {
        doTest();
    }

    public void testSamConstructor() throws Exception {
        doTest();
    }

    public void doTest() throws Exception {
        myFixture.configureByFile(fileName());
        invokeOptimizeImports();
        myFixture.checkResultByFile(checkFileName(), false);
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/editor/optimizeImports").getPath() +
               File.separator;
    }

    public String checkFileName() {
        return getTestName(false) + "_after.kt";
    }

    private void invokeOptimizeImports() {
        CommandProcessor.getInstance().executeCommand(myFixture.getProject(), new JetImportOptimizer().processFile(myFixture.getFile()),
                "Optimize Imports", null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
    }
}
