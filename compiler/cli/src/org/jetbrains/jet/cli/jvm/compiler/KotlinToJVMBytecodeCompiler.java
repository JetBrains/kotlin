/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;
import kotlin.modules.AllModules;
import kotlin.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.asJava.FilteredJvmDiagnostics;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.CompilerPluginContext;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.inline.InlineCodegenUtil;
import org.jetbrains.jet.codegen.optimization.OptimizationUtils;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.IncrementalCache;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.IncrementalCacheProvider;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.IncrementalPackage;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.MainFunctionDetector;
import org.jetbrains.jet.utils.KotlinPaths;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KotlinToJVMBytecodeCompiler {

    private KotlinToJVMBytecodeCompiler() {
    }

    @NotNull
    private static List<String> getAbsolutePaths(@NotNull File directory, @NotNull Module module) {
        List<String> result = Lists.newArrayList();

        for (String sourceFile : module.getSourceFiles()) {
            File source = new File(sourceFile);
            if (!source.isAbsolute()) {
                source = new File(directory, sourceFile);
            }

            if (!source.exists()) {
                throw new CompileEnvironmentException("'" + source + "' does not exist in module " + module.getModuleName());
            }

            result.add(source.getAbsolutePath());
        }
        return result;
    }

    private static void writeOutput(
            @NotNull CompilerConfiguration configuration,
            @NotNull ClassFileFactory outputFiles,
            @Nullable File outputDir,
            @Nullable File jarPath,
            boolean jarRuntime,
            @Nullable FqName mainClass
    ) {
        MessageCollector messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE);
        CompileEnvironmentUtil.writeOutputToDirOrJar(jarPath, outputDir, jarRuntime, mainClass, outputFiles, messageCollector);
    }

    public static boolean compileModules(
            @NotNull CompilerConfiguration configuration,
            @NotNull List<Module> chunk,
            @NotNull File directory,
            @Nullable File jarPath,
            boolean jarRuntime
    ) {
        Map<Module, ClassFileFactory> outputFiles = Maps.newHashMap();

        CompilerConfiguration compilerConfiguration = createCompilerConfiguration(configuration, chunk, directory);

        Disposable parentDisposable = Disposer.newDisposable();
        JetCoreEnvironment environment = null;
        try {
            environment = JetCoreEnvironment.createForProduction(parentDisposable, compilerConfiguration);

            AnalyzeExhaust exhaust = analyze(environment);
            if (exhaust == null) {
                return false;
            }

            exhaust.throwIfError();

            for (Module module : chunk) {
                List<JetFile> jetFiles = CompileEnvironmentUtil.getJetFiles(
                        environment.getProject(), getAbsolutePaths(directory, module), new Function1<String, Unit>() {
                            @Override
                            public Unit invoke(String s) {
                                throw new IllegalStateException("Should have been checked before: " + s);
                            }
                        }
                );
                GenerationState generationState =
                        generate(environment, exhaust, jetFiles, module.getModuleName(), new File(module.getOutputDirectory()));
                outputFiles.put(module, generationState.getFactory());
            }
        }
        finally {
            if (environment != null) {
                Disposer.dispose(parentDisposable);
            }
        }

        for (Module module : chunk) {
            writeOutput(configuration, outputFiles.get(module), new File(module.getOutputDirectory()), jarPath, jarRuntime, null);
        }
        return true;
    }

    @NotNull
    private static CompilerConfiguration createCompilerConfiguration(
            @NotNull CompilerConfiguration base,
            @NotNull List<Module> chunk,
            @NotNull File directory
    ) {
        CompilerConfiguration configuration = base.copy();
        for (Module module : chunk) {
            configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, getAbsolutePaths(directory, module));

            for (String classpathRoot : module.getClasspathRoots()) {
                configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, new File(classpathRoot));
            }

            for (String annotationsRoot : module.getAnnotationsRoots()) {
                configuration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, new File(annotationsRoot));
            }

            configuration.add(JVMConfigurationKeys.MODULE_IDS, module.getModuleName());
        }

        return configuration;
    }

    @Nullable
    private static FqName findMainClass(@NotNull GenerationState generationState, @NotNull List<JetFile> files) {
        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(generationState.getBindingContext());
        FqName mainClass = null;
        for (JetFile file : files) {
            if (mainFunctionDetector.hasMain(file.getDeclarations())) {
                if (mainClass != null) {
                    // more than one main
                    return null;
                }
                FqName fqName = file.getPackageFqName();
                mainClass = PackageClassUtils.getPackageClassFqName(fqName);
            }
        }
        return mainClass;
    }

    public static boolean compileBunchOfSources(
            @NotNull JetCoreEnvironment environment,
            @Nullable File jar,
            @Nullable File outputDir,
            boolean includeRuntime
    ) {

        GenerationState generationState = analyzeAndGenerate(environment);
        if (generationState == null) {
            return false;
        }

        FqName mainClass = findMainClass(generationState, environment.getSourceFiles());

        try {
            writeOutput(environment.getConfiguration(), generationState.getFactory(), outputDir, jar, includeRuntime, mainClass);
            return true;
        }
        finally {
            generationState.destroy();
        }
    }

    public static void compileAndExecuteScript(
            @NotNull KotlinPaths paths,
            @NotNull JetCoreEnvironment environment,
            @NotNull List<String> scriptArgs
    ) {
        Class<?> scriptClass = compileScript(paths, environment);
        if (scriptClass == null) return;

        try {
            scriptClass.getConstructor(String[].class).newInstance(new Object[] {ArrayUtil.toStringArray(scriptArgs)});
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to evaluate script: " + e, e);
        }
    }

    @Nullable
    public static Class<?> compileScript(@NotNull KotlinPaths paths, @NotNull JetCoreEnvironment environment) {
        List<AnalyzerScriptParameter> scriptParameters = environment.getConfiguration().getList(JVMConfigurationKeys.SCRIPT_PARAMETERS);
        if (!scriptParameters.isEmpty()) {
            JetScriptDefinitionProvider.getInstance(environment.getProject()).addScriptDefinition(
                    new JetScriptDefinition(".kts", scriptParameters)
            );
        }
        GenerationState state = analyzeAndGenerate(environment);
        if (state == null) {
            return null;
        }

        GeneratedClassLoader classLoader;
        try {
            classLoader = new GeneratedClassLoader(state.getFactory(),
                                                   new URLClassLoader(new URL[] {
                                                           // TODO: add all classpath
                                                           paths.getRuntimePath().toURI().toURL()
                                                   }, AllModules.class.getClassLoader())
            );

            FqName nameForScript = ScriptNameUtil.classNameForScript(environment.getSourceFiles().get(0).getScript());
            return classLoader.loadClass(nameForScript.asString());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to evaluate script: " + e, e);
        }
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(@NotNull JetCoreEnvironment environment) {
        AnalyzeExhaust exhaust = analyze(environment);

        if (exhaust == null) {
            return null;
        }

        exhaust.throwIfError();

        return generate(environment, exhaust, environment.getSourceFiles(), null, null);
    }

    @Nullable
    private static AnalyzeExhaust analyze(@NotNull final JetCoreEnvironment environment) {
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(
                environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY));
        analyzerWithCompilerReport.analyzeAndReport(
                environment.getSourceFiles(), new Function0<AnalyzeExhaust>() {
                    @NotNull
                    @Override
                    public AnalyzeExhaust invoke() {
                        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject());
                        BindingTrace sharedTrace = support.getTrace();
                        ModuleDescriptorImpl sharedModule = support.getModule();

                        IncrementalCacheProvider incrementalCacheProvider = IncrementalCacheProvider.OBJECT$.getInstance();
                        File incrementalCacheBaseDir = environment.getConfiguration().get(JVMConfigurationKeys.INCREMENTAL_CACHE_BASE_DIR);
                        final IncrementalCache incrementalCache;
                        if (incrementalCacheProvider != null && incrementalCacheBaseDir != null) {
                            incrementalCache = incrementalCacheProvider.getIncrementalCache(incrementalCacheBaseDir);
                            Disposer.register(environment.getApplication(), new Disposable() {
                                @Override
                                public void dispose() {
                                    incrementalCache.close();
                                }
                            });
                        }
                        else {
                            incrementalCache = null;
                        }


                        return AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                                environment.getProject(),
                                environment.getSourceFiles(),
                                sharedTrace,
                                Predicates.<PsiFile>alwaysTrue(),
                                sharedModule,
                                environment.getConfiguration().get(JVMConfigurationKeys.MODULE_IDS),
                                incrementalCache
                        );
                    }
                }
        );

        AnalyzeExhaust exhaust = analyzerWithCompilerReport.getAnalyzeExhaust();
        assert exhaust != null : "AnalyzeExhaust should be non-null, compiling: " + environment.getSourceFiles();

        CompilerPluginContext context = new CompilerPluginContext(environment.getProject(), exhaust.getBindingContext(),
                                                                  environment.getSourceFiles());
        for (CompilerPlugin plugin : environment.getConfiguration().getList(CLIConfigurationKeys.COMPILER_PLUGINS)) {
            plugin.processFiles(context);
        }

        return analyzerWithCompilerReport.hasErrors() ? null : exhaust;
    }

    @NotNull
    private static GenerationState generate(
            @NotNull JetCoreEnvironment environment,
            @NotNull AnalyzeExhaust exhaust,
            @NotNull List<JetFile> sourceFiles,
            @Nullable String moduleId,
            File outputDirectory
    ) {
        CompilerConfiguration configuration = environment.getConfiguration();
        File incrementalCacheDir = configuration.get(JVMConfigurationKeys.INCREMENTAL_CACHE_BASE_DIR);
        IncrementalCacheProvider incrementalCacheProvider = IncrementalCacheProvider.OBJECT$.getInstance();

        Collection<FqName> packagesWithRemovedFiles;
        if (incrementalCacheDir == null || moduleId == null || incrementalCacheProvider == null) {
            packagesWithRemovedFiles = null;
        }
        else {
            IncrementalCache incrementalCache = incrementalCacheProvider.getIncrementalCache(incrementalCacheDir);
            try {
                packagesWithRemovedFiles = IncrementalPackage.getPackagesWithRemovedFiles(
                        incrementalCache, moduleId, environment.getSourceFiles());
            }
            finally {
                incrementalCache.close();
            }
        }
        BindingTraceContext diagnosticHolder = new BindingTraceContext();
        GenerationState generationState = new GenerationState(
                environment.getProject(),
                ClassBuilderFactories.BINARIES,
                Progress.DEAF,
                exhaust.getModuleDescriptor(),
                exhaust.getBindingContext(),
                sourceFiles,
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, false),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, false),
                GenerationState.GenerateClassFilter.GENERATE_ALL,
                configuration.get(JVMConfigurationKeys.ENABLE_INLINE, InlineCodegenUtil.DEFAULT_INLINE_FLAG),
                configuration.get(JVMConfigurationKeys.ENABLE_OPTIMIZATION, OptimizationUtils.DEFAULT_OPTIMIZATION_FLAG),
                packagesWithRemovedFiles,
                moduleId,
                diagnosticHolder,
                outputDirectory);
        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION);
        AnalyzerWithCompilerReport.reportDiagnostics(
                new FilteredJvmDiagnostics(
                        diagnosticHolder.getBindingContext().getDiagnostics(),
                        exhaust.getBindingContext().getDiagnostics()
                ),
                environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        );
        return generationState;
    }
}
