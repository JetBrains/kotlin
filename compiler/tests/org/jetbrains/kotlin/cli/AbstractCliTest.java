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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.CLITool;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.cli.js.dce.K2JSDce;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.KotlinCompilerVersion;
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.StringsKt;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCliTest extends TestCaseWithTmpdir {
    @NotNull
    public static Pair<String, ExitCode> executeCompilerGrabOutput(@NotNull CLITool<?> compiler, @NotNull List<String> args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        try {
            System.setErr(new PrintStream(bytes));
            ExitCode exitCode = CLITool.doMainNoExit(compiler, ArrayUtil.toStringArray(args));
            return new Pair<>(bytes.toString("utf-8"), exitCode);
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
        String testDataAbsoluteDir = new File(testDataDir).getAbsolutePath();
        String normalizedOutputWithoutExitCode = StringUtil.convertLineSeparators(pureOutput)
                .replace(testDataAbsoluteDir, "$TESTDATA_DIR$")
                .replace(FileUtil.toSystemIndependentName(testDataAbsoluteDir), "$TESTDATA_DIR$")
                .replace(PathUtil.getKotlinPathsForDistDirectory().getHomePath().getAbsolutePath(), "$PROJECT_DIR$")
                .replace("expected version is " + JvmMetadataVersion.INSTANCE, "expected version is $ABI_VERSION$")
                .replace("expected version is " + JsMetadataVersion.INSTANCE, "expected version is $ABI_VERSION$")
                .replace("\\", "/")
                .replace(KotlinCompilerVersion.VERSION, "$VERSION$");

        return normalizedOutputWithoutExitCode + exitCode;
    }

    private void doTest(@NotNull String fileName, @NotNull CLITool<?> compiler) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Pair<String, ExitCode> outputAndExitCode = executeCompilerGrabOutput(compiler, readArgs(fileName, tmpdir.getPath()));
        String actual = getNormalizedCompilerOutput(
                outputAndExitCode.getFirst(), outputAndExitCode.getSecond(), new File(fileName).getParent()
        );

        File outFile = new File(fileName.replaceFirst("\\.args$", ".out"));
        KotlinTestUtils.assertEqualsToFile(outFile, actual);

        File additionalTestConfig = new File(fileName.replaceFirst("\\.args$", ".test"));
        if (additionalTestConfig.exists()) {
            doTestAdditionalChecks(additionalTestConfig);
        }
    }

    private void doTestAdditionalChecks(@NotNull File testConfigFile) throws IOException {
        List<String> diagnostics = new ArrayList<>(0);
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
    static List<String> readArgs(@NotNull String argsFilePath, @NotNull String tempDir) throws IOException {
        List<String> lines = FilesKt.readLines(new File(argsFilePath), Charsets.UTF_8);

        return CollectionsKt.mapNotNull(lines, arg -> {
            if (arg.isEmpty()) {
                return null;
            }

            // Do not replace ':' after '\' (used in compiler plugin tests)
            String argsWithColonsReplaced = arg
                    .replace("\\:", "$COLON$")
                    .replace(":", File.pathSeparator)
                    .replace("$COLON$", ":");

            return argsWithColonsReplaced
                    .replace("$TEMP_DIR$", tempDir)
                    .replace("$TESTDATA_DIR$", new File(argsFilePath).getParent());
        });
    }

    protected void doJvmTest(@NotNull String fileName) throws Exception {
        doTest(fileName, new K2JVMCompiler());
    }

    protected void doJsTest(@NotNull String fileName) throws Exception {
        doTest(fileName, new K2JSCompiler());
    }

    protected void doJsDceTest(@NotNull String fileName) throws Exception {
        doTest(fileName, new K2JSDce());
    }

    public static String removePerfOutput(String output) {
        String[] lines = StringUtil.splitByLinesKeepSeparators(output);
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (!line.contains("PERF:")) {
                result.append(line);
            }
        }
        return result.toString();
    }
}
