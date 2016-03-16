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
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractBytecodeTextTest extends CodegenTestCase {
    private static final Pattern AT_OUTPUT_FILE_PATTERN = Pattern.compile("^\\s*//\\s*@(.*):$");
    private static final Pattern EXPECTED_OCCURRENCES_PATTERN = Pattern.compile("^\\s*//\\s*(\\d+)\\s*(.*)$");

    @Override
    protected void doMultiFileTest(@NotNull File wholeFile, @NotNull List<TestFile> files, @Nullable File javaFilesDir) throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, files, javaFilesDir);
        loadMultiFiles(files);

        if (isMultiFileTest(files)) {
            doTestMultiFile(files);
        }
        else {
            List<OccurrenceInfo> expected = readExpectedOccurrences(wholeFile.getPath());
            String actual = generateToText();
            checkGeneratedTextAgainstExpectedOccurrences(actual, expected);
        }
    }

    private static boolean isMultiFileTest(@NotNull List<TestFile> files) {
        int kotlinFiles = 0;
        for (TestFile file : files) {
            if (file.name.endsWith(".kt")) {
                kotlinFiles++;
            }
        }
        return kotlinFiles > 1;
    }

    protected static void checkGeneratedTextAgainstExpectedOccurrences(
            @NotNull String text,
            @NotNull List<OccurrenceInfo> expectedOccurrences
    ) {
        StringBuilder expected = new StringBuilder();
        StringBuilder actual = new StringBuilder();

        for (OccurrenceInfo info : expectedOccurrences) {
            expected.append(info.numberOfOccurrences).append(" ").append(info.needle).append("\n");
            int actualCount = StringUtil.findMatches(text, Pattern.compile("(" + info.needle + ")")).size();
            actual.append(actualCount).append(" ").append(info.needle).append("\n");
        }

        try {
            assertEquals(text, expected.toString(), actual.toString());
        }
        catch (Throwable e) {
            System.out.println(text);
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    private void doTestMultiFile(@NotNull List<TestFile> files) throws Exception {
        Map<String, List<OccurrenceInfo>> expectedOccurrencesByOutputFile = new LinkedHashMap<String, List<OccurrenceInfo>>();
        for (TestFile file : files) {
            readExpectedOccurrencesForMultiFileTest(file, expectedOccurrencesByOutputFile);
        }

        Map<String, String> generated = generateEachFileToText();
        for (String expectedOutputFile : expectedOccurrencesByOutputFile.keySet()) {
            assertTextWasGenerated(expectedOutputFile, generated);
            String generatedText = generated.get(expectedOutputFile);
            List<OccurrenceInfo> expectedOccurrences = expectedOccurrencesByOutputFile.get(expectedOutputFile);
            checkGeneratedTextAgainstExpectedOccurrences(generatedText, expectedOccurrences);
        }
    }

    private static void assertTextWasGenerated(String expectedOutputFile, Map<String, String> generated) {
        if (!generated.containsKey(expectedOutputFile)) {
            StringBuilder failMessage = new StringBuilder();
            failMessage.append("Missing output file ").append(expectedOutputFile).append(", got ").append(generated.size()).append(": ");
            for (String generatedFile : generated.keySet()) {
                failMessage.append(generatedFile).append(" ");
            }
            fail(failMessage.toString());
        }
    }

    @NotNull
    protected List<OccurrenceInfo> readExpectedOccurrences(@NotNull String filename) throws Exception {
        List<OccurrenceInfo> result = new ArrayList<OccurrenceInfo>();
        String[] lines = FileUtil.loadFile(new File(filename), Charsets.UTF_8.name(), true).split("\n");

        for (String line : lines) {
            Matcher matcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line);
            if (matcher.matches()) {
                result.add(parseOccurrenceInfo(matcher));
            }
        }

        return result;
    }

    private static void readExpectedOccurrencesForMultiFileTest(
            @NotNull TestFile file,
            @NotNull Map<String, List<OccurrenceInfo>> occurrenceMap
    ) {
        List<OccurrenceInfo> currentOccurrenceInfos = null;
        for (String line : file.content.split("\n")) {
            Matcher atOutputFileMatcher = AT_OUTPUT_FILE_PATTERN.matcher(line);
            if (atOutputFileMatcher.matches()) {
                String outputFileName = atOutputFileMatcher.group(1);
                if (occurrenceMap.containsKey(outputFileName)) {
                    throw new AssertionError(
                            file.name + ": Expected occurrences for output file " + outputFileName + " were already provided"
                    );
                }
                currentOccurrenceInfos = new ArrayList<OccurrenceInfo>();
                occurrenceMap.put(outputFileName, currentOccurrenceInfos);
            }

            Matcher expectedOccurrencesMatcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line);
            if (expectedOccurrencesMatcher.matches()) {
                if (currentOccurrenceInfos == null) {
                    throw new AssertionError(
                            file.name + ": Should specify output file with '// @<OUTPUT_FILE_NAME>:' before expectations"
                    );
                }
                OccurrenceInfo occurrenceInfo = parseOccurrenceInfo(expectedOccurrencesMatcher);
                currentOccurrenceInfos.add(occurrenceInfo);
            }
        }
    }

    @NotNull
    private static OccurrenceInfo parseOccurrenceInfo(Matcher matcher) {
        int numberOfOccurrences = Integer.parseInt(matcher.group(1));
        String needle = matcher.group(2);
        return new OccurrenceInfo(numberOfOccurrences, needle);
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
