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

package org.jetbrains.jet.testing;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HighlightTestDataUtil {
    public static String insertInfoTags(List<LineMarkerInfo> lineMarkers, boolean withDescription, String text) {
        List<LineMarkerTagPoint> lineMarkerPoints = Lists.newArrayList();
        for (LineMarkerInfo markerInfo : lineMarkers) {
            lineMarkerPoints.add(new LineMarkerTagPoint(markerInfo.startOffset, true, markerInfo, withDescription));
            lineMarkerPoints.add(new LineMarkerTagPoint(markerInfo.endOffset, false, markerInfo, withDescription));
        }

        return insertTagsInText(lineMarkerPoints, text);
    }

    public static String insertInfoTags(List<HighlightInfo> highlights, String text) {
        List<HighlightTagPoint> highlightPoints = Lists.newArrayList();
        for (HighlightInfo highlight : highlights) {
            highlightPoints.add(new HighlightTagPoint(highlight.startOffset, true, highlight));
            highlightPoints.add(new HighlightTagPoint(highlight.endOffset, false, highlight));
        }

        return insertTagsInText(highlightPoints, text);
    }

    private static String insertTagsInText(List<? extends HighlightTag> tags, String text) {
        StringBuilder builder = new StringBuilder(text);

        List<? extends HighlightTag> sortedTagPoints = Ordering.natural().reverse().sortedCopy(tags);

        // Insert tags into text starting from the end for preventing invalidating previous tags offsets
        for (HighlightTag point : sortedTagPoints) {
            String tagText;
            if (point.isStart) {
                String attributesString = point.getAttributesString();
                if (attributesString.isEmpty()) {
                    tagText = String.format("<%s>", point.getName());
                }
                else {
                    tagText = String.format("<%s %s>", point.getName(), attributesString);
                }
            }
            else {
                tagText = String.format("</%s>", point.getName());
            }

            builder.insert(point.offset, tagText);
        }

        return builder.toString();
    }

    private static abstract class HighlightTag<Data> implements Comparable<HighlightTag<?>> {
        protected final int offset;
        protected final boolean isStart;
        protected final Data data;

        private HighlightTag(int offset, boolean start, Data data) {
            this.offset = offset;
            isStart = start;
            this.data = data;
        }

        @Override
        public int compareTo(@NotNull HighlightTag<?> other) {
            if (offset != other.offset) {
                return ((Integer) offset).compareTo(other.offset);
            }

            if (isStart != other.isStart) {
                // All "starts" should go after "ends" for same offset
                return isStart ? -1 : 1;
            }

            String thisTag = this.getName();
            String otherTag = other.getName();

            // Invert order for end tags
            return thisTag.compareTo(otherTag) * (isStart ? -1 : 1);
        }

        @NotNull
        abstract String getName();

        @NotNull
        abstract String getAttributesString();
    }

    private static class HighlightTagPoint extends HighlightTag<HighlightInfo> {
        private final HighlightInfo highlightInfo;

        private HighlightTagPoint(int offset, boolean start, HighlightInfo info) {
            super(offset, start, info);
            highlightInfo = info;
        }

        @NotNull
        @Override
        public String getName() {
            return highlightInfo.getSeverity().equals(HighlightSeverity.INFORMATION)
                   ? "info"
                   : highlightInfo.getSeverity().toString().toLowerCase();
        }

        @NotNull
        @Override
        String getAttributesString() {
            if (isStart) {
                if (highlightInfo.getDescription() != null) {
                    return String.format("textAttributesKey=\"%s\" descr=%s",
                                         highlightInfo.forcedTextAttributesKey,
                                         highlightInfo.getDescription());
                }
                else {
                    return String.format("textAttributesKey=\"%s\"", getName());
                }
            }
            else {
                return "";
            }
        }
    }

    private static class LineMarkerTagPoint extends HighlightTag<LineMarkerInfo> {
        private final boolean withDescription;

        public LineMarkerTagPoint(int offset, boolean start, LineMarkerInfo info, boolean withDescription) {
            super(offset, start, info);
            this.withDescription = withDescription;
        }

        @NotNull
        @Override
        String getName() {
            return "lineMarker";
        }

        @NotNull
        @Override
        String getAttributesString() {
            return withDescription ? String.format("descr=\"%s\"", data.getLineMarkerTooltip()) : "descr=\"*\"";
        }
    }
}
