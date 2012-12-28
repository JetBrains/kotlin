/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.repl;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplSessionTestFile {

    public enum MatchType {
        EQUALS,
        SUBSTRING,
    }

    public static class OneLine {
        @NotNull
        private final String code;
        @NotNull
        private final String expected;
        @NotNull
        private final MatchType matchType;

        public OneLine(@NotNull String code, @NotNull String expected, @NotNull MatchType matchType) {
            this.code = code;
            this.expected = expected;
            this.matchType = matchType;
        }

        @NotNull
        public String getCode() {
            return code;
        }

        @NotNull
        public String getExpected() {
            return expected;
        }

        @NotNull
        public MatchType getMatchType() {
            return matchType;
        }
    }

    @NotNull
    private final List<OneLine> lines;

    public ReplSessionTestFile(@NotNull List<OneLine> lines) {
        this.lines = lines;
    }

    @NotNull
    public List<OneLine> getLines() {
        return lines;
    }

    public static ReplSessionTestFile load(@NotNull File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                List<String> lines = CharStreams.readLines(reader);
                return load(new SimpleLinesParser(lines));
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ReplSessionTestFile load(@NotNull SimpleLinesParser parser) throws IOException {
        List<OneLine> list = Lists.newArrayList();

        Pattern startPattern = Pattern.compile(">>>( |$)(.*)");
        Pattern substringPattern = Pattern.compile("substring: (.*)");

        while (!parser.lookingAtEof()) {
            Matcher matcher = parser.next(startPattern);
            String code = matcher.group(2);

            StringBuilder value = new StringBuilder();

            Matcher substringMatcher = parser.lookingAt(substringPattern);
            if (substringMatcher != null) {
                list.add(new OneLine(code, substringMatcher.group(1), MatchType.SUBSTRING));
                parser.next();
                continue;
            }

            while (!parser.lookingAtEof() && parser.lookingAt(startPattern) == null) {
                value.append(parser.next()).append("\n");
            }

            list.add(new OneLine(code, value.toString(), MatchType.EQUALS));
        }
        return new ReplSessionTestFile(list);
    }

}
