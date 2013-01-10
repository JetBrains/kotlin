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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.repl.ReplInterpreter;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ReplInterpreterTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final Disposable disposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    @After
    public void tearDown() {
        Disposer.dispose(disposable);
    }

    private void testFile(@NotNull String relativePath) {
        CompilerConfiguration configuration =
                JetTestUtils.compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK);
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, new File("out/production/runtime"));
        ReplInterpreter repl = new ReplInterpreter(disposable, configuration);

        ReplSessionTestFile file = ReplSessionTestFile.load(new File("compiler/testData/repl/" + relativePath));
        for (ReplSessionTestFile.OneLine t : file.getLines()) {
            String code = t.getCode();

            String expected = StringUtil.convertLineSeparators(t.getExpected()).replaceFirst("\n$", "");
            ReplSessionTestFile.MatchType matchType = t.getMatchType();

            ReplInterpreter.LineResult lineResult = repl.eval(code);
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

            if (matchType == ReplSessionTestFile.MatchType.EQUALS) {
                Assert.assertEquals("after evaluation of: " + code, expected, actualString);
            }
            else if (matchType == ReplSessionTestFile.MatchType.SUBSTRING) {
                Assert.assertTrue("must contain substring: " + expected + ", actual: " + actualString, actualString.contains(expected));
            }

        }
    }

    @Test
    public void constants() {
        testFile("constants.repl");
    }

    @Test
    public void simple() {
        testFile("simple.repl");
    }

    @Test
    public void function() {
        testFile("function.repl");
    }

    @Test
    public void functionReferencesPrev() {
        testFile("functionReferencesPrev.repl");
    }

    @Test
    public void twoClosures() {
        testFile("twoClosures.repl");
    }

    @Test
    public void functionOverloadResolutionAnyBeatsString() {
        testFile("functionOverloadResolutionAnyBeatsString.repl");
    }

    @Test
    public void functionOverloadResolution() {
        testFile("functionOverloadResolution.repl");
    }

    @Test
    public void empty() {
        testFile("empty.repl");
    }

    @Test
    public void imports() {
        testFile("imports.repl");
    }


    @Test
    public void syntaxErrors() {
        testFile("syntaxErrors.repl");
    }

    @Test
    public void analyzeErrors() {
        testFile("analyzeErrors.repl");
    }

    @Test
    public void evaluationErrors() {
        testFile("evaluationErrors.repl");
    }

    @Test
    public void multiline() {
        testFile("multiline.repl");
    }

    @Test
    public void multiline3() {
        testFile("multiline3.repl");
    }

}
