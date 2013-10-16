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

import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.messages.MessageCollectorUtil;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

public class KotlinCompilerRunner {
    private static final String K2JVM_COMPILER = "org.jetbrains.jet.cli.jvm.K2JVMCompiler";
    private static final String K2JS_COMPILER = "org.jetbrains.jet.cli.js.K2JSCompiler";

    public static void runK2JvmCompiler(
            CommonCompilerArguments commonArguments,
            K2JVMCompilerArguments k2jvmArguments,
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File moduleFile,
            OutputItemsCollector collector
    ) {
        K2JVMCompilerArguments arguments = mergeBeans(commonArguments, k2jvmArguments);
        setupK2JvmSettings(moduleFile, arguments);

        runCompiler(K2JVM_COMPILER, arguments, messageCollector, collector, environment);
    }

    public static void runK2JsCompiler(
            CommonCompilerArguments commonArguments,
            K2JSCompilerArguments k2jsArguments,
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            OutputItemsCollector collector,
            List<File> sourceFiles,
            Set<String> libraryFiles,
            File outputFile
    ) {
        K2JSCompilerArguments arguments = mergeBeans(commonArguments, k2jsArguments);
        setupK2JsSettings(outputFile, sourceFiles, libraryFiles, arguments);

        runCompiler(K2JS_COMPILER, arguments, messageCollector, collector, environment);
    }

    private static void runCompiler(
            final String compilerClassName,
            final CommonCompilerArguments arguments,
            final MessageCollector messageCollector,
            OutputItemsCollector collector,
            final CompilerEnvironment environment
    ) {
        CompilerRunnerUtil.outputCompilerMessagesAndHandleExitCode(messageCollector, collector, new Function<PrintStream, Integer>() {
            @Override
            public Integer fun(PrintStream stream) {
                return execCompiler(compilerClassName, arguments, environment, stream, messageCollector);
            }
        });
    }

    private static int execCompiler(
            String compilerClassName,
            CommonCompilerArguments arguments,
            CompilerEnvironment environment,
            PrintStream out,
            MessageCollector messageCollector
    ) {
        try {
            messageCollector.report(CompilerMessageSeverity.INFO,
                                    "Using kotlinHome=" + environment.getKotlinPaths().getHomePath(),
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

    private static <F, T extends F> T mergeBeans(F from, T to) {
        T copy = XmlSerializerUtil.createCopy(to);

        for (Accessor accessor : XmlSerializerUtil.getAccessors(from.getClass())) {
            accessor.write(copy, accessor.read(from));
        }

        return copy;
    }

    private static void setupCommonSettings(CommonCompilerArguments settings) {
        settings.tags = true;
        settings.verbose = true;
        settings.version = true;
        settings.printArgs = true;
    }

    private static void setupK2JvmSettings(
            File moduleFile,
            K2JVMCompilerArguments settings
    ) {
        setupCommonSettings(settings);

        settings.module = moduleFile.getAbsolutePath();
        settings.notNullAssertions = true;
        settings.notNullParamAssertions = true;
        settings.noStdlib = true;
        settings.noJdkAnnotations = true;
        settings.noJdk = true;
    }

    private static void setupK2JsSettings(
            File outputFile,
            List<File> sourceFiles,
            Set<String> libraryFiles,
            K2JSCompilerArguments settings
    ) {
        setupCommonSettings(settings);

        List<String> sourceFilePaths = ContainerUtil.map(sourceFiles, new Function<File, String>() {
            @Override
            public String fun(File file) {
                return file.getPath();
            }
        });
        settings.sourceFiles = ArrayUtil.toStringArray(sourceFilePaths);
        settings.outputFile = outputFile.getPath();
        settings.libraryFiles = ArrayUtil.toStringArray(libraryFiles);
        //TODO drop later
        settings.sourcemap = true;
    }
}
