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

package org.jetbrains.jet.cli.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import org.jetbrains.jet.test.Tmpdir;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

public class CliBaseTest {
    protected static final String NOT_EXISTING_PATH = "not/existing/path";

    @Rule
    public final Tmpdir tmpdir = new Tmpdir();
    @Rule
    public final TestName testName = new TestName();

    @NotNull
    private static String executeCompilerGrabOutput(@NotNull CLICompiler<?> compiler, @NotNull String[] args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream origOut = System.out;
        try {
            System.setOut(new PrintStream(bytes));
            ExitCode exitCode = CLICompiler.doMainNoExit(compiler, args);
            return bytes.toString("utf-8") + exitCode + "\n";
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
        finally {
            System.setOut(origOut);
        }
    }

    private void executeCompilerCompareOutput(@NotNull CLICompiler<?> compiler, @NotNull String[] args) {
        String actual = executeCompilerGrabOutput(compiler, args)
                .replace(new File("compiler/testData/cli/").getAbsolutePath(), "$TESTDATA_DIR$")
                .replace("\\", "/");

        JetTestUtils.assertEqualsToFile(new File("compiler/testData/cli/" + testName.getMethodName() + ".out"), actual);
    }

    protected void executeCompilerCompareOutputJVM(@NotNull String[] args) {
        executeCompilerCompareOutput(new K2JVMCompiler(), args);
    }

    protected void executeCompilerCompareOutputJS(@NotNull String[] args) {
        executeCompilerCompareOutput(new K2JSCompiler(), args);
    }
}
