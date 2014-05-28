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

package org.jetbrains.jet.plugin;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class WordSelectionTest extends JetLightCodeInsightFixtureTestCase {
    public void testStatements() {
        doTest(11);
    }

    public void testWhenEntries() {
        doTest(6);
    }

    public void testTypeArguments() {
        doTest(1);
    }

    public void testValueArguments() {
        doTest(1);
    }

    public void testTypeParameters() {
        doTest(1);
    }

    public void testValueParameters() {
        doTest(1);
    }

    private void doTest(int howMany) {
        String testName = getTestName(false);
        String[] afterFiles = new String[howMany];
        for (int i = 1; i <= howMany; i++) {
            afterFiles[i - 1] = String.format("%s.%d.kt", testName, i);
        }

        CodeInsightTestUtil.doWordSelectionTest(myFixture, testName + ".kt", afterFiles);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_LATEST;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String testRelativeDir = "wordSelection";
        myFixture.setTestDataPath(new File(PluginTestCaseBase.getTestDataPathBase(), testRelativeDir).getPath() +
                                  File.separator);
    }

}
