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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.repl.ReplInterpreter;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.utils.UtilsPackage;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractReplInterpreterTest extends UsefulTestCase {
    private static final Pattern START_PATTERN = Pattern.compile(">>>( *)(.*)$");
    private static final Pattern SUBSTRING_PATTERN = Pattern.compile("substring: (.*)");

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private enum MatchType {
        EQUALS,
        SUBSTRING,
    }

    private static class OneLine {
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
    private static List<OneLine> loadLines(@NotNull File file) {
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

    protected void doTest(@NotNull String path) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.FULL_JDK);
        ReplInterpreter repl = new ReplInterpreter(getTestRootDisposable(), configuration);

        for (OneLine line : loadLines(new File(path))) {
            String expected = StringUtil.convertLineSeparators(line.expected).replaceFirst("\n$", "");

            ReplInterpreter.LineResult lineResult = repl.eval(line.code);
            Object actual;
            if (lineResult.getType() == ReplInterpreter.LineResultType.SUCCESS) {
                actual = lineResult.getValue();
            }
            else if (lineResult.getType() == ReplInterpreter.LineResultType.INCOMPLETE) {
                actual = "incomplete";
            }
            else {
                actual = lineResult.getErrorText();
            }
            String actualString = StringUtil.convertLineSeparators(actual != null ? actual.toString() : "null").replaceFirst("\n$", "");

            if (line.matchType == MatchType.EQUALS) {
                Assert.assertEquals("after evaluation of: " + line.code, expected, actualString);
            }
            else if (line.matchType == MatchType.SUBSTRING) {
                Assert.assertTrue("must contain substring: " + expected + ", actual: " + actualString, actualString.contains(expected));
            }
        }
    }
}
