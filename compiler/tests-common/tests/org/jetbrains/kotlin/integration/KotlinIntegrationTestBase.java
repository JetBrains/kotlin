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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.text.Regex;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.KotlinCompilerVersion;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public abstract class KotlinIntegrationTestBase extends TestCaseWithTmpdir {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    protected int runJava(@NotNull String testDataDir, @Nullable String logName, @NotNull String... arguments) throws Exception {
        GeneralCommandLine commandLine = new GeneralCommandLine().withWorkDirectory(testDataDir);
        commandLine.setExePath(getJavaRuntime().getAbsolutePath());
        commandLine.addParameters(arguments);

        StringBuilder executionLog = new StringBuilder();
        int exitCode = runProcess(commandLine, executionLog);

        if (logName == null) {
            assertEquals("Non-zero exit code", 0, exitCode);
        }
        else {
            check(testDataDir, logName, executionLog.toString());
        }

        return exitCode;
    }

    private static String normalizePath(String content, File baseDir, String pathId) {
        String contentWithRelativePaths = content.replace(baseDir.getAbsolutePath(), pathId);

        @Language("RegExp")
        String RELATIVE_PATH_WITH_MIXED_SEPARATOR = Regex.Companion.escape(pathId) + "[-.\\w/\\\\]*";

        return new Regex(RELATIVE_PATH_WITH_MIXED_SEPARATOR).replace(
                contentWithRelativePaths, mr -> FileUtil.toSystemIndependentName(mr.getValue())
        );
    }

    @NotNull
    protected String normalizeOutput(@NotNull File testDataDir, @NotNull String content) {
        content = normalizePath(content, testDataDir, "[TestData]");
        content = normalizePath(content, tmpdir, "[Temp]");
        content = normalizePath(content, getCompilerLib(), "[CompilerLib]");
        content = normalizePath(content, getKotlinProjectHome(), "[KotlinProjectHome]");
        content = content.replaceAll(Pattern.quote(KotlinCompilerVersion.VERSION), "[KotlinVersion]");
        content = content.replaceAll("\\(JRE .+\\)", "(JRE [JREVersion])");
        content = StringUtil.convertLineSeparators(content);
        return content;
    }

    private void check(String testDataDir, String baseName, String content) throws IOException {
        File expectedFile = new File(testDataDir, baseName + ".expected");
        String normalizedContent = normalizeOutput(new File(testDataDir), content);

        KotlinTestUtils.assertEqualsToFile(expectedFile, normalizedContent);
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

        appendIfNotEmpty(executionLog, "OUT:\n", outContent.toString());
        appendIfNotEmpty(executionLog, "\nERR:\n", errContent.toString());

        executionLog.append("\nReturn code: ").append(exitCode).append("\n");

        return exitCode;
    }

    private static void appendIfNotEmpty(StringBuilder executionLog, String prefix, String content) {
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

    public static File getCompilerLib() {
        File file = PathUtil.getKotlinPathsForDistDirectory().getLibPath().getAbsoluteFile();
        assertTrue("Lib directory doesn't exist. Run 'ant dist'", file.isDirectory());
        return file;
    }

    protected static File getKotlinProjectHome() {
        return new File(PathManager.getHomePath()).getParentFile();
    }

    private static class OutputListener extends ProcessAdapter {
        private final StringBuilder out;
        private final StringBuilder err;

        public OutputListener(@NotNull StringBuilder out, @NotNull StringBuilder err) {
            this.out = out;
            this.err = err;
        }

        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
            if (outputType == ProcessOutputTypes.STDERR) {
                err.append(event.getText());
            }
            else if (outputType == ProcessOutputTypes.SYSTEM) {
                // skip
            }
            else {
                out.append(event.getText());
            }
        }

        @Override
        public void processTerminated(ProcessEvent event) {}
    }
}