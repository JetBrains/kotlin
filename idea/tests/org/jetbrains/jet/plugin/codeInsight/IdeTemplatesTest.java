package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Evgeny Gerashchenko
 * @since 2/14/12
 */
public class IdeTemplatesTest extends LightCodeInsightFixtureTestCase {
    public void testAll() {
        myFixture.configureByFile(PluginTestCaseBase.getTestDataPathBase() + "/templates/IdeTemplates.kt");
        CodeFoldingManager.getInstance(myFixture.getProject()).buildInitialFoldings(myFixture.getEditor());

        ArrayList<Region> regions = getExpectedRegions();
        assertEquals(regions.toString(), getActualRegions().toString());

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
