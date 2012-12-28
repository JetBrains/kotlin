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

public class JetInJavaCompletionTest extends JetCompletionMultiTestBase {

    public void testJetClassInJava() {
        doFileTest();
    }

    public void testJetSubpackage() {
        doFileTest();
    }

    public void testClassFromNamespace() {
        doFileTest();
    }

    public void testJetFunction() {
        doFileTest();
    }

    public void testTraitInJava() {
        doFileTest();
    }

    public void testJetEnums() {
        doFileTest();
    }

    public void testJetEnumFields() {
        doFileTest();
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/completion/injava/";
    }

    @Override
    String[] getFileNameList() {
        String fileName = getTestName(false);
        return new String[]{fileName + ".java", fileName + ".kt"};
    }
}
