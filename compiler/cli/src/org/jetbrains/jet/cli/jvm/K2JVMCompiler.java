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
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.compiler.K2JVMCompileEnvironmentConfiguration;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
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
        File[] altHeadersClasspath;
        if (mode.includeAltHeaders()) {
            File path = PathUtil.getAltHeadersPath();
            File[] defaultAltHeadersPathArray;
            if (path != null) {
                defaultAltHeadersPathArray = new File[] {path};
            } else {
                // TODO should we throw an exception here instead?
                defaultAltHeadersPathArray = new File[0];
            }
            if (arguments.altHeaders != null) {
                File[] files = pathsToFiles(arguments.altHeaders);
                altHeadersClasspath = ArrayUtil.mergeArrays(files, defaultAltHeadersPathArray);
            }
            else {
                altHeadersClasspath = defaultAltHeadersPathArray;
            }
        }
        else {
            altHeadersClasspath = new File[0];
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

        CompilerDependencies dependencies = new CompilerDependencies(mode, CompilerDependencies.findRtJar(), altHeadersClasspath, runtimeJar);
        JetCoreEnvironment environment = JetCoreEnvironment.getCoreEnvironmentForJVM(rootDisposable, dependencies);
        K2JVMCompileEnvironmentConfiguration configuration =
                new K2JVMCompileEnvironmentConfiguration(environment, messageCollector);

        messageCollector.report(CompilerMessageSeverity.LOGGING, "Configuring the compilation environment",
                                CompilerMessageLocation.NO_LOCATION);
        try {
            configureEnvironment(configuration, arguments);

            boolean noErrors;
            if (arguments.module != null) {
                boolean oldVerbose = messageCollector.isVerbose();
                messageCollector.setVerbose(false);
                List<Module> modules = CompileEnvironmentUtil
                        .loadModuleScript(arguments.module, messageCollector);
                messageCollector.setVerbose(oldVerbose);
                File directory = new File(arguments.module).getParentFile();
                noErrors = KotlinToJVMBytecodeCompiler.compileModules(configuration, modules,
                                                                      directory, arguments.jar, arguments.outputDir,
                                                                      arguments.includeRuntime);
            }
            else {
                // TODO ideally we'd unify to just having a single field that supports multiple files/dirs
                if (arguments.getSourceDirs() != null) {
                    noErrors = KotlinToJVMBytecodeCompiler.compileBunchOfSourceDirectories(configuration,
                            arguments.getSourceDirs(), arguments.jar, arguments.outputDir, arguments.script, arguments.includeRuntime);
                }
                else {
                    List<String> sources = Lists.newArrayList();
                    if (arguments.src != null) {
                        sources.add(arguments.src);
                    }
                    sources.addAll(arguments.freeArgs);
                    noErrors = KotlinToJVMBytecodeCompiler.compileBunchOfSources(configuration,
                            sources, arguments.jar, arguments.outputDir, arguments.script, arguments.includeRuntime);
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
                if (arguments.mode.equalsIgnoreCase(variant.name().replace("_", ""))) {
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
            Iterable<String> classpath = Splitter.on(File.pathSeparatorChar).split(arguments.classpath);
            CompileEnvironmentUtil.addToClasspath(configuration.getEnvironment(), Iterables.toArray(classpath, String.class));
        }
    }

    private static File[] pathsToFiles(String paths) {
        return ContainerUtil.map(Iterables.toArray(Splitter.on(File.pathSeparatorChar).split(paths), String.class),
                                 new Function<String, File>() {
                                     @Override
                                     public File fun(String s) {
                                         return new File(s);
                                     }
                                 }, new File[0]);
    }
}
