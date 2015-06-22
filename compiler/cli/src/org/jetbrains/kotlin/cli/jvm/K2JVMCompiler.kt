/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.*;
import org.jetbrains.kotlin.cli.common.modules.ModuleScriptData;
import org.jetbrains.kotlin.cli.jvm.compiler.*;
import org.jetbrains.kotlin.cli.jvm.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.cli.jvm.repl.ReplFromTerminal;
import org.jetbrains.kotlin.codegen.CompilationException;
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException;
import org.jetbrains.kotlin.compiler.plugin.PluginCliOptionProcessingException;
import org.jetbrains.kotlin.compiler.plugin.PluginPackage;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.IncrementalCompilation;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCacheProvider;
import org.jetbrains.kotlin.resolve.AnalyzerScriptParameter;
import org.jetbrains.kotlin.util.PerformanceCounter;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Predicates.in;
import static org.jetbrains.kotlin.cli.common.ExitCode.*;
import static org.jetbrains.kotlin.cli.jvm.config.ConfigPackage.addJavaSourceRoot;
import static org.jetbrains.kotlin.cli.jvm.config.ConfigPackage.addJvmClasspathRoots;
import static org.jetbrains.kotlin.config.ConfigPackage.addKotlinSourceRoot;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class K2JVMCompiler extends CLICompiler<K2JVMCompilerArguments> {
    private final long initStartNanos = System.nanoTime();

    public static void main(String... args) {
        doMain(new K2JVMCompiler(), args);
    }

    @Override
    @NotNull
    protected ExitCode doExecute(
            @NotNull K2JVMCompilerArguments arguments,
            @NotNull Services services,
            @NotNull MessageCollector messageCollector,
            @NotNull Disposable rootDisposable
    ) {
        MessageSeverityCollector messageSeverityCollector = new MessageSeverityCollector(messageCollector);
        KotlinPaths paths = arguments.kotlinHome != null
                                ? new KotlinPathsFromHomeDir(new File(arguments.kotlinHome))
                                : PathUtil.getKotlinPathsForCompiler();

        messageSeverityCollector.report(CompilerMessageSeverity.LOGGING,
                                "Using Kotlin home directory " + paths.getHomePath(), CompilerMessageLocation.NO_LOCATION);

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageSeverityCollector);

        if (IncrementalCompilation.ENABLED) {
            IncrementalCacheProvider incrementalCacheProvider = services.get(IncrementalCacheProvider.class);
            if (incrementalCacheProvider != null) {
                configuration.put(JVMConfigurationKeys.INCREMENTAL_CACHE_PROVIDER, incrementalCacheProvider);
            }
        }

        CompilerJarLocator locator = services.get(CompilerJarLocator.class);
        if (locator != null) {
            configuration.put(JVMConfigurationKeys.COMPILER_JAR_LOCATOR, locator);
        }

        try {
            if (!arguments.noJdk) {
                addJvmClasspathRoots(configuration, PathUtil.getJdkClassesRoots());
            }
        }
        catch (Throwable t) {
            MessageCollectorUtil.reportException(messageSeverityCollector, t);
            return INTERNAL_ERROR;
        }

        try {
            PluginCliParser.loadPlugins(arguments, configuration);
        }
        catch (PluginCliOptionProcessingException e) {
            String message = e.getMessage() + "\n\n" + PluginPackage.cliPluginUsageString(e.getPluginId(), e.getOptions());
            messageSeverityCollector.report(CompilerMessageSeverity.ERROR, message, CompilerMessageLocation.NO_LOCATION);
            return INTERNAL_ERROR;
        }
        catch (CliOptionProcessingException e) {
            messageSeverityCollector.report(CompilerMessageSeverity.ERROR, e.getMessage(), CompilerMessageLocation.NO_LOCATION);
            return INTERNAL_ERROR;
        }
        catch (Throwable t) {
            MessageCollectorUtil.reportException(messageSeverityCollector, t);
            return INTERNAL_ERROR;
        }

        if (arguments.script) {
            if (arguments.freeArgs.isEmpty()) {
                messageSeverityCollector.report(CompilerMessageSeverity.ERROR, "Specify script source path to evaluate",
                                        CompilerMessageLocation.NO_LOCATION);
                return COMPILATION_ERROR;
            }
            addKotlinSourceRoot(configuration, arguments.freeArgs.get(0));
        }
        else if (arguments.module == null) {
            for (String arg : arguments.freeArgs) {
                addKotlinSourceRoot(configuration, arg);
                File file = new File(arg);
                if (file.isDirectory()) {
                    addJavaSourceRoot(configuration, file);
                }
            }
        }

        addJvmClasspathRoots(configuration, getClasspath(paths, arguments));

        configuration.addAll(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, getAnnotationsPath(paths, arguments));

        if (arguments.module == null && arguments.freeArgs.isEmpty() && !arguments.version) {
            ReplFromTerminal.run(rootDisposable, configuration);
            return ExitCode.OK;
        }

        configuration.put(JVMConfigurationKeys.SCRIPT_PARAMETERS, arguments.script
                                                                  ? CommandLineScriptUtils.scriptParameters()
                                                                  : Collections.<AnalyzerScriptParameter>emptyList());

        putAdvancedOptions(configuration, arguments);

        messageSeverityCollector.report(CompilerMessageSeverity.LOGGING, "Configuring the compilation environment",
                                CompilerMessageLocation.NO_LOCATION);
        try {
            configureEnvironment(configuration, arguments);

            String destination = arguments.destination;

            File jar;
            File outputDir;
            if (destination != null) {
                boolean isJar = destination.endsWith(".jar");
                jar = isJar ? new File(destination) : null;
                outputDir = isJar ? null : new File(destination);
            }
            else {
                jar = null;
                outputDir = null;
            }
            final KotlinCoreEnvironment environment;

            if (arguments.module != null) {
                MessageCollector sanitizedCollector = new FilteringMessageCollector(messageSeverityCollector, in(CompilerMessageSeverity.VERBOSE));
                ModuleScriptData moduleScript = CompileEnvironmentUtil.loadModuleDescriptions(arguments.module, sanitizedCollector);

                if (outputDir != null) {
                    messageSeverityCollector.report(CompilerMessageSeverity.WARNING,
                                            "The '-d' option with a directory destination is ignored because '-module' is specified",
                                            CompilerMessageLocation.NO_LOCATION);
                }

                File directory = new File(arguments.module).getAbsoluteFile().getParentFile();

                CompilerConfiguration compilerConfiguration = KotlinToJVMBytecodeCompiler
                        .createCompilerConfiguration(configuration, moduleScript.getModules(), directory);
                environment = createCoreEnvironment(rootDisposable, compilerConfiguration);

                KotlinToJVMBytecodeCompiler.compileModules(
                        environment, configuration, moduleScript.getModules(), directory, jar, arguments.includeRuntime);
            }
            else if (arguments.script) {
                List<String> scriptArgs = arguments.freeArgs.subList(1, arguments.freeArgs.size());
                environment = createCoreEnvironment(rootDisposable, configuration);
                KotlinToJVMBytecodeCompiler.compileAndExecuteScript(configuration, paths, environment, scriptArgs);
            }
            else {
                environment = createCoreEnvironment(rootDisposable, configuration);

                if (messageSeverityCollector.anyReported(CompilerMessageSeverity.ERROR)) {
                    return COMPILATION_ERROR;
                }

                if (environment.getSourceFiles().isEmpty()) {
                    messageSeverityCollector.report(CompilerMessageSeverity.ERROR, "No source files", CompilerMessageLocation.NO_LOCATION);
                    return COMPILATION_ERROR;
                }

                KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, jar, outputDir, arguments.includeRuntime);
            }

            if (arguments.reportPerf) {
                PerformanceCounter.Companion.report(new Function1<String, Unit>() {
                    @Override
                    public Unit invoke(String s) {
                        reportPerf(environment.getConfiguration(), s);
                        return Unit.INSTANCE$;
                    }
                });
            }
            return OK;
        }
        catch (CompilationException e) {
            messageSeverityCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                                    MessageUtil.psiElementToMessageLocation(e.getElement()));
            return INTERNAL_ERROR;
        }
    }

    private KotlinCoreEnvironment createCoreEnvironment(
            @NotNull Disposable rootDisposable,
            @NotNull CompilerConfiguration configuration) {
        KotlinCoreEnvironment result = KotlinCoreEnvironment.createForProduction(
                rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        long initNanos = System.nanoTime() - initStartNanos;
        reportPerf(configuration, "INIT: Compiler initialized in " + TimeUnit.NANOSECONDS.toMillis(initNanos) + " ms");
        return result;
    }

    public static void reportPerf(CompilerConfiguration configuration, String message) {
        MessageCollector collector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);
        assert collector != null;
        collector.report(CompilerMessageSeverity.INFO, "PERF: " + message, CompilerMessageLocation.NO_LOCATION);
    }

    private static void putAdvancedOptions(@NotNull CompilerConfiguration configuration, @NotNull K2JVMCompilerArguments arguments) {
        configuration.put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, arguments.noCallAssertions);
        configuration.put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, arguments.noParamAssertions);
        configuration.put(JVMConfigurationKeys.DISABLE_INLINE, arguments.noInline);
        configuration.put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, arguments.noOptimize);
    }

    /**
     * Allow derived classes to add additional command line arguments
     */
    @NotNull
    @Override
    protected K2JVMCompilerArguments createArguments() {
        return new K2JVMCompilerArguments();
    }

    @NotNull
    private static List<File> getClasspath(@NotNull KotlinPaths paths, @NotNull K2JVMCompilerArguments arguments) {
        List<File> classpath = Lists.newArrayList();
        if (arguments.classpath != null) {
            for (String element : Splitter.on(File.pathSeparatorChar).split(arguments.classpath)) {
                classpath.add(new File(element));
            }
        }
        if (!arguments.noStdlib) {
            classpath.add(paths.getRuntimePath());
        }
        return classpath;
    }

    @NotNull
    private static List<File> getAnnotationsPath(@NotNull KotlinPaths paths, @NotNull K2JVMCompilerArguments arguments) {
        List<File> annotationsPath = Lists.newArrayList();
        if (arguments.annotations != null) {
            for (String element : Splitter.on(File.pathSeparatorChar).split(arguments.annotations)) {
                annotationsPath.add(new File(element));
            }
        }
        if (!arguments.noJdkAnnotations) {
            annotationsPath.add(paths.getJdkAnnotationsPath());
        }
        return annotationsPath;
    }
}
