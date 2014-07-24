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

package org.jetbrains.kotlin;

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
import com.intellij.util.ArrayUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.intellij.lang.annotations.Language;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.Tmpdir;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public abstract class KotlinIntegrationTestBase {
    protected File testDataDir;

    @Rule
    public final Tmpdir tmpdir = new Tmpdir();

    @Rule
    public TestRule watchman = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            File baseTestDataDir =
                    new File(getKotlinProjectHome(), "compiler" + File.separator + "integration-tests" + File.separator + "testData");
            testDataDir = new File(baseTestDataDir, description.getMethodName());
        }
    };

    static {
        System.setProperty("java.awt.headless", "true");
    }

    protected int runCompiler(String logName, String... arguments) throws Exception {
        File lib = getCompilerLib();

        String classpath = lib.getAbsolutePath() + File.separator +
                           "kotlin-compiler.jar" + File.pathSeparator +
                           getKotlinRuntimePath();

        Collection<String> javaArgs = new ArrayList<String>();
        javaArgs.add("-cp");
        javaArgs.add(classpath);
        javaArgs.add("org.jetbrains.jet.cli.jvm.K2JVMCompiler");
        Collections.addAll(javaArgs, arguments);

        return runJava(logName, ArrayUtil.toStringArray(javaArgs));
    }

    protected int runJava(String logName, String... arguments) throws Exception {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setWorkDirectory(testDataDir);
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
        content = normalizePath(content, testDataDir, "[TestData]");
        content = normalizePath(content, tmpdir.getTmpDir(), "[Temp]");
        content = normalizePath(content, getCompilerLib(), "[CompilerLib]");
        content = StringUtil.convertLineSeparators(content);
        return content;
    }

    protected void check(String baseName, String content) throws IOException {
        File actualFile = new File(testDataDir, baseName + ".actual");
        File expectedFile = new File(testDataDir, baseName + ".expected");

        String normalizedContent = normalizeOutput(content);

        if (!expectedFile.isFile()) {
            Files.write(normalizedContent, actualFile, Charsets.UTF_8);
            fail("No .expected file " + expectedFile);
        }
        else {
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
    }

    protected static int runProcess(GeneralCommandLine commandLine, StringBuilder executionLog) throws ExecutionException {
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

    protected static File getJavaRuntime() {
        File javaHome = new File(System.getProperty("java.home"));
        String javaExe = SystemInfo.isWindows ? "java.exe" : "java";

        File runtime = new File(javaHome, "bin" + File.separator + javaExe);
        assertTrue("no java runtime at " + runtime, runtime.isFile());

        return runtime;
    }

    protected static File getCompilerLib() {
        File file = new File(getKotlinProjectHome(), "dist" + File.separator + "kotlinc" + File.separator + "lib");
        assertTrue("no kotlin compiler lib at " + file, file.isDirectory());
        return file;
    }

    protected static String getKotlinRuntimePath() {
        File file = new File(getCompilerLib(), "kotlin-runtime.jar");
        assertTrue("no kotlin runtime at " + file, file.isFile());
        return file.getAbsolutePath();
    }

    protected static File getKotlinProjectHome() {
        return new File(PathManager.getHomePath()).getParentFile();
    }
}