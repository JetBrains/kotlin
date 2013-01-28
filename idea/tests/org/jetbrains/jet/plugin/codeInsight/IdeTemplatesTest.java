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

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdeTemplatesTest extends LightCodeInsightFixtureTestCase {
    private ArrayList<Region> myExpectedRegions;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/templates/");
    }

    public void testAll() {
        myFixture.configureByFile("IdeTemplates.kt");
        CodeFoldingManager.getInstance(myFixture.getProject()).buildInitialFoldings(myFixture.getEditor());

        myExpectedRegions = getExpectedRegions();
        assertEquals(myExpectedRegions.toString(), getActualRegions().toString());

        for (int i = 0; i <= myExpectedRegions.size(); i++) {
            nextParam();
            checkSelectedRegion(i % myExpectedRegions.size());
        }

        for (int i = myExpectedRegions.size() - 1; i >= 0; i--) {
            prevParam();
            checkSelectedRegion(i);
        }
    }

    private void checkSelectedRegion(int region) {
        SelectionModel selectionModel = myFixture.getEditor().getSelectionModel();
        assertEquals(myExpectedRegions.get(region).start, selectionModel.getSelectionStart());
        assertEquals(myExpectedRegions.get(region).end, selectionModel.getSelectionEnd());
    }

    private void prevParam() {
        doAction("PrevTemplateParameter");
    }

    private void nextParam() {
        doAction("NextTemplateParameter");
    }

    private ArrayList<Region> getExpectedRegions() {
        Pattern regex = Pattern.compile("<#<(\\w+)>#>");
        Matcher matcher = regex.matcher(myFixture.getEditor().getDocument().getText());
        ArrayList<Region> expected = new ArrayList<Region>();
        while (matcher.find()) {
            expected.add(new Region(matcher.start(), matcher.end(), matcher.group(1)));
        }
        return expected;
    }

    private ArrayList<Region> getActualRegions() {
        ArrayList<Region> actual = new ArrayList<Region>();
        for (FoldRegion fr : myFixture.getEditor().getFoldingModel().getAllFoldRegions()) {
            if (fr.shouldNeverExpand()) {
                assertFalse(fr.isExpanded());
                actual.add(new Region(fr.getStartOffset(), fr.getEndOffset(), fr.getPlaceholderText()));
            }
        }
        return actual;
    }

    private void doAction(@NotNull String actionId) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        action.actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent()),
                                                 ActionPlaces.UNKNOWN, action.getTemplatePresentation(),
                                                 ActionManager.getInstance(), 0));
    }

    private static class Region {
        public int start;
        public int end;
        public String group;

        private Region(int start, int end, String group) {
            this.start = start;
            this.end = end;
            this.group = group;
        }

        @Override
        public String toString() {
            return String.format("%d..%d:%s", start, end, group);
        }
    }
}
