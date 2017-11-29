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
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.AbstractForeignAnnotationsTestKt;
import org.jetbrains.kotlin.cli.common.CLITool;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.cli.js.dce.K2JSDce;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.KotlinCompilerVersion;
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion;
import org.jetbrains.kotlin.test.CompilerTestUtil;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.StringsKt;
import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCliTest extends TestCaseWithTmpdir {
    private static final String TESTDATA_DIR = "$TESTDATA_DIR$";

    public static Pair<String, ExitCode> executeCompilerGrabOutput(@NotNull CLITool<?> compiler, @NotNull List<String> args) {
        StringBuilder output = new StringBuilder();

        int index = 0;
        do {
            int next = args.subList(index, args.size()).indexOf("---");
            if (next == -1) {
                next = args.size();
            }
            Pair<String, ExitCode> pair = CompilerTestUtil.executeCompiler(compiler, args.subList(index, next));
            output.append(pair.getFirst());
            if (pair.getSecond() != ExitCode.OK) {
                return new Pair<>(output.toString(), pair.getSecond());
            }
            index = next + 1;
        }
        while (index < args.size());

        return new Pair<>(output.toString(), ExitCode.OK);
    }

    @NotNull
    public static String getNormalizedCompilerOutput(@NotNull String pureOutput, @NotNull ExitCode exitCode, @NotNull String testDataDir) {
        String testDataAbsoluteDir = new File(testDataDir).getAbsolutePath();
        String normalizedOutputWithoutExitCode = StringUtil.convertLineSeparators(pureOutput)
                .replace(testDataAbsoluteDir, TESTDATA_DIR)
                .replace(FileUtil.toSystemIndependentName(testDataAbsoluteDir), TESTDATA_DIR)
                .replace(PathUtil.getKotlinPathsForDistDirectory().getHomePath().getAbsolutePath(), "$PROJECT_DIR$")
                .replace("expected version is " + JvmMetadataVersion.INSTANCE, "expected version is $ABI_VERSION$")
                .replace("expected version is " + JsMetadataVersion.INSTANCE, "expected version is $ABI_VERSION$")
                .replace("\\", "/")
                .replace(KotlinCompilerVersion.VERSION, "$VERSION$");

        return normalizedOutputWithoutExitCode + exitCode + "\n";
    }

    private void doTest(@NotNull String fileName, @NotNull CLITool<?> compiler) {
        System.setProperty("java.awt.headless", "true");
        Pair<String, ExitCode> outputAndExitCode = executeCompilerGrabOutput(compiler, readArgs(fileName, tmpdir.getPath()));
        String actual = getNormalizedCompilerOutput(
                outputAndExitCode.getFirst(), outputAndExitCode.getSecond(), new File(fileName).getParent()
        );

        File outFile = new File(fileName.replaceFirst("\\.args$", ".out"));
        KotlinTestUtils.assertEqualsToFile(outFile, actual);

        File additionalTestConfig = new File(fileName.replaceFirst("\\.args$", ".test"));
        if (additionalTestConfig.exists()) {
            doTestAdditionalChecks(additionalTestConfig, fileName);
        }
    }

    private void doTestAdditionalChecks(@NotNull File testConfigFile, @NotNull String argsFilePath) {
        List<String> diagnostics = new ArrayList<>(0);
        String content = FilesKt.readText(testConfigFile, Charsets.UTF_8);

        List<String> existsList = InTextDirectivesUtils.findListWithPrefixes(content, "// EXISTS: ");
        for (String fileName : existsList) {
            File file = checkedPathToFile(fileName, argsFilePath);
            if (!file.exists()) {
                diagnostics.add("File does not exist, but should: " + fileName);
            }
            else if (!file.isFile()) {
                diagnostics.add("File is a directory, but should be a normal file: " + fileName);
            }
        }

        List<String> absentList = InTextDirectivesUtils.findListWithPrefixes(content, "// ABSENT: ");
        for (String fileName : absentList) {
            File file = checkedPathToFile(fileName, argsFilePath);
            if (file.exists() && file.isFile()) {
                diagnostics.add("File exists, but shouldn't: " + fileName);
            }
        }

        List<String> containsTextList = InTextDirectivesUtils.findLinesWithPrefixesRemoved(content, "// CONTAINS: ");
        for (String containsSpec : containsTextList) {
            String[] parts = containsSpec.split(",", 2);
            String fileName = parts[0].trim();
            String contentToSearch = parts[1].trim();
            File file = checkedPathToFile(fileName, argsFilePath);
            if (!file.exists()) {
                diagnostics.add("File does not exist: " + fileName);
            }
            else if (file.isDirectory()) {
                diagnostics.add("File is a directory: " + fileName);
            }
            else {
                String text = FilesKt.readText(file, Charsets.UTF_8);
                if (!text.contains(contentToSearch)) {
                    diagnostics.add("File " + fileName + " does not contain string: " + contentToSearch);
                }
            }
        }

        if (!diagnostics.isEmpty()) {
            diagnostics.add(0, diagnostics.size() + " problem(s) found:");
            Assert.fail(StringsKt.join(diagnostics, "\n"));
        }
    }

    @NotNull
    private File checkedPathToFile(@NotNull String path, @NotNull String argsFilePath) {
        if (path.startsWith(TESTDATA_DIR + "/")) {
            return new File(new File(argsFilePath).getParent(), path.substring(TESTDATA_DIR.length() + 1));
        }
        else {
            return new File(tmpdir, path);
        }
    }

    @NotNull
    private static List<String> readArgs(@NotNull String argsFilePath, @NotNull String tempDir) {
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
                    .replace(TESTDATA_DIR, new File(argsFilePath).getParent())
                    .replace(
                            "$FOREIGN_ANNOTATIONS_DIR$",
                            new File(AbstractForeignAnnotationsTestKt.getFOREIGN_ANNOTATIONS_SOURCES_PATH()).getPath()
                    );
        });
    }

    protected void doJvmTest(@NotNull String fileName) {
        doTest(fileName, new K2JVMCompiler());
    }

    protected void doJsTest(@NotNull String fileName) {
        doTest(fileName, new K2JSCompiler());
    }

    protected void doJsDceTest(@NotNull String fileName) {
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
