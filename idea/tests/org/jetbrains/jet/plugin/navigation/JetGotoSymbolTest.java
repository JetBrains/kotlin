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

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

public class JetGotoSymbolTest extends JetLightCodeInsightFixtureTestCase {
    public void testProperties() {
        doTest();
    }

    public void testFunctions() {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/gotoSymbol").getPath() + File.separator;
    }

    protected void doTest() {
        myFixture.configureByFile(fileName());
        NavigationTestUtils.assertGotoSymbol(getProject(), myFixture.getEditor());
    }

    @Override
    protected String fileName() {
        return getTestName(true) + ".kt";
    }
}
