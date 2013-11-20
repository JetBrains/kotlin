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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
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
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class CliBaseTest {
    private static final String JS_TEST_DATA = "compiler/testData/cli/js";
    private static final String JVM_TEST_DATA = "compiler/testData/cli/jvm";

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

    private void executeCompilerCompareOutput(@NotNull CLICompiler<?> compiler, @NotNull String testDataDir) throws Exception {
        String actual = executeCompilerGrabOutput(compiler, readArgs(testDataDir))
                .replace(new File(testDataDir).getAbsolutePath(), "$TESTDATA_DIR$")
                .replace("\\", "/");

        JetTestUtils.assertEqualsToFile(new File(testDataDir + "/" + testName.getMethodName() + ".out"), actual);
    }

    private String[] readArgs(@NotNull final String testDataDir) throws IOException {
        List<String> lines = FileUtil.loadLines(testDataDir + "/" + testName.getMethodName() + ".args");

        return ArrayUtil.toStringArray(ContainerUtil.mapNotNull(lines, new Function<String, String>() {
            @Override
            public String fun(String arg) {
                if (arg.isEmpty()) {
                    return null;
                }
                return arg
                        .replace(":", File.pathSeparator)
                        .replace("$TEMP_DIR$", tmpdir.getTmpDir().getPath())
                        .replace("$TESTDATA_DIR$", testDataDir);
            }
        }));
    }

    protected void executeCompilerCompareOutputJVM() throws Exception {
        executeCompilerCompareOutput(new K2JVMCompiler(), JVM_TEST_DATA);
    }

    protected void executeCompilerCompareOutputJS() throws Exception {
        executeCompilerCompareOutput(new K2JSCompiler(), JS_TEST_DATA);
    }
}
