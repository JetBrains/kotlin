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
import kotlin.io.path.PathsKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.checkers.ThirdPartyAnnotationPathsKt;
import org.jetbrains.kotlin.cli.common.CLITool;
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.Usage;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.cli.js.dce.K2JSDce;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler;
import org.jetbrains.kotlin.config.KotlinCompilerVersion;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion;
import org.jetbrains.kotlin.test.CompilerTestUtil;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.StringsKt;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.cli.common.arguments.PreprocessCommandLineArgumentsKt.ARGFILE_ARGUMENT;

public abstract class AbstractCliTest extends TestCaseWithTmpdir {
    private static final String TESTDATA_DIR = "$TESTDATA_DIR$";

    private static final String BUILD_FILE_ARGUMENT_PREFIX = "-Xbuild-file=";

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
    public static String getNormalizedCompilerOutput(@NotNull String pureOutput, @Nullable ExitCode exitCode, @NotNull String testDataDir) {
        String testDataAbsoluteDir = new File(testDataDir).getAbsolutePath();
        String normalizedOutputWithoutExitCode = StringUtil.convertLineSeparators(pureOutput)
                .replace(testDataAbsoluteDir, TESTDATA_DIR)
                .replace(FileUtil.toSystemIndependentName(testDataAbsoluteDir), TESTDATA_DIR)
                .replace(PathUtil.getKotlinPathsForDistDirectory().getHomePath().getAbsolutePath(), "$PROJECT_DIR$")
                .replace(PathUtil.getKotlinPathsForDistDirectory().getHomePath().getParentFile().getAbsolutePath(), "$DIST_DIR$")
                .replace("expected version is " + JvmMetadataVersion.INSTANCE, "expected version is $ABI_VERSION$")
                .replace("expected version is " + JsMetadataVersion.INSTANCE, "expected version is $ABI_VERSION$")
                .replace("compiler version " + JvmMetadataVersion.INSTANCE, "compiler version $ABI_VERSION$")
                .replace("up to " + JvmMetadataVersion.INSTANCE, "up to $ABI_VERSION$")
                .replace("up to " + JvmMetadataVersion.INSTANCE_NEXT, "up to $ABI_VERSION_NEXT$")
                .replace("\\", "/")
                .replace(KotlinCompilerVersion.VERSION, "$VERSION$")
                .replace("\n" + Usage.BAT_DELIMITER_CHARACTERS_NOTE + "\n", "")
                .replaceAll("log4j:WARN.*\n", "");

        return exitCode == null ? normalizedOutputWithoutExitCode : (normalizedOutputWithoutExitCode + exitCode + "\n");
    }

    private void doTest(@NotNull String fileName, @NotNull CLITool<?> compiler) {
        System.setProperty("java.awt.headless", "true");

        File environmentTestConfig = new File(fileName.replaceFirst("\\.args$", ".env"));
        if (environmentTestConfig.exists()) {
            compiler.setReadingSettingsFromEnvironmentAllowed(true);
            CompilerSystemProperties.LANGUAGE_VERSION_SETTINGS.setValue(FilesKt.readText(environmentTestConfig, Charsets.UTF_8));
        }

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

        List<String> notContainsTextList = InTextDirectivesUtils.findLinesWithPrefixesRemoved(content, "// NOT_CONTAINS: ");
        for (String notContainsSpec : notContainsTextList) {
            String[] parts = notContainsSpec.split(",", 2);
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
                if (text.contains(contentToSearch)) {
                    diagnostics.add("File " + fileName + " contains string: " + contentToSearch);
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
    private static List<String> readArgs(@NotNull String testArgsFilePath, @NotNull String tempDir) {
        File testArgsFile = new File(testArgsFilePath);
        List<String> lines = FilesKt.readLines(testArgsFile, Charsets.UTF_8);
        return CollectionsKt.mapNotNull(lines, arg -> readArg(arg, testArgsFile.getParentFile().getAbsolutePath(), tempDir));
    }

    private static String readArg(String arg, @NotNull String testDataDir, @NotNull String tempDir) {
        if (arg.isEmpty()) {
            return null;
        }

        String argWithColonsReplaced = arg
                .replace("\\:", "$COLON$")
                .replace(":", File.pathSeparator)
                .replace("$COLON$", ":");

        String argWithTestPathsReplaced = replaceTestPaths(argWithColonsReplaced, testDataDir, tempDir);

        if (arg.startsWith(BUILD_FILE_ARGUMENT_PREFIX)) {
            return replacePathsInBuildXml(argWithTestPathsReplaced, testDataDir, tempDir);
        }

        if (arg.startsWith(ARGFILE_ARGUMENT)) {
            return createTempFileWithPathsReplaced(argWithTestPathsReplaced, ARGFILE_ARGUMENT, "", testDataDir, tempDir);
        }

        return argWithTestPathsReplaced;
    }

    @NotNull
    public static String replacePathsInBuildXml(@NotNull String argument, @NotNull String testDataDir, @NotNull String tempDir) {
        return createTempFileWithPathsReplaced(argument, BUILD_FILE_ARGUMENT_PREFIX, ".xml", testDataDir, tempDir);
    }

    // Create new temporary file with all test paths replaced and return the new argument value with the new file path
    @NotNull
    private static String createTempFileWithPathsReplaced(
            @NotNull String argument,
            @NotNull String argumentPrefix,
            @NotNull String tempFileSuffix,
            @NotNull String testDataDir,
            @NotNull String tempDir
    ) {
        String filePath = kotlin.text.StringsKt.substringAfter(argument, argumentPrefix, argument);
        Path file = Paths.get(filePath);
        if (!Files.exists(file)) return argument;

        try {
            Path result = Files.createTempFile(Paths.get(tempDir), file.getFileName().toString(), tempFileSuffix);
            String oldContent = PathsKt.readText(file, Charsets.UTF_8);
            String newContent = replaceTestPaths(oldContent, testDataDir, tempDir);
            PathsKt.writeText(result, newContent, Charsets.UTF_8);

            return argumentPrefix + result.toAbsolutePath();
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    private static String replaceTestPaths(@NotNull String str, @NotNull String testDataDir, @NotNull String tempDir) {
        return str
                .replace("$TEMP_DIR$", tempDir)
                .replace(TESTDATA_DIR, testDataDir)
                .replace(
                        "$FOREIGN_ANNOTATIONS_DIR$",
                        new File(ThirdPartyAnnotationPathsKt.FOREIGN_ANNOTATIONS_SOURCES_PATH).getPath()
                )
                .replace(
                        "$JSR_305_DECLARATIONS$",
                        new File(ThirdPartyAnnotationPathsKt.JSR_305_SOURCES_PATH).getPath()
                )
                .replace(
                        "$FOREIGN_JAVA8_ANNOTATIONS_DIR$",
                        new File(ThirdPartyAnnotationPathsKt.FOREIGN_JDK8_ANNOTATIONS_SOURCES_PATH).getPath()
                ).replace(
                        "$JDK_17$",
                        KtTestUtil.getJdk17Home().getPath()
                );
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

    protected void doMetadataTest(@NotNull String fileName) {
        doTest(fileName, new K2MetadataCompiler());
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
