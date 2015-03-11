/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.completion.handlers;

import org.jetbrains.kotlin.idea.KotlinCompletionTestCase;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;

import java.io.File;

public class CompletionMultifileHandlerTest extends KotlinCompletionTestCase {
    public void testExtensionFunctionImport() throws Exception {
        doTest();
    }

    public void testExtensionPropertyImport() throws Exception {
        doTest();
    }

    public void testImportAlreadyImportedObject() throws Exception {
        doTest();
    }

    public void testJetClassCompletionImport() throws Exception {
        doTest();
    }

    public void testTopLevelFunctionImport() throws Exception {
        doTest();
    }

    public void testTopLevelFunctionInQualifiedExpr() throws Exception {
        doTest();
    }

    public void testTopLevelPropertyImport() throws Exception {
        doTest();
    }

    public void testNoParenthesisInImports() throws Exception {
        doTest();
    }

    public void testKeywordExtensionFunctionName() throws Exception {
        doTest();
    }

    public void testInfixExtensionCallImport() throws Exception {
        doTest();
    }

    public void testClassWithClassObject() throws Exception {
        doTest();
    }

    public void testGlobalFunctionImportInLambda() throws Exception {
        doTest();
    }

    public void testObjectInStringTemplate() throws Exception {
        doTest();
    }

    public void doTest() throws Exception {
        String fileName = getTestName(false);

        configureByFiles(null, fileName + "-1.kt", fileName + "-2.kt");
        complete(2);
        if (myItems != null) {
            assertTrue("Multiple items in completion", myItems.length == 1);
            selectItem(myItems[0]);
        }
        checkResultByFile(fileName + ".kt.after");
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/multifile/").getPath() + File.separator;
    }
}
