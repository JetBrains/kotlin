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

package org.jetbrains.kotlin.cli;

import com.intellij.openapi.util.Pair;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Charsets;
import kotlin.io.IoPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.Tmpdir;
import org.jetbrains.kotlin.utils.UtilsPackage;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class CliBaseTest {
    static final String JS_TEST_DATA = "compiler/testData/cli/js";
    static final String JVM_TEST_DATA = "compiler/testData/cli/jvm";

    @Rule
    public final Tmpdir tmpdir = new Tmpdir();
    @Rule
    public final TestName testName = new TestName();

    @NotNull
    public static Pair<String, ExitCode> executeCompilerGrabOutput(@NotNull CLICompiler<?> compiler, @NotNull List<String> args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        try {
            System.setErr(new PrintStream(bytes));
            ExitCode exitCode = CLICompiler.doMainNoExit(compiler, ArrayUtil.toStringArray(args));
            return Pair.create(bytes.toString("utf-8"), exitCode);
        }
        catch (Exception e) {
            throw UtilsPackage.rethrow(e);
        }
        finally {
            System.setErr(origErr);
        }
    }

    @NotNull
    public static String getNormalizedCompilerOutput(@NotNull String pureOutput, @NotNull ExitCode exitCode, @NotNull String testDataDir) {
        String normalizedOutputWithoutExitCode = pureOutput
                .replace(new File(testDataDir).getAbsolutePath(), "$TESTDATA_DIR$")
                .replace("expected ABI version is " + Integer.toString(JvmAbi.VERSION), "expected ABI version is $ABI_VERSION$")
                .replace("\\", "/");

        return normalizedOutputWithoutExitCode + exitCode;
    }

    private void executeCompilerCompareOutput(@NotNull CLICompiler<?> compiler, @NotNull String testDataDir) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Pair<String, ExitCode> outputAndExitCode =
                executeCompilerGrabOutput(compiler, readArgs(testDataDir + "/" + testName.getMethodName() + ".args", testDataDir,
                                                             tmpdir.getTmpDir().getPath()));
        String actual = getNormalizedCompilerOutput(outputAndExitCode.first, outputAndExitCode.second, testDataDir);

        JetTestUtils.assertEqualsToFile(new File(testDataDir + "/" + testName.getMethodName() + ".out"), actual);
    }

    @NotNull
    static List<String> readArgs(
            @NotNull String argsFilePath,
            @NotNull final String testDataDir,
            @NotNull final String tempDir
    ) throws IOException {
        List<String> lines = IoPackage.readLines(new File(argsFilePath), Charsets.UTF_8);

        return ContainerUtil.mapNotNull(lines, new Function<String, String>() {
            @Override
            public String fun(String arg) {
                if (arg.isEmpty()) {
                    return null;
                }
                // Do not replace : after \ (used in compiler plugin tests)
                String argsWithColonsReplaced = arg
                        .replace("\\:", "$COLON$")
                        .replace(":", File.pathSeparator)
                        .replace("$COLON$", ":");

                return argsWithColonsReplaced
                        .replace("$TEMP_DIR$", tempDir)
                        .replace("$TESTDATA_DIR$", testDataDir);
            }
        });
    }

    protected void executeCompilerCompareOutputJVM() throws Exception {
        executeCompilerCompareOutput(new K2JVMCompiler(), JVM_TEST_DATA);
    }

    protected void executeCompilerCompareOutputJS() throws Exception {
        executeCompilerCompareOutput(new K2JSCompiler(), JS_TEST_DATA);
    }
}
