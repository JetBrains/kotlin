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

package org.jetbrains.jet.completion;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author Nikolay Krasko
 */
public class JetMultifileBasicCompletionTest extends JetCompletionMultiTestBase {

    public void testDoNotCompleteWithConstraints() {
        doFileTest();
    }

    public void testTopLevelFunction() throws Exception {
        doFileTest();
    }

    public void todotestExtensionFunctionOnUnresolved() throws Exception {
        doFileTest();
    }

    public void testExtensionOnNullable() throws Exception {
        doFileTest();
    }

    public void todotestExtensionProperty() throws Exception {
        doFileTest();
    }

    public void testJavaInnerClasses() throws Exception {
        String fileName = getTestName(false);
        doFileTest(1, new String[] {fileName + ".kt", fileName + ".java"});
    }

    public void testNotImportedExtensionFunction() throws Exception {
        doFileTest();
    }

    public void testExtensionFunction() throws Exception {
        doFileTest(2);
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/completion/basic/multifile/";
    }

    @Override
    String[] getFileNameList() {
        String fileName = getTestName(false);
        return new String[]{fileName + "-1.kt", fileName + "-2.kt"};
    }
}
