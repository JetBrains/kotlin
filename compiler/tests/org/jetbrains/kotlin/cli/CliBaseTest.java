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

import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Pair;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.KotlinVersion;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion;
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.StringsKt;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public abstract class CliBaseTest extends TestCaseWithTmpdir {
    static final String JS_TEST_DATA = "compiler/testData/cli/js";
    static final String JVM_TEST_DATA = "compiler/testData/cli/jvm";

    @NotNull
    public static Pair<String, ExitCode> executeCompilerGrabOutput(@NotNull CLICompiler<?> compiler, @NotNull List<String> args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        try {
            System.setErr(new PrintStream(bytes));
            ExitCode exitCode = CLICompiler.doMainNoExit(compiler, ArrayUtil.toStringArray(args));
            return new Pair<String, ExitCode>(bytes.toString("utf-8"), exitCode);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
        finally {
            System.setErr(origErr);
        }
    }

    @NotNull
    public static String getNormalizedCompilerOutput(@NotNull String pureOutput, @NotNull ExitCode exitCode, @NotNull String testDataDir) {
        return getNormalizedCompilerOutput(pureOutput, exitCode, testDataDir, JvmMetadataVersion.INSTANCE);
    }

    @NotNull
    public static String getNormalizedCompilerOutput(
            @NotNull String pureOutput,
            @NotNull ExitCode exitCode,
            @NotNull String testDataDir,
            @NotNull BinaryVersion version
    ) {
        String normalizedOutputWithoutExitCode = pureOutput
                .replace(new File(testDataDir).getAbsolutePath(), "$TESTDATA_DIR$")
                .replace(PathUtil.getKotlinPathsForDistDirectory().getHomePath().getAbsolutePath(), "$PROJECT_DIR$")
                .replace("expected version is " + version, "expected version is $ABI_VERSION$")
                .replace("\\", "/")
                .replace(KotlinVersion.VERSION, "$VERSION$");

        return normalizedOutputWithoutExitCode + exitCode;
    }

    private void executeCompilerCompareOutput(@NotNull CLICompiler<?> compiler, @NotNull String testDataDir) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String testMethodName = getTestName(true);
        Pair<String, ExitCode> outputAndExitCode =
                executeCompilerGrabOutput(compiler, readArgs(testDataDir + "/" + testMethodName + ".args", testDataDir,
                                                             tmpdir.getPath()));
        String actual = getNormalizedCompilerOutput(outputAndExitCode.getFirst(), outputAndExitCode.getSecond(), testDataDir);

        KotlinTestUtils.assertEqualsToFile(new File(testDataDir + "/" + testMethodName + ".out"), actual);

        File additionalTestConfig = new File(testDataDir + "/" + testMethodName + ".test");
        if (additionalTestConfig.exists()) {
            doTestAdditionalChecks(additionalTestConfig);
        }
    }

    private void doTestAdditionalChecks(@NotNull File testConfigFile) throws IOException {
        List<String> diagnostics = new ArrayList<String>(0);
        String content = FilesKt.readText(testConfigFile, Charsets.UTF_8);

        List<String> existsList = InTextDirectivesUtils.findListWithPrefixes(content, "// EXISTS: ");
        for (String fileName : existsList) {
            File file = new File(tmpdir, fileName);
            if (!file.exists()) {
                diagnostics.add("File does not exist, but should: " + fileName);
            }
            else if (!file.isFile()) {
                diagnostics.add("File is a directory, but should be a normal file: " + fileName);
            }
        }

        List<String> absentList = InTextDirectivesUtils.findListWithPrefixes(content, "// ABSENT: ");
        for (String fileName : absentList) {
            File file = new File(tmpdir, fileName);
            if (file.exists() && file.isFile()) {
                diagnostics.add("File exists, but shouldn't: " + fileName);
            }
        }

        if (!diagnostics.isEmpty()) {
            diagnostics.add(0, diagnostics.size() + " problem(s) found:");
            Assert.fail(StringsKt.join(diagnostics, "\n"));
        }
    }

    @NotNull
    static List<String> readArgs(
            @NotNull String argsFilePath,
            @NotNull final String testDataDir,
            @NotNull final String tempDir
    ) throws IOException {
        List<String> lines = FilesKt.readLines(new File(argsFilePath), Charsets.UTF_8);

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
