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

package org.jetbrains.kotlin.integration;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.OutputListener;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.KotlinVersion;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.Tmpdir;
import org.jetbrains.kotlin.utils.PathUtil;
import org.junit.ComparisonFailure;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public abstract class KotlinIntegrationTestBase {
    protected static final File INTEGRATION_TEST_DATA_BASE_DIR = new File(JetTestUtils.getTestDataPathBase(), "integration");

    @Rule
    public final Tmpdir tmpdir = new Tmpdir();

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @NotNull
    protected abstract File getTestDataDir();

    protected int runJava(String logName, String... arguments) throws Exception {
        GeneralCommandLine commandLine = new GeneralCommandLine().withWorkDirectory(getTestDataDir());
        commandLine.setExePath(getJavaRuntime().getAbsolutePath());
        commandLine.addParameters(arguments);

        StringBuilder executionLog = new StringBuilder();
        int exitCode = runProcess(commandLine, executionLog);

        if (logName == null) {
            assertEquals("Non-zero exit code", 0, exitCode);
        }
        else {
            check(logName, executionLog.toString());
        }

        return exitCode;
    }

    private static String normalizePath(String content, File baseDir, String pathId) {
        String contentWithRelativePaths = content.replace(baseDir.getAbsolutePath(), pathId);

        @Language("RegExp")
        String RELATIVE_PATH_WITH_MIXED_SEPARATOR = Pattern.quote(pathId) + "[-.\\w/\\\\]*";

        return KotlinPackage.replaceAll(contentWithRelativePaths, RELATIVE_PATH_WITH_MIXED_SEPARATOR, new Function1<MatchResult, String>() {
            @Override
            public String invoke(MatchResult mr) {
                return FileUtil.toSystemIndependentName(mr.group());
            }
        });
    }

    protected String normalizeOutput(String content) {
        content = normalizePath(content, getTestDataDir(), "[TestData]");
        content = normalizePath(content, tmpdir.getTmpDir(), "[Temp]");
        content = normalizePath(content, getCompilerLib(), "[CompilerLib]");
        content = normalizePath(content, getKotlinProjectHome(), "[KotlinProjectHome]");
        content = content.replaceAll(KotlinVersion.VERSION, "[KotlinVersion]");
        content = StringUtil.convertLineSeparators(content);
        return content;
    }

    private void check(String baseName, String content) throws IOException {
        File actualFile = new File(getTestDataDir(), baseName + ".actual");
        File expectedFile = new File(getTestDataDir(), baseName + ".expected");

        String normalizedContent = normalizeOutput(content);

        if (!expectedFile.isFile()) {
            Files.write(normalizedContent, actualFile, Charsets.UTF_8);
            fail("No .expected file " + expectedFile);
        }

        try {
            JetTestUtils.assertEqualsToFile(expectedFile, normalizedContent);
            //noinspection ResultOfMethodCallIgnored
            actualFile.delete();
        }
        catch (ComparisonFailure e) {
            Files.write(normalizedContent, actualFile, Charsets.UTF_8);
            throw e;
        }
    }

    private static int runProcess(GeneralCommandLine commandLine, StringBuilder executionLog) throws ExecutionException {
        OSProcessHandler handler =
                new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString(), commandLine.getCharset());

        StringBuilder outContent = new StringBuilder();
        StringBuilder errContent = new StringBuilder();

        handler.addProcessListener(new OutputListener(outContent, errContent));

        handler.startNotify();
        handler.waitFor();
        int exitCode = handler.getProcess().exitValue();

        appendIfNotEmpty(executionLog, "OUT:\n", outContent);
        appendIfNotEmpty(executionLog, "\nERR:\n", errContent);

        executionLog.append("\nReturn code: ").append(exitCode).append("\n");

        return exitCode;
    }

    private static void appendIfNotEmpty(StringBuilder executionLog, String prefix, StringBuilder content) {
        if (content.length() > 0) {
            executionLog.append(prefix);
            executionLog.append(content);
        }
    }

    private static File getJavaRuntime() {
        File javaHome = new File(System.getProperty("java.home"));
        String javaExe = SystemInfo.isWindows ? "java.exe" : "java";

        File runtime = new File(javaHome, "bin" + File.separator + javaExe);
        assertTrue("No java runtime at " + runtime, runtime.isFile());

        return runtime;
    }

    protected static File getCompilerLib() {
        File file = PathUtil.getKotlinPathsForDistDirectory().getLibPath().getAbsoluteFile();
        assertTrue("Lib directory doesn't exist. Run 'ant dist'", file.isDirectory());
        return file;
    }

    protected static File getKotlinProjectHome() {
        return new File(PathManager.getHomePath()).getParentFile();
    }
}
