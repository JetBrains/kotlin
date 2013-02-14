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

package org.jetbrains.jet.cli.jvm;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.jvm.compiler.CommandLineScriptUtils;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.jet.cli.jvm.repl.ReplFromTerminal;
import org.jetbrains.jet.codegen.BuiltinToJavaTypesMapping;
import org.jetbrains.jet.codegen.CompilationException;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.KotlinPathsFromHomeDir;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.cli.common.ExitCode.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class K2JVMCompiler extends CLICompiler<K2JVMCompilerArguments> {

    public static void main(String... args) {
        doMain(new K2JVMCompiler(), args);
    }

    @Override
    @NotNull
    protected ExitCode doExecute(K2JVMCompilerArguments arguments, MessageCollector messageCollector, Disposable rootDisposable) {
        KotlinPaths paths = arguments.kotlinHome != null
                                ? new KotlinPathsFromHomeDir(new File(arguments.kotlinHome))
                                : PathUtil.getKotlinPathsForCompiler();

        messageCollector.report(CompilerMessageSeverity.LOGGING,
                                "Using Kotlin home directory " + paths.getHomePath(), CompilerMessageLocation.NO_LOCATION);

        CompilerConfiguration configuration = new CompilerConfiguration();

        try {
            configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, getClasspath(paths, arguments));
            configuration.addAll(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, getAnnotationsPath(paths, arguments));
        }
        catch (Throwable t) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(t),
                                    CompilerMessageLocation.NO_LOCATION);
            return INTERNAL_ERROR;
        }

        final List<String> argumentsSourceDirs = arguments.getSourceDirs();
        if (!arguments.script &&
            arguments.module == null &&
            arguments.src == null &&
            arguments.freeArgs.isEmpty() &&
            (argumentsSourceDirs == null || argumentsSourceDirs.size() == 0)) {

            ReplFromTerminal.run(rootDisposable, configuration);
            return ExitCode.OK;
        }
        else if (arguments.module != null) {
        }
        else if (arguments.script) {
            configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, arguments.freeArgs.get(0));
        }
        else {
            // TODO ideally we'd unify to just having a single field that supports multiple files/dirs
            if (arguments.getSourceDirs() != null) {
                for (String source : arguments.getSourceDirs()) {
                    configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, source);
                }
            }
            else {
                if (arguments.src != null) {
                    List<String> sourcePathsSplitByPathSeparator
                            = Arrays.asList(arguments.src.split(StringUtil.escapeToRegexp(File.pathSeparator)));
                    configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, sourcePathsSplitByPathSeparator);
                }
                for (String freeArg : arguments.freeArgs) {
                    configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, freeArg);
                }
            }
        }

        boolean builtins = arguments.builtins;

        configuration.put(JVMConfigurationKeys.SCRIPT_PARAMETERS, arguments.script
                                                                          ? CommandLineScriptUtils.scriptParameters()
                                                                          : Collections.<AnalyzerScriptParameter>emptyList());
        configuration.put(JVMConfigurationKeys.STUBS, builtins);
        configuration.put(JVMConfigurationKeys.BUILTIN_TO_JAVA_TYPES_MAPPING_KEY,
                                  builtins ? BuiltinToJavaTypesMapping.DISABLED : BuiltinToJavaTypesMapping.ENABLED);

        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, arguments.notNullAssertions);
        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, arguments.notNullParamAssertions);

        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);

        messageCollector.report(CompilerMessageSeverity.LOGGING, "Configuring the compilation environment",
                                CompilerMessageLocation.NO_LOCATION);
        try {
            configureEnvironment(configuration, arguments);

            File jar = arguments.jar != null ? new File(arguments.jar) : null;
            File outputDir = arguments.outputDir != null ? new File(arguments.outputDir) : null;

            if (arguments.module != null) {
                MessageCollector sanitizedCollector = new FilteringMessageCollector(messageCollector);
                List<Module> modules = CompileEnvironmentUtil.loadModuleScript(paths, arguments.module, sanitizedCollector);

                File directory = new File(arguments.module).getParentFile();
                KotlinToJVMBytecodeCompiler.compileModules(configuration, modules,
                                                                      directory, jar, outputDir,
                                                                      arguments.includeRuntime);
            }
            else if (arguments.script) {
                List<String> scriptArgs = arguments.freeArgs.subList(1, arguments.freeArgs.size());
                JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, configuration);
                KotlinToJVMBytecodeCompiler.compileAndExecuteScript(paths, environment, scriptArgs);
            }
            else {
                JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, configuration);
                KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, jar, outputDir, arguments.includeRuntime);
            }
            return OK;
        }
        catch (CompilationException e) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(e),
                                    MessageUtil.psiElementToMessageLocation(e.getElement()));
            return INTERNAL_ERROR;
        }
    }


    /**
     * Allow derived classes to add additional command line arguments
     */
    @NotNull
    @Override
    protected K2JVMCompilerArguments createArguments() {
        return new K2JVMCompilerArguments();
    }

    // TODO this method is here only to workaround KT-2498
    @Override
    protected void configureEnvironment(@NotNull CompilerConfiguration configuration, @NotNull K2JVMCompilerArguments arguments) {
        super.configureEnvironment(configuration, arguments);
    }

    //TODO: Hacked! Be sure that our kotlin stuff builds correctly before you remove.
    // our compiler throws method not found error
    // probably relates to KT-1863... well, may be not
    @NotNull
    @Override
    public ExitCode exec(@NotNull PrintStream errStream, @NotNull K2JVMCompilerArguments arguments) {
        return super.exec(errStream, arguments);
    }

    @NotNull
    private static List<File> getClasspath(@NotNull KotlinPaths paths, @NotNull K2JVMCompilerArguments arguments) {
        List<File> classpath = Lists.newArrayList();
        if (!arguments.noJdk) {
            classpath.add(PathUtil.findRtJar());
        }
        if (!arguments.noStdlib) {
            classpath.add(paths.getRuntimePath());
        }
        if (arguments.classpath != null) {
            for (String element : Splitter.on(File.pathSeparatorChar).split(arguments.classpath)) {
                classpath.add(new File(element));
            }
        }
        return classpath;
    }

    @NotNull
    private static List<File> getAnnotationsPath(@NotNull KotlinPaths paths, @NotNull K2JVMCompilerArguments arguments) {
        List<File> annotationsPath = Lists.newArrayList();
        if (!arguments.noJdkAnnotations) {
            annotationsPath.add(paths.getJdkAnnotationsPath());
        }
        if (arguments.annotations != null) {
            for (String element : Splitter.on(File.pathSeparatorChar).split(arguments.annotations)) {
                annotationsPath.add(new File(element));
            }
        }
        return annotationsPath;
    }

    private static class FilteringMessageCollector implements MessageCollector {
        private final MessageCollector messageCollector;

        public FilteringMessageCollector(@NotNull MessageCollector messageCollector) {
            this.messageCollector = messageCollector;
        }

        @Override
        public void report(
                @NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location
        ) {
            if (!CompilerMessageSeverity.VERBOSE.contains(severity)) {
                messageCollector.report(severity, message, location);
            }
        }
    }
}
