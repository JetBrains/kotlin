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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;

import java.io.File;
import java.util.List;

public abstract class AbstractHighlightingTest extends JetLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    protected void doTest(String filePath) throws Exception {
        String fileText = FileUtil.loadFile(new File(filePath), true);
        boolean checkInfos = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_INFOS");
        boolean checkWeakWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_WEAK_WARNINGS");
        boolean checkWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_WARNINGS");

        myFixture.configureByFile(filePath);

        try {
            myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
        }
        catch (FileComparisonFailure e) {
            System.out.println(getFileWithTextAttributesKeys());

            throw e;
        }
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }

    private String getFileWithTextAttributesKeys() {
        List<HighlightInfo> highlights =
                DaemonCodeAnalyzerImpl.getHighlights(myFixture.getDocument(myFixture.getFile()), null, myFixture.getProject());

        List<HighlightTagPoint> pointsForHighlightInfos = Lists.newArrayList();
        for (HighlightInfo highlight : highlights) {
            pointsForHighlightInfos.add(new HighlightTagPoint(highlight.startOffset, true, highlight));
            pointsForHighlightInfos.add(new HighlightTagPoint(highlight.endOffset, false, highlight));
        }

        StringBuilder builder = new StringBuilder(myFixture.getFile().getText());

        List<HighlightTagPoint> sortedTagPoints = Ordering.natural().reverse().sortedCopy(pointsForHighlightInfos);

        // Insert tags into text starting from the end for preventing invalidating of highlight offsets
        for (HighlightTagPoint point : sortedTagPoints) {
            String tagName;
            if (point.highlightInfo.getSeverity().equals(HighlightSeverity.INFORMATION)) {
                tagName = "info";
            }
            else {
                tagName = point.highlightInfo.getSeverity().toString().toLowerCase();
            }

            String tagText;
            if (point.isStart) {
                if (point.highlightInfo.getDescription() != null) {
                    tagText = String.format("<%s textAttributesKey=\"%s\" descr=%s>",
                                            tagName,
                                            point.highlightInfo.forcedTextAttributesKey,
                                            point.highlightInfo.getDescription());
                }
                else {
                    tagText = String.format("<%s textAttributesKey=\"%s\">", tagName, point.highlightInfo.forcedTextAttributesKey);
                }
            }
            else {
                tagText = String.format("</%s>", tagName);
            }

            builder.insert(point.offset, tagText);
        }

        return builder.toString();
    }

    private static class HighlightTagPoint implements Comparable<HighlightTagPoint> {
        private final int offset;
        private final boolean isStart;
        private final HighlightInfo highlightInfo;

        private HighlightTagPoint(int offset, boolean start, HighlightInfo info) {
            this.offset = offset;
            isStart = start;
            highlightInfo = info;
        }

        @Override
        public int compareTo(@NotNull HighlightTagPoint other) {
            if (offset != other.offset) {
                return ((Integer) offset).compareTo(other.offset);
            }

            if (isStart != other.isStart) {
                // All "starts" should go after "ends" for same offset
                return isStart ? -1 : 1;
            }

            if (highlightInfo.getSeverity() != other.highlightInfo.getSeverity()) {
                // Invert order for end tags
                return highlightInfo.getSeverity().compareTo(other.highlightInfo.getSeverity()) * (isStart ? -1 : 1);
            }

            // The order isn't important for highlighting with same offset and severity
            return 0;
        }
    }
}