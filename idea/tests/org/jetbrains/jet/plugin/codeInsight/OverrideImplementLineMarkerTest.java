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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.ExpectedHighlightingData;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.List;

public class OverrideImplementLineMarkerTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getBasePath() {
        return PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE + "/codeInsight/lineMarker";
    }

    public void testTrait() throws Throwable {
        doTest();
    }

    public void testClass() throws Throwable {
        doTest();
    }

    public void testOverrideFunction() throws Throwable {
        doTest();
    }

    private void doTest() {
        try {
            myFixture.configureByFile(getTestName(false) + ".kt");
            Project project = myFixture.getProject();
            Document document = myFixture.getEditor().getDocument();

            ExpectedHighlightingData data = new ExpectedHighlightingData(document, false, false, false, myFixture.getFile());
            data.init();

            PsiDocumentManager.getInstance(project).commitAllDocuments();

            myFixture.doHighlighting();

            List<LineMarkerInfo> markers = DaemonCodeAnalyzerImpl.getLineMarkers(document, project);
            data.checkLineMarkers(markers, document.getText());
        }
        catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
}
