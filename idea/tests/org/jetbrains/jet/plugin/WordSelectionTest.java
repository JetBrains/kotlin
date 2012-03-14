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

package org.jetbrains.jet.plugin;

import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Evgeny Gerashchenko
 * @since 2/24/12
 */
public class WordSelectionTest extends LightCodeInsightFixtureTestCase {
    public void testStatements() {
        doTest(11);
    }

    public void testWhenEntries() {
        doTest(6);
    }

    private void doTest(int howMany) {
        String testName = getTestName(false);
        String[] afterFiles = new String[howMany];
        for (int i = 1; i <= howMany; i++) {
            afterFiles[i - 1] = String.format("%s.%d.kt", testName, i);
        }

        CodeInsightTestUtil.doWordSelectionTest(myFixture, testName + ".kt", afterFiles);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final String testRelativeDir = "wordSelection";
        myFixture.setTestDataPath(new File(PluginTestCaseBase.getTestDataPathBase(), testRelativeDir).getPath() +
                                  File.separator);
    }

}
