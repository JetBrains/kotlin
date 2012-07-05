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

package org.jetbrains.jet.cli.jvm;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.K2JVMCompileEnvironmentConfiguration;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.jet.cli.jvm.repl.ReplFromTerminal;
import org.jetbrains.jet.codegen.BuiltinToJavaTypesMapping;
import org.jetbrains.jet.codegen.CompilationException;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import static org.jetbrains.jet.cli.common.ExitCode.*;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class K2JVMCompiler extends CLICompiler<K2JVMCompilerArguments, K2JVMCompileEnvironmentConfiguration> {

    public static void main(String... args) {
        doMain(new K2JVMCompiler(), args);
    }

    @Override
    @NotNull
    protected ExitCode doExecute(K2JVMCompilerArguments arguments, PrintingMessageCollector messageCollector, Disposable rootDisposable) {
        CompilerConfiguration compilerConfiguration = createCompilerConfiguration(arguments);

        final List<String> argumentsSourceDirs = arguments.getSourceDirs();
        if (!arguments.script &&
            arguments.module == null &&
            arguments.src == null &&
            arguments.freeArgs.isEmpty() &&
            (argumentsSourceDirs == null || argumentsSourceDirs.size() == 0)) {

            ReplFromTerminal.run(rootDisposable, compilerConfiguration);
            return ExitCode.OK;
        }

        JetCoreEnvironment environment = JetCoreEnvironment.createCoreEnvironmentForJVM(rootDisposable, compilerConfiguration);
        boolean builtins = arguments.builtins;
        K2JVMCompileEnvironmentConfiguration configuration = new K2JVMCompileEnvironmentConfiguration(
                environment, messageCollector, arguments.script,
                builtins ? BuiltinsScopeExtensionMode.ONLY_STANDARD_CLASSES : BuiltinsScopeExtensionMode.ALL,
                builtins,
                builtins ? BuiltinToJavaTypesMapping.DISABLED : BuiltinToJavaTypesMapping.ENABLED);

        messageCollector.report(CompilerMessageSeverity.LOGGING, "Configuring the compilation environment",
                                CompilerMessageLocation.NO_LOCATION);
        try {
            configureEnvironment(configuration, arguments);

            File jar = arguments.jar != null ? new File(arguments.jar) : null;
            File outputDir = arguments.outputDir != null ? new File(arguments.outputDir) : null;

            boolean noErrors;
            if (arguments.module != null) {
                boolean oldVerbose = messageCollector.isVerbose();
                messageCollector.setVerbose(false);
                List<Module> modules = CompileEnvironmentUtil
                        .loadModuleScript(arguments.module, messageCollector);
                messageCollector.setVerbose(oldVerbose);
                File directory = new File(arguments.module).getParentFile();
                noErrors = KotlinToJVMBytecodeCompiler.compileModules(configuration, modules,
                                                                      directory, jar, outputDir,
                                                                      arguments.includeRuntime);
            }
            else if (arguments.script) {
                configuration.getEnvironment().addSources(arguments.freeArgs.get(0));
                List<String> scriptArgs = arguments.freeArgs.subList(1, arguments.freeArgs.size());
                noErrors = KotlinToJVMBytecodeCompiler.compileAndExecuteScript(configuration, scriptArgs);
            }
            else {
                // TODO ideally we'd unify to just having a single field that supports multiple files/dirs
                if (arguments.getSourceDirs() != null) {
                    noErrors = KotlinToJVMBytecodeCompiler.compileBunchOfSourceDirectories(configuration,
                            arguments.getSourceDirs(), jar, outputDir, arguments.script, arguments.includeRuntime);
                }
                else {
                    if (arguments.src != null) {
                        configuration.getEnvironment().addSources(arguments.src);
                    }
                    for (String freeArg : arguments.freeArgs) {
                        configuration.getEnvironment().addSources(freeArg);
                    }

                    noErrors = KotlinToJVMBytecodeCompiler.compileBunchOfSources(
                            configuration, jar, outputDir, arguments.includeRuntime);
                }
            }
            return noErrors ? OK : COMPILATION_ERROR;
        }
        catch (CompilationException e) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(e),
                                    MessageUtil.psiElementToMessageLocation(e.getElement()));
            return INTERNAL_ERROR;
        }
        catch (Throwable t) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(t),
                                    CompilerMessageLocation.NO_LOCATION);
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

    //TODO: Hacked! Be sure that our kotlin stuff builds correctly before you remove.
    // our compiler throws method not found error
    // probably relates to KT-1863... well, may be not
    @NotNull
    @Override
    public ExitCode exec(PrintStream errStream, K2JVMCompilerArguments arguments) {
        return super.exec(errStream, arguments);
    }


    @Override
    protected void configureEnvironment(@NotNull K2JVMCompileEnvironmentConfiguration configuration, @NotNull K2JVMCompilerArguments arguments) {
        super.configureEnvironment(configuration, arguments);

        configuration.getEnvironment().configure(createCompilerConfiguration(arguments));
    }

    @NotNull
    private static CompilerConfiguration createCompilerConfiguration(@NotNull K2JVMCompilerArguments arguments) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.putUserData(JVMConfigurationKeys.CLASSPATH_KEY, getClasspath(arguments).toArray(new File[0]));
        configuration.putUserData(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, getAnnotationsPath(arguments).toArray(new File[0]));
        return configuration;
    }

    @NotNull
    private static List<File> getClasspath(@NotNull K2JVMCompilerArguments arguments) {
        List<File> classpath = Lists.newArrayList();
        if (!arguments.noJdk) {
            classpath.add(PathUtil.findRtJar());
        }
        if (!arguments.noStdlib) {
            classpath.add(PathUtil.getDefaultRuntimePath());
        }
        if (arguments.classpath != null) {
            for (String element : Splitter.on(File.pathSeparatorChar).split(arguments.classpath)) {
                classpath.add(new File(element));
            }
        }
        return classpath;
    }

    @NotNull
    private static List<File> getAnnotationsPath(@NotNull K2JVMCompilerArguments arguments) {
        List<File> annotationsPath = Lists.newArrayList();
        if (!arguments.noJdkAnnotations) {
            annotationsPath.add(PathUtil.getJdkAnnotationsPath());
        }
        if (arguments.annotations != null) {
            for (String element : Splitter.on(File.pathSeparatorChar).split(arguments.classpath)) {
                annotationsPath.add(new File(element));
            }
        }
        return annotationsPath;
    }
}
