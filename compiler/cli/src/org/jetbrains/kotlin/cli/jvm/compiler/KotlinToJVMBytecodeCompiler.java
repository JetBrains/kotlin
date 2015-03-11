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

package org.jetbrains.kotlin.cli.jvm.compiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ArrayUtil;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;
import kotlin.modules.AllModules;
import kotlin.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.CompilerPlugin;
import org.jetbrains.kotlin.cli.common.CompilerPluginContext;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.Progress;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCache;
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCacheProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.parsing.JetScriptDefinition;
import org.jetbrains.kotlin.parsing.JetScriptDefinitionProvider;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AnalyzerScriptParameter;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.ScriptNameUtil;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.utils.KotlinPaths;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

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
        if (jarPath != null) {
            CompileEnvironmentUtil.writeToJar(jarPath, jarRuntime, mainClass, outputFiles);
        }
        else {
            MessageCollector messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE);
            OutputUtilsPackage.writeAll(outputFiles, outputDir == null ? new File(".") : outputDir, messageCollector);
        }
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
            environment = JetCoreEnvironment
                    .createForProduction(parentDisposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

            AnalysisResult result = analyze(environment);
            if (result == null) {
                return false;
            }

            result.throwIfError();

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
                        generate(environment, result, jetFiles, module.getModuleName(), new File(module.getOutputDirectory()));
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
            @NotNull CompilerConfiguration configuration,
            @NotNull KotlinPaths paths,
            @NotNull JetCoreEnvironment environment,
            @NotNull List<String> scriptArgs
    ) {
        Class<?> scriptClass = compileScript(configuration, paths, environment);
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
    public static Class<?> compileScript(
            @NotNull CompilerConfiguration configuration,
            @NotNull KotlinPaths paths,
            @NotNull JetCoreEnvironment environment
    ) {
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
            List<URL> classPaths = Lists.newArrayList(paths.getRuntimePath().toURI().toURL());
            for (File file : configuration.get(JVMConfigurationKeys.CLASSPATH_KEY, Collections.<File>emptyList())) {
                classPaths.add(file.toURI().toURL());
            }
            classLoader = new GeneratedClassLoader(state.getFactory(),
                                                   new URLClassLoader(classPaths.toArray(new URL[classPaths.size()]),
                                                                      AllModules.class.getClassLoader())
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
        AnalysisResult result = analyze(environment);

        if (result == null) {
            return null;
        }

        result.throwIfError();

        return generate(environment, result, environment.getSourceFiles(), null, null);
    }

    @Nullable
    private static AnalysisResult analyze(@NotNull final JetCoreEnvironment environment) {
        MessageCollector collector = environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);
        assert collector != null;

        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(collector);
        analyzerWithCompilerReport.analyzeAndReport(
                environment.getSourceFiles(), new Function0<AnalysisResult>() {
                    @NotNull
                    @Override
                    public AnalysisResult invoke() {
                        BindingTrace sharedTrace = new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace();
                        ModuleDescriptorImpl analyzeModule = TopDownAnalyzerFacadeForJVM.createSealedJavaModule();

                        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                                environment.getProject(),
                                ContextPackage.GlobalContext(),
                                environment.getSourceFiles(),
                                sharedTrace,
                                analyzeModule,
                                environment.getConfiguration().get(JVMConfigurationKeys.MODULE_IDS),
                                environment.getConfiguration().get(JVMConfigurationKeys.INCREMENTAL_CACHE_PROVIDER)
                        );
                    }
                }
        );

        AnalysisResult result = analyzerWithCompilerReport.getAnalysisResult();
        assert result != null : "AnalysisResult should be non-null, compiling: " + environment.getSourceFiles();

        CompilerPluginContext context = new CompilerPluginContext(environment.getProject(), result.getBindingContext(),
                                                                  environment.getSourceFiles());
        for (CompilerPlugin plugin : environment.getConfiguration().getList(CLIConfigurationKeys.COMPILER_PLUGINS)) {
            plugin.processFiles(context);
        }

        return analyzerWithCompilerReport.hasErrors() ? null : result;
    }

    @NotNull
    private static GenerationState generate(
            @NotNull JetCoreEnvironment environment,
            @NotNull AnalysisResult result,
            @NotNull List<JetFile> sourceFiles,
            @Nullable String moduleId,
            File outputDirectory
    ) {
        CompilerConfiguration configuration = environment.getConfiguration();
        IncrementalCacheProvider incrementalCacheProvider = configuration.get(JVMConfigurationKeys.INCREMENTAL_CACHE_PROVIDER);

        Collection<FqName> packagesWithObsoleteParts;
        if (moduleId == null || incrementalCacheProvider == null) {
            packagesWithObsoleteParts = null;
        }
        else {
            IncrementalCache incrementalCache = incrementalCacheProvider.getIncrementalCache(moduleId);
            packagesWithObsoleteParts = new HashSet<FqName>();
            for (String internalName : incrementalCache.getObsoletePackageParts()) {
                packagesWithObsoleteParts.add(JvmClassName.byInternalName(internalName).getPackageFqName());
            }
        }
        BindingTraceContext diagnosticHolder = new BindingTraceContext();
        GenerationState generationState = new GenerationState(
                environment.getProject(),
                ClassBuilderFactories.BINARIES,
                Progress.DEAF,
                result.getModuleDescriptor(),
                result.getBindingContext(),
                sourceFiles,
                configuration.get(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, false),
                configuration.get(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, false),
                GenerationState.GenerateClassFilter.GENERATE_ALL,
                configuration.get(JVMConfigurationKeys.DISABLE_INLINE, false),
                configuration.get(JVMConfigurationKeys.DISABLE_OPTIMIZATION, false),
                packagesWithObsoleteParts,
                moduleId,
                diagnosticHolder,
                outputDirectory
        );
        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION);
        AnalyzerWithCompilerReport.reportDiagnostics(
                new FilteredJvmDiagnostics(
                        diagnosticHolder.getBindingContext().getDiagnostics(),
                        result.getBindingContext().getDiagnostics()
                ),
                environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        );
        return generationState;
    }
}
