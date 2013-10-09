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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.SystemProperties;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.messages.MessageCollectorUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

public class KotlinCompilerRunner {
    private static final String K2JVM_COMPILER = "org.jetbrains.jet.cli.jvm.K2JVMCompiler";
    private static final String K2JS_COMPILER = "org.jetbrains.jet.cli.js.K2JSCompiler";

    public static void runK2JvmCompiler(
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File moduleFile,
            OutputItemsCollector collector,
            boolean runOutOfProcess
    ) {
        String[] arguments = createArgumentsForJvmCompiler(moduleFile);

        if (runOutOfProcess) {
            runOutOfProcess(K2JVM_COMPILER, arguments, messageCollector, collector, environment);
        }
        else {
            runInProcess(K2JVM_COMPILER, arguments, messageCollector, collector, environment);
        }
    }

    public static void runK2JsCompiler(
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            OutputItemsCollector collector,
            List<File> sourceFiles,
            Set<String> libraryFiles,
            File outputFile
    ) {
        String[] arguments = createArgumentsForJsCompiler(outputFile, sourceFiles, libraryFiles);
        runInProcess(K2JS_COMPILER, arguments, messageCollector, collector, environment);
    }

    private static void runInProcess(
            final String compilerClassName,
            final String[] arguments,
            final MessageCollector messageCollector,
            OutputItemsCollector collector,
            final CompilerEnvironment environment) {
        CompilerRunnerUtil.outputCompilerMessagesAndHandleExitCode(messageCollector, collector, new Function<PrintStream, Integer>() {
            @Override
            public Integer fun(PrintStream stream) {
                return execInProcess(compilerClassName, arguments, environment, stream, messageCollector);
            }
        });
    }

    private static int execInProcess(String compilerClassName, String[] arguments, CompilerEnvironment environment, PrintStream out, MessageCollector messageCollector) {
        try {
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
            MessageCollectorUtil.reportException(messageCollector, e);
            return -1;
        }
    }

    private static String[] createArgumentsForJvmCompiler(File moduleFile) {
        return new String[]{
                "-module", moduleFile.getAbsolutePath(),
                "-tags", "-verbose", "-version",
                "-notNullAssertions", "-notNullParamAssertions",
                "-noStdlib", "-noJdkAnnotations", "-noJdk"};
    }

    private static String[] createArgumentsForJsCompiler(
            File outputFile,
            List<File> sourceFiles,
            Set<String> libraryFiles
    ) {
        List<String> args = new ArrayList<String>();

        Collections.addAll(args, "-tags", "-verbose", "-version", "-sourcemap");

        String separator = ",";
        String sourceFilesAsString = StringUtil.join(sourceFiles, new Function<File, String>() {
            @Override
            public String fun(File file) {
                return file.getPath();
            }
        }, separator);

        args.add("-sourceFiles");
        args.add(sourceFilesAsString);

        args.add("-output");
        args.add(outputFile.getPath());

        args.add("-libraryFiles");
        args.add(StringUtil.join(libraryFiles, separator));

        return ArrayUtil.toStringArray(args);
    }

    private static void runOutOfProcess(
            String compilerClassName,
            String[] arguments,
            final MessageCollector messageCollector,
            final OutputItemsCollector itemCollector,
            CompilerEnvironment environment
    ) {
        SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass(compilerClassName);

        for (String arg : arguments) {
            params.getProgramParametersList().add(arg);
        }

        for (File jar : CompilerRunnerUtil.kompilerClasspath(environment.getKotlinPaths(), messageCollector)) {
            params.getClassPath().add(jar);
        }

        params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
        //        params.getVMParametersList().addParametersString("-agentlib:yjpagent=sampling");

        Sdk sdk = params.getJdk();

        assert sdk != null;

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
        }
    }

}
