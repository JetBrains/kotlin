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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.repl.ReplInterpreter;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.junit.Assert;

import java.io.File;

import static org.jetbrains.jet.repl.ReplSessionTestFile.MatchType.EQUALS;
import static org.jetbrains.jet.repl.ReplSessionTestFile.MatchType.SUBSTRING;

public abstract class AbstractReplInterpreterTest extends UsefulTestCase {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    protected void doTest(@NotNull String path) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.FULL_JDK);
        ReplInterpreter repl = new ReplInterpreter(getTestRootDisposable(), configuration);

        for (ReplSessionTestFile.OneLine line : ReplSessionTestFile.load(new File(path))) {
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

            if (line.matchType == EQUALS) {
                Assert.assertEquals("after evaluation of: " + line.code, expected, actualString);
            }
            else if (line.matchType == SUBSTRING) {
                Assert.assertTrue("must contain substring: " + expected + ", actual: " + actualString, actualString.contains(expected));
            }
        }
    }
}
