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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.util.ArrayUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.CompilerPlugin;
import org.jetbrains.kotlin.cli.common.CompilerPluginContext;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.modules.Module;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.Progress;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents;
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
import org.jetbrains.kotlin.resolve.lazy.JvmPackageMappingProvider;
import org.jetbrains.kotlin.util.PerformanceCounter;
import org.jetbrains.kotlin.utils.KotlinPaths;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.jetbrains.kotlin.cli.jvm.config.ConfigPackage.*;
import static org.jetbrains.kotlin.config.ConfigPackage.addKotlinSourceRoots;

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
            @NotNull KotlinCoreEnvironment environment,
            @NotNull CompilerConfiguration configuration,
            @NotNull List<Module> chunk,
            @NotNull File directory,
            @Nullable File jarPath,
            boolean jarRuntime
    ) {
        Map<Module, ClassFileFactory> outputFiles = Maps.newHashMap();

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        String targetDescription = "in modules [" + Joiner.on(", ").join(Collections2.transform(chunk, new Function<Module, String>() {
            @Override
            public String apply(@Nullable Module input) {
                return input != null ? input.getModuleName() : "<null>";
            }
        })) + "] ";
        AnalysisResult result = analyze(environment, targetDescription);
        if (result == null) {
            return false;
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        result.throwIfError();

        for (Module module : chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
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

        for (Module module : chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
            writeOutput(configuration, outputFiles.get(module), new File(module.getOutputDirectory()), jarPath, jarRuntime, null);
        }
        return true;
    }

    @NotNull
    public static CompilerConfiguration createCompilerConfiguration(
            @NotNull CompilerConfiguration base,
            @NotNull List<Module> chunk,
            @NotNull File directory
    ) {
        CompilerConfiguration configuration = base.copy();

        for (Module module : chunk) {
            addKotlinSourceRoots(configuration, getAbsolutePaths(directory, module));
        }

        for (Module module : chunk) {
            for (String javaSourceRoot : module.getJavaSourceRoots()) {
                addJavaSourceRoot(configuration, new File(javaSourceRoot));
            }
        }

        for (Module module : chunk) {
            for (String classpathRoot : module.getClasspathRoots()) {
                addJvmClasspathRoot(configuration, new File(classpathRoot));
            }
        }

        for (Module module : chunk) {
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
            @NotNull KotlinCoreEnvironment environment,
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
            @NotNull KotlinCoreEnvironment environment,
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
            @NotNull KotlinCoreEnvironment environment
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
            for (File file : getJvmClasspathRoots(configuration)) {
                classPaths.add(file.toURI().toURL());
            }
            //noinspection UnnecessaryFullyQualifiedName
            classLoader = new GeneratedClassLoader(state.getFactory(),
                                                   new URLClassLoader(classPaths.toArray(new URL[classPaths.size()]), null)
            );

            FqName nameForScript = ScriptNameUtil.classNameForScript(environment.getSourceFiles().get(0).getScript());
            return classLoader.loadClass(nameForScript.asString());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to evaluate script: " + e, e);
        }
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(@NotNull KotlinCoreEnvironment environment) {
        AnalysisResult result = analyze(environment, null);

        if (result == null) {
            return null;
        }

        if (!result.getShouldGenerateCode()) return null;

        result.throwIfError();

        return generate(environment, result, environment.getSourceFiles(), null, null);
    }

    @Nullable
    private static AnalysisResult analyze(@NotNull final KotlinCoreEnvironment environment, @Nullable String targetDescription) {
        MessageCollector collector = environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);
        assert collector != null;

        long analysisStart = PerformanceCounter.Companion.currentTime();
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(collector);
        analyzerWithCompilerReport.analyzeAndReport(
                environment.getSourceFiles(), new Function0<AnalysisResult>() {
                    @NotNull
                    @Override
                    public AnalysisResult invoke() {
                        BindingTrace sharedTrace = new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace();
                        ModuleContext moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.getProject(),
                                                                                                                getModuleName(environment));

                        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                                moduleContext,
                                environment.getSourceFiles(),
                                sharedTrace,
                                environment.getConfiguration().get(JVMConfigurationKeys.MODULE_IDS),
                                environment.getConfiguration().get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
                                new JvmPackageMappingProvider(environment)
                        );
                    }
                }
        );
        long analysisNanos = PerformanceCounter.Companion.currentTime() - analysisStart;
        String message = "ANALYZE: " + environment.getSourceFiles().size() + " files (" +
                         environment.getSourceLinesOfCode() + " lines) " +
                         (targetDescription != null ? targetDescription : "") +
                         "in " + TimeUnit.NANOSECONDS.toMillis(analysisNanos) + " ms";
        K2JVMCompiler.Companion.reportPerf(environment.getConfiguration(), message);

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
            @NotNull KotlinCoreEnvironment environment,
            @NotNull AnalysisResult result,
            @NotNull List<JetFile> sourceFiles,
            @Nullable String moduleId,
            File outputDirectory
    ) {
        CompilerConfiguration configuration = environment.getConfiguration();
        IncrementalCompilationComponents incrementalCompilationComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS);

        Collection<FqName> packagesWithObsoleteParts;
        if (moduleId == null || incrementalCompilationComponents == null) {
            packagesWithObsoleteParts = null;
        }
        else {
            IncrementalCache incrementalCache = incrementalCompilationComponents.getIncrementalCache(moduleId);
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
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        long generationStart = PerformanceCounter.Companion.currentTime();

        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION);

        long generationNanos = PerformanceCounter.Companion.currentTime() - generationStart;
        String desc = moduleId != null ? "module " + moduleId + " " : "";
        String message = "GENERATE: " + sourceFiles.size() + " files (" +
                         environment.countLinesOfCode(sourceFiles) + " lines) " + desc + "in " + TimeUnit.NANOSECONDS.toMillis(generationNanos) + " ms";
        K2JVMCompiler.Companion.reportPerf(environment.getConfiguration(), message);
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        AnalyzerWithCompilerReport.reportDiagnostics(
                new FilteredJvmDiagnostics(
                        diagnosticHolder.getBindingContext().getDiagnostics(),
                        result.getBindingContext().getDiagnostics()
                ),
                environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        );
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        return generationState;
    }
}
