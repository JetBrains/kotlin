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

package org.jetbrains.jet.repl;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplSessionTestFile {
    private static final Pattern START_PATTERN = Pattern.compile(">>>( *)(.*)$");
    private static final Pattern SUBSTRING_PATTERN = Pattern.compile("substring: (.*)");

    public enum MatchType {
        EQUALS,
        SUBSTRING,
    }

    public static class OneLine {
        public final String code;
        public final String expected;
        public final MatchType matchType;

        public OneLine(@NotNull String code, @NotNull String expected, @NotNull MatchType matchType) {
            this.code = code;
            this.expected = expected;
            this.matchType = matchType;
        }
    }

    @NotNull
    public static List<OneLine> load(@NotNull File file) {
        Queue<String> lines;
        try {
            lines = new ArrayDeque<String>(FileUtil.loadLines(file));
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }

        List<OneLine> result = new ArrayList<OneLine>();

        while (!lines.isEmpty()) {
            String line = lines.poll();
            Matcher matcher = START_PATTERN.matcher(line);
            assert matcher.matches() : "Line doesn't match start pattern: " + line;
            String code = matcher.group(2);

            Matcher substringMatcher = SUBSTRING_PATTERN.matcher(lines.peek());
            if (substringMatcher.matches()) {
                result.add(new OneLine(code, substringMatcher.group(1), MatchType.SUBSTRING));
                lines.poll();
                continue;
            }

            StringBuilder value = new StringBuilder();
            while (!lines.isEmpty() && !START_PATTERN.matcher(lines.peek()).matches()) {
                value.append(lines.poll()).append("\n");
            }

            result.add(new OneLine(code, value.toString(), MatchType.EQUALS));
        }

        return result;
    }
}
