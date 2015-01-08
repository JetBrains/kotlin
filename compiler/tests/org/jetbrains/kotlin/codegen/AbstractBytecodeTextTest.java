/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractBytecodeTextTest extends CodegenTestCase {
    private static final Pattern EXPECTED_OCCURRENCES_PATTERN = Pattern.compile("^\\s*//\\s*(\\d+)\\s*(.*)$");

    public void doTest(@NotNull String filename) throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
        loadFileByFullPath(filename);
        List<OccurrenceInfo> expected = readExpectedOccurrences(filename);
        countAndCompareActualOccurrences(expected);
    }

    protected void countAndCompareActualOccurrences(@NotNull List<OccurrenceInfo> expectedOccurrences) {
        StringBuilder expected = new StringBuilder();
        StringBuilder actual = new StringBuilder();

        String text = generateToText();
        for (OccurrenceInfo info : expectedOccurrences) {
            expected.append(info.numberOfOccurrences).append(" ").append(info.needle).append("\n");
            int actualCount = StringUtil.findMatches(text, Pattern.compile("(" + info.needle + ")")).size();
            actual.append(actualCount).append(" ").append(info.needle).append("\n");
        }

        try {
            assertEquals(expected.toString(), actual.toString());
        }
        catch (Throwable e) {
            System.out.println(text);
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    protected List<OccurrenceInfo> readExpectedOccurrences(@NotNull String filename) throws Exception {
        List<OccurrenceInfo> result = new ArrayList<OccurrenceInfo>();
        String[] lines = FileUtil.loadFile(new File(filename), true).split("\n");

        for (String line : lines) {
            Matcher matcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line);
            if (matcher.matches()) {
                int numberOfOccurrences = Integer.parseInt(matcher.group(1));
                String needle = matcher.group(2);
                result.add(new OccurrenceInfo(numberOfOccurrences, needle));
            }
        }

        return result;
    }

    protected static class OccurrenceInfo {
        private final int numberOfOccurrences;
        private final String needle;

        private OccurrenceInfo(int numberOfOccurrences, @NotNull String needle) {
            this.numberOfOccurrences = numberOfOccurrences;
            this.needle = needle;
        }
    }
}
