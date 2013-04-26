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

package org.jetbrains.jet.plugin.navigation;

import com.intellij.codeInsight.CodeInsightTestCase;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

public class JetGotoImplementationMultifileTest extends CodeInsightTestCase {
    public void testImplementFunInJava() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementKotlinClassInJava() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementJavaClassInKotlin() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementMethodInKotlin() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementVarInJava() throws Exception {
        doKotlinJavaTest();
    }

    private void doKotlinJavaTest() throws Exception {
        doMultifileTest(getTestName(false) + ".kt", getTestName(false) + ".java");
    }

    private void doMultifileTest(String ... fileNames) throws Exception {
        configureByFiles(null, fileNames);
        GotoTargetHandler.GotoData gotoData = NavigationTestUtils.invokeGotoImplementations(getEditor(), getFile());
        NavigationTestUtils.assertGotoImplementations(getEditor(), gotoData);
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(),
                        "/navigation/implementations/multifile/" + getTestName(false)).getPath() + File.separator;
    }
}
