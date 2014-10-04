/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.ExpectedHighlightingData;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.highlighter.markers.SuperDeclarationMarkerNavigationHandler;
import org.jetbrains.jet.plugin.navigation.NavigationTestUtils;
import org.jetbrains.jet.testing.ReferenceUtils;
import org.jetbrains.jet.testing.TagsTestDataUtil;
import org.junit.Assert;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractLineMarkersTest extends JetLightCodeInsightFixtureTestCase {

    private static final String LINE_MARKER_PREFIX = "LINEMARKER:";
    private static final String TARGETS_PREFIX = "TARGETS";

    @Override
    protected String getBasePath() {
        return PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE + "/codeInsight/lineMarker";
    }

    public void doTest(String path) {
        try {
            myFixture.configureByFile(path);
            Project project = myFixture.getProject();
            Document document = myFixture.getEditor().getDocument();

            ExpectedHighlightingData data = new ExpectedHighlightingData(
                    document, false, false, false, myFixture.getFile());
            data.init();

            PsiDocumentManager.getInstance(project).commitAllDocuments();

            myFixture.doHighlighting();

            List<LineMarkerInfo> markers = DaemonCodeAnalyzerImpl.getLineMarkers(document, project);

            try {
                data.checkLineMarkers(markers, document.getText());
            }
            catch (AssertionError error) {
                try {
                    String actualTextWithTestData = TagsTestDataUtil.insertInfoTags(markers, true, myFixture.getFile().getText());
                    JetTestUtils.assertEqualsToFile(new File(getTestDataPath(), fileName()), actualTextWithTestData);
                }
                catch (FileComparisonFailure failure) {
                    throw new FileComparisonFailure(error.getMessage() + "\n" + failure.getMessage(),
                                                    failure.getExpected(),
                                                    failure.getActual(),
                                                    failure.getFilePath());
                }
            }

            assertNavigationElements(markers);
        }
        catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private void assertNavigationElements(List<LineMarkerInfo> markers) {
        List<String> navigationDataComments = JetTestUtils.getLastCommentsInFile(
                (JetFile) myFixture.getFile(), JetTestUtils.CommentType.BLOCK_COMMENT, false);
        if (navigationDataComments.isEmpty()) return;

        for (String navigationComment : navigationDataComments) {
            final String description = getLineMarkerDescription(navigationComment);
            LineMarkerInfo navigateMarker = ContainerUtil.find(markers, new Condition<LineMarkerInfo>() {
                @Override
                public boolean value(LineMarkerInfo marker) {
                    String tooltip = marker.getLineMarkerTooltip();
                    return tooltip != null && tooltip.startsWith(description);
                }
            });

            assertNotNull(
                    String.format("Can't find marker for navigation check with description \"%s\"", description),
                    navigateMarker);

            GutterIconNavigationHandler handler = navigateMarker.getNavigationHandler();
            if (handler instanceof SuperDeclarationMarkerNavigationHandler) {
                PsiElement element = navigateMarker.getElement();

                //noinspection unchecked
                handler.navigate(null, element);
                List<NavigatablePsiElement> navigateElements =
                        ((SuperDeclarationMarkerNavigationHandler) handler).getNavigationElements();

                Collections.sort(navigateElements, new Comparator<NavigatablePsiElement>() {
                    @Override
                    public int compare(@NotNull NavigatablePsiElement first, @NotNull NavigatablePsiElement second) {
                        String elementFirstStr = ReferenceUtils.renderAsGotoImplementation(first);
                        String elementSecondStr = ReferenceUtils.renderAsGotoImplementation(second);

                        return elementFirstStr.compareTo(elementSecondStr);
                    }
                });

                String actualNavigationData =
                        NavigationTestUtils.getNavigateElementsText(myFixture.getProject(), navigateElements);

                assertSameLines(getExpectedNavigationText(navigationComment), actualNavigationData);
            }
            else {
                Assert.fail("Only SuperDeclarationMarkerNavigationHandler are supported in navigate check");
            }
        }
    }

    private static String getLineMarkerDescription(String navigationComment) {
        int firstLineEnd = navigationComment.indexOf("\n");
        assertTrue("The first line in block comment must contain description of marker for navigation check", firstLineEnd != -1);

        String navigationMarkerText = navigationComment.substring(0, firstLineEnd);

        assertTrue(String.format("Add %s directive in first line of comment", LINE_MARKER_PREFIX),
                   navigationMarkerText.startsWith(LINE_MARKER_PREFIX));

        navigationMarkerText = navigationMarkerText.substring(LINE_MARKER_PREFIX.length());

        return navigationMarkerText.trim();
    }

    private static String getExpectedNavigationText(String navigationComment) {
        int firstLineEnd = navigationComment.indexOf("\n");

        String expectedNavigationText = navigationComment.substring(firstLineEnd + 1);

        assertTrue(
                String.format("Marker %s is expected before navigation data", TARGETS_PREFIX),
                expectedNavigationText.startsWith(TARGETS_PREFIX));

        expectedNavigationText = expectedNavigationText.substring(expectedNavigationText.indexOf("\n") + 1);

        return expectedNavigationText;
    }
}
