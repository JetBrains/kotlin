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

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public class ImportClassHelperTest extends LightDaemonAnalyzerTestCase {
    public void testDoNotImportIfGeneralExist() {
        testImportInFile("jettesting.data.testFunction");
    }

    public void testDoNotImportIfGeneralSpaceExist() {
        testImportInFile("jettesting.data.testFunction");
    }

    public void testNoDefaultImport() {
        testImportInFile("kotlin.io.println");
    }

    public void testImportBeforeObject() {
        testImportInFile("java.util.HashSet");
    }

    public void testInsertInEmptyFile() throws IOException {
        configureFromFileText("testInsertInEmptyFile.kt", "");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ImportClassHelper.addImportDirective("java.util.ArrayList", (JetFile) getFile());
            }
        });

        checkResultByText("import java.util.ArrayList");
    }

    public void testInsertInPackageOnlyFile() throws IOException {
        configureFromFileText("testInsertInPackageOnlyFile.kt", "package some");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ImportClassHelper.addImportDirective("java.util.ArrayList", (JetFile) getFile());
            }
        });

        checkResultByText("package some\n\nimport java.util.ArrayList");
    }

    public void testImportInFile(final String importString) {
        configureByFile(getTestName(false) + ".kt");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ImportClassHelper.addImportDirective(importString, (JetFile) getFile());
            }
        });
        checkResultByFile(getTestName(false) + ".kt.after");
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/quickfix/importHelper/";
    }
}
