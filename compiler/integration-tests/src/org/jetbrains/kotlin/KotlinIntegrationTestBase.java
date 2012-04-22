/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import org.apache.commons.lang.SystemUtils;
import sun.misc.JarFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.*;

public abstract class KotlinIntegrationTestBase {
    protected int runCompiler(String logName, String... arguments) throws Exception {
        final File lib = getCompilerLib();

        final File[] jars = lib.listFiles(new JarFilter());
        final String classpath = StringUtil.join(jars, new Function<File, String>() {
            @Override
            public String fun(File file) {
                return file.getAbsolutePath();
            }
        }, File.pathSeparator);

        Collection<String> javaArgs = new ArrayList<String>();
        javaArgs.add("-cp");
        javaArgs.add(classpath);
        javaArgs.add("org.jetbrains.jet.cli.KotlinCompiler");
        Collections.addAll(javaArgs, arguments);

        return runJava(logName, ArrayUtil.toStringArray(javaArgs));
    }

    protected int runJava(String logName, String... arguments) throws Exception {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setWorkDirectory(getTestDataDirectory());
        commandLine.setExePath(getJavaRuntime().getAbsolutePath());
        commandLine.addParameters(arguments);

        StringBuilder executionLog = new StringBuilder();
        int exitCode = runProcess(commandLine, executionLog);

        if (logName == null) {
            assertEquals("Non-zero exit code", 0, exitCode);
        }
        else {
            check(logName, executionLog);
        }

        return exitCode;
    }

    protected void check(String baseName, StringBuilder content) throws IOException {
        final File tmpFile = new File(getTestDataDirectory(), baseName + ".tmp");
        final File goldFile = new File(getTestDataDirectory(), baseName + ".gold");

        if (!goldFile.isFile()) {
            Files.write(content, tmpFile, Charsets.UTF_8);
            fail("No gold file " + goldFile);
        } else {
            final String goldContent = Files.toString(goldFile, UTF_8);
            if (!goldContent.equals(content.toString())) {
                Files.write(content, tmpFile, Charsets.UTF_8);
                fail("tmp and gold differ, tmp file: " + tmpFile);
            }
            tmpFile.delete();
        }
    }

    protected int runProcess(final GeneralCommandLine commandLine, final StringBuilder executionLog) throws ExecutionException {
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
                    outContent.append("OUT ");
                    outContent.append(event.getText());
                }
                else if (outputType == ProcessOutputTypes.STDERR) {
                    errContent.append("ERR ");
                    errContent.append(event.getText());
                }
            }
        });

        handler.startNotify();
        handler.waitFor();
        int exitCode = handler.getProcess().exitValue();

        executionLog.append(outContent);
        executionLog.append(errContent);
        executionLog.append("Return code: ").append(exitCode).append(SystemUtils.LINE_SEPARATOR);

        return exitCode;
    }

    protected static File getJavaRuntime() {
        final File javaHome = new File(System.getProperty("java.home"));
        final String javaExe = SystemInfo.isWindows ? "java.exe" : "java";

        final File runtime = new File(javaHome, "bin" + File.separator + javaExe);
        assertTrue("no java runtime at " + runtime, runtime.isFile());

        return runtime;
    }

    protected static File getCompilerLib() {
        final File file = new File(getKotlinProjectHome(), "dist" + File.separator + "kotlinc" + File.separator + "lib");
        assertTrue("no kotlin compiler lib at " + file, file.isDirectory());
        return file;
    }

    protected static File getTestDataDirectory() {
        return new File(getKotlinProjectHome(), "compiler" + File.separator + "integration-tests" + File.separator + "data");
    }

    protected static File getKotlinProjectHome() {
        return new File(PathManager.getHomePath()).getParentFile();
    }
}
