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

import com.intellij.codeInsight.CodeInsightTestCase;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.editor.importOptimizer.JetImportOptimizer;

import java.io.File;

public class OptimizeImportsMultiFileTest extends CodeInsightTestCase {
    
    public void testKotlinPackage() throws Exception {
        doTest(getTestName(false) + "/main.kt", getTestName(false) + "/kotlinClass.kt");
    }

    public void testJavaStaticField() throws Exception {
        doTest(getTestName(false) + "/main.kt", getTestName(false) + "/MyJavaClass.java");
    }

    public void testArrayAccessExpression() throws Exception {
        doTest(getTestName(false) + "/main.kt", getTestName(false) + "/myClass.kt");
    }

    public void testInvokeFunction() throws Exception {
        doTest(getTestName(false) + "/main.kt", getTestName(false) + "/myClass.kt");
    }

    public void doTest(String... fileNames) throws Exception {
        configureByFiles(null, fileNames);
        invokeFormatFile();
        checkResultByFile(checkFileName(), true);
    }

    public String checkFileName() {
        return getTestName(false) + "/" + getTestName(false) + "_after.kt";
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/editor/optimizeImports/multifile").getPath() +
               File.separator;
    }

    private void invokeFormatFile() {
        CommandProcessor.getInstance().executeCommand(
                getProject(), new JetImportOptimizer().processFile(getFile()),
                "Optimize Imports", null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
    }

}
