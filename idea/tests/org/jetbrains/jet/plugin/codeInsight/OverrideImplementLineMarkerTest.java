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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.List;

public class OverrideImplementLineMarkerTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getBasePath() {
        return PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE + "/codeInsight/lineMarker";
    }

    public void testTrait() throws Throwable {
        doSimpleTest(2);
    }

    public void testClass() throws Throwable {
        doSimpleTest(2);
    }

    private void doSimpleTest(int count) throws Throwable {
        myFixture.configureByFile(getTestName(false) + ".kt");
        doTest(count);
    }

    private void doTest(int count) {
        Editor editor = myFixture.getEditor();
        Project project = myFixture.getProject();

        myFixture.doHighlighting();

        List<LineMarkerInfo> infoList = DaemonCodeAnalyzerImpl.getLineMarkers(editor.getDocument(), project);
        assertNotNull(infoList);
        assertEquals(count, infoList.size());
    }
}
