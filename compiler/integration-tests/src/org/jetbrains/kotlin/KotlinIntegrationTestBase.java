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
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.ArrayUtil;
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

    protected int runCompiler(String logName, String... arguments) throws Exception {
        File lib = getCompilerLib();

        String classpath = lib.getAbsolutePath() + File.separator + "kotlin-compiler.jar";

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

    private static String replacePath(String content, File path, String pathId) {
        String absolutePath = path.getAbsolutePath();

        return content
                .replace(absolutePath + "/", pathId + "/")
                .replace(absolutePath + "\\", pathId + "/")
                .replace(absolutePath, pathId);
    }

    protected String normalizeOutput(String content) {
        content = replacePath(content, testDataDir, "[TestData]");
        content = replacePath(content, tmpdir.getTmpDir(), "[Temp]");
        content = replacePath(content, getCompilerLib(), "[CompilerLib]");
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
            String goldContent = FileUtil.loadFile(expectedFile, CharsetToolkit.UTF8, true);
            try {
                assertEquals(goldContent, normalizedContent);
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

        final StringBuilder outContent = new StringBuilder();
        final StringBuilder errContent = new StringBuilder();

        handler.addProcessListener(new ProcessAdapter() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                if (outputType == ProcessOutputTypes.SYSTEM) {
                    System.out.print(event.getText());
                }
                else if (outputType == ProcessOutputTypes.STDOUT) {
                    appendToContent(outContent, "OUT ", event.getText());
                }
                else if (outputType == ProcessOutputTypes.STDERR) {
                    appendToContent(errContent, "ERR ", event.getText());
                }
            }

            private synchronized void appendToContent(StringBuilder content, String prefix, String line) {
                content.append(prefix);
                content.append(StringUtil.trimTrailing(line));
                content.append("\n");
            }
        });

        handler.startNotify();
        handler.waitFor();
        int exitCode = handler.getProcess().exitValue();

        executionLog.append(outContent);
        executionLog.append(errContent);
        executionLog.append("Return code: ").append(exitCode).append("\n");

        return exitCode;
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
