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

package org.jetbrains.jet.compiler.runner;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import com.intellij.util.SystemProperties;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;

public class KotlinCompilerRunner {
    public static void runCompiler(
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File scriptFile,
            OutputItemsCollector collector,
            boolean runOutOfProcess
    ) {
        if (runOutOfProcess) {
            runOutOfProcess(messageCollector, collector, environment, scriptFile);
        }
        else {
            runInProcess(messageCollector, collector, environment, scriptFile);
        }
    }

    private static void runInProcess(final MessageCollector messageCollector,
            OutputItemsCollector collector,
            final CompilerEnvironment environment,
            final File scriptFile) {
        CompilerRunnerUtil.outputCompilerMessagesAndHandleExitCode(messageCollector, collector, new Function<PrintStream, Integer>() {
            @Override
            public Integer fun(PrintStream stream) {
                return execInProcess(environment, scriptFile, stream, messageCollector);
            }
        });
    }

    private static int execInProcess(CompilerEnvironment environment, File scriptFile, PrintStream out, MessageCollector messageCollector) {
        try {
            String compilerClassName = "org.jetbrains.jet.cli.jvm.K2JVMCompiler";
            String[] arguments = commandLineArguments(environment.getOutput(), scriptFile);
            messageCollector.report(CompilerMessageSeverity.INFO,
                                    "Using kotlinHome=" + environment.getKotlinPaths().getHomePath(),
                                    CompilerMessageLocation.NO_LOCATION);
            messageCollector.report(CompilerMessageSeverity.INFO,
                               "Invoking in-process compiler " + compilerClassName + " with arguments " + Arrays.asList(arguments),
                               CompilerMessageLocation.NO_LOCATION);
            Object rc = CompilerRunnerUtil.invokeExecMethod(compilerClassName, arguments, environment,
                                                            messageCollector, out, /*usePreloader=*/true);
            // exec() returns a K2JVMCompiler.ExitCode object, that class is not accessible here,
            // so we take it's contents through reflection
            return CompilerRunnerUtil.getReturnCodeFromObject(rc);
        }
        catch (Throwable e) {
            CompilerOutputParser.reportException(messageCollector, e);
            return -1;
        }
    }

    private static String[] commandLineArguments(File outputDir, File scriptFile) {
        return new String[]{
                "-module", scriptFile.getAbsolutePath(),
                "-output", outputDir.getPath(),
                "-tags", "-verbose", "-version",
                "-notNullAssertions", "-notNullParamAssertions",
                "-noStdlib", "-noJdkAnnotations", "-noJdk"};
    }

    private static void runOutOfProcess(
            final MessageCollector messageCollector,
            final OutputItemsCollector itemCollector,
            CompilerEnvironment environment,
            File scriptFile
    ) {
        SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass("org.jetbrains.jet.cli.jvm.K2JVMCompiler");

        for (String arg : commandLineArguments(environment.getOutput(), scriptFile)) {
            params.getProgramParametersList().add(arg);
        }

        for (File jar : CompilerRunnerUtil.kompilerClasspath(environment.getKotlinPaths(), messageCollector)) {
            params.getClassPath().add(jar);
        }

        params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
        //        params.getVMParametersList().addParametersString("-agentlib:yjpagent=sampling");

        Sdk sdk = params.getJdk();

        GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(
                ((JavaSdkType) sdk.getSdkType()).getVMExecutablePath(sdk), params, false);

        messageCollector.report(CompilerMessageSeverity.INFO,
                                "Invoking out-of-process compiler with arguments: " + commandLine,
                                CompilerMessageLocation.NO_LOCATION);

        try {
            final Process process = commandLine.createProcess();

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    CompilerOutputParser
                            .parseCompilerMessagesFromReader(messageCollector, new InputStreamReader(process.getInputStream()),
                                                             itemCollector);
                }
            });

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileUtil.loadBytes(process.getErrorStream());
                    }
                    catch (IOException e) {
                        // Don't care
                    }
                }
            });

            int exitCode = process.waitFor();
            CompilerRunnerUtil.handleProcessTermination(exitCode, messageCollector);
        }
        catch (Exception e) {
            messageCollector.report(CompilerMessageSeverity.ERROR,
                                    "[Internal Error] " + e.getLocalizedMessage(),
                                    CompilerMessageLocation.NO_LOCATION);
            return;
        }
    }

}
