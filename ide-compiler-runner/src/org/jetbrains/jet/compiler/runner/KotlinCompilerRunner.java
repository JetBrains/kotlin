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

import com.intellij.openapi.util.text.StringUtil;
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
import org.jetbrains.jet.compiler.CompilerSettings;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

public class KotlinCompilerRunner {
    private static final String K2JVM_COMPILER = "org.jetbrains.jet.cli.jvm.K2JVMCompiler";
    private static final String K2JS_COMPILER = "org.jetbrains.jet.cli.js.K2JSCompiler";
    private static final K2JVMCompilerArguments DEFAULT_K2JVM_ARGUMENTS = new K2JVMCompilerArguments();
    private static final K2JSCompilerArguments DEFAULT_K2JS_ARGUMENTS = new K2JSCompilerArguments();

    public static void runK2JvmCompiler(
            CommonCompilerArguments commonArguments,
            K2JVMCompilerArguments k2jvmArguments,
            CompilerSettings compilerSettings,
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File moduleFile,
            OutputItemsCollector collector
    ) {
        K2JVMCompilerArguments arguments = mergeBeans(commonArguments, k2jvmArguments);
        setupK2JvmArguments(moduleFile, arguments);

        runCompiler(K2JVM_COMPILER, arguments, compilerSettings.getAdditionalArguments(),
                    DEFAULT_K2JVM_ARGUMENTS, messageCollector, collector, environment);
    }

    public static void runK2JsCompiler(
            CommonCompilerArguments commonArguments,
            K2JSCompilerArguments k2jsArguments,
            CompilerSettings compilerSettings,
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            OutputItemsCollector collector,
            Collection<File> sourceFiles,
            List<String> libraryFiles,
            File outputFile
    ) {
        K2JSCompilerArguments arguments = mergeBeans(commonArguments, k2jsArguments);
        setupK2JsArguments(outputFile, sourceFiles, libraryFiles, arguments);

        runCompiler(K2JS_COMPILER, arguments, compilerSettings.getAdditionalArguments(),
                    DEFAULT_K2JS_ARGUMENTS, messageCollector, collector, environment);
    }

    private static void runCompiler(
            final String compilerClassName,
            CommonCompilerArguments arguments,
            String additionalArguments,
            CommonCompilerArguments defaultArguments,
            final MessageCollector messageCollector,
            OutputItemsCollector collector,
            final CompilerEnvironment environment
    ) {
        final List<String> argumentsList = ArgumentUtils.convertArgumentsToStringList(arguments, defaultArguments);
        argumentsList.addAll(StringUtil.split(additionalArguments, " "));

        CompilerRunnerUtil.outputCompilerMessagesAndHandleExitCode(messageCollector, collector, new Function<PrintStream, Integer>() {
            @Override
            public Integer fun(PrintStream stream) {
                return execCompiler(compilerClassName, ArrayUtil.toStringArray(argumentsList), environment, stream, messageCollector);
            }
        });
    }

    private static int execCompiler(
            String compilerClassName,
            String[] arguments,
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
    }

    private static void setupK2JvmArguments(
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

    private static void setupK2JsArguments(
            File outputFile,
            Collection<File> sourceFiles,
            List<String> libraryFiles,
            K2JSCompilerArguments settings
    ) {
        setupCommonSettings(settings);

        settings.freeArgs = ContainerUtil.map(sourceFiles, new Function<File, String>() {
            @Override
            public String fun(File file) {
                return file.getPath();
            }
        });
        settings.outputFile = outputFile.getPath();
        settings.libraryFiles = ArrayUtil.toStringArray(libraryFiles);
    }
}
