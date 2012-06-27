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
import com.google.common.collect.Iterables;
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
import org.jetbrains.jet.codegen.CompilationException;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
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

        CompilerSpecialMode mode = parseCompilerSpecialMode(arguments);
        File jdkHeadersJar;
        if (mode.includeJdkHeaders()) {
            if (arguments.jdkHeaders != null) {
                jdkHeadersJar = new File(arguments.jdkHeaders);
            }
            else {
                jdkHeadersJar = PathUtil.getAltHeadersPath();
            }
        }
        else {
            jdkHeadersJar = null;
        }
        File runtimeJar;

        if (mode.includeKotlinRuntime()) {
            if (arguments.stdlib != null) {
                runtimeJar = new File(arguments.stdlib);
            }
            else {
                runtimeJar = PathUtil.getDefaultRuntimePath();
            }
        }
        else {
            runtimeJar = null;
        }

        CompilerDependencies dependencies = new CompilerDependencies(mode, CompilerDependencies.findRtJar(), jdkHeadersJar, runtimeJar);

        final List<String> argumentsSourceDirs = arguments.getSourceDirs();
        if (!arguments.script &&
            arguments.module == null &&
            arguments.src == null &&
            arguments.freeArgs.isEmpty() &&
            (argumentsSourceDirs == null || argumentsSourceDirs.size() == 0)) {

            ReplFromTerminal.run(rootDisposable, dependencies, getClasspath(arguments));
            return ExitCode.OK;
        }

        JetCoreEnvironment environment = JetCoreEnvironment.getCoreEnvironmentForJVM(rootDisposable, dependencies);
        K2JVMCompileEnvironmentConfiguration configuration = new K2JVMCompileEnvironmentConfiguration(environment, messageCollector, arguments.script);

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

    @NotNull
    private static CompilerSpecialMode parseCompilerSpecialMode(@NotNull K2JVMCompilerArguments arguments) {
        if (arguments.mode == null) {
            return CompilerSpecialMode.REGULAR;
        }
        else {
            for (CompilerSpecialMode variant : CompilerSpecialMode.values()) {
                if (arguments.mode.equalsIgnoreCase(variant.name().replaceAll("_", ""))) {
                    return variant;
                }
            }
        }
        // TODO: report properly
        throw new IllegalArgumentException("unknown compiler mode: " + arguments.mode);
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

        if (configuration.getEnvironment().getCompilerDependencies().getRuntimeJar() != null) {
            CompileEnvironmentUtil.addToClasspath(configuration.getEnvironment(),
                                                  configuration.getEnvironment().getCompilerDependencies().getRuntimeJar());
        }

        if (arguments.classpath != null) {
            List<File> classpath = getClasspath(arguments);
            CompileEnvironmentUtil.addToClasspath(configuration.getEnvironment(), Iterables.toArray(classpath, File.class));
        }

        if (arguments.annotations != null) {
            for (String root : Splitter.on(File.pathSeparatorChar).split(arguments.annotations)) {
                JetCoreEnvironment.addExternalAnnotationsRoot(PathUtil.jarFileOrDirectoryToVirtualFile(new File(root)));
            }
        }
    }

    @NotNull
    private static List<File> getClasspath(@NotNull K2JVMCompilerArguments arguments) {
        List<File> classpath = Lists.newArrayList();
        if (arguments.classpath != null) {
            for (String element : Splitter.on(File.pathSeparatorChar).split(arguments.classpath)) {
                classpath.add(new File(element));
            }
        }
        return classpath;
    }
}
