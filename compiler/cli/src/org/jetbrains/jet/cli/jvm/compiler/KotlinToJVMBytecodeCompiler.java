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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import jet.Function0;
import jet.modules.AllModules;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.CompilerPluginContext;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.output.OutputDirector;
import org.jetbrains.jet.cli.common.output.SingleDirectoryDirector;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.plugin.JetMainDetector;
import org.jetbrains.jet.utils.KotlinPaths;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class KotlinToJVMBytecodeCompiler {

    private static final boolean COMPILE_CHUNK_AS_ONE_MODULE = true;

    private KotlinToJVMBytecodeCompiler() {
    }

    @Nullable
    public static ClassFileFactory compileModule(CompilerConfiguration configuration, Module module, File directory) {
        List<String> sourceFiles = module.getSourceFiles();
        if (sourceFiles.isEmpty()) {
            throw new CompileEnvironmentException("No source files where defined in module " + module.getModuleName());
        }

        CompilerConfiguration compilerConfiguration = configuration.copy();
        for (String sourceFile : sourceFiles) {
            File source = new File(sourceFile);
            if (!source.isAbsolute()) {
                source = new File(directory, sourceFile);
            }

            if (!source.exists()) {
                throw new CompileEnvironmentException("'" + source + "' does not exist in module " + module.getModuleName());
            }

            compilerConfiguration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, source.getPath());
        }

        for (String classpathRoot : module.getClasspathRoots()) {
            compilerConfiguration.add(JVMConfigurationKeys.CLASSPATH_KEY, new File(classpathRoot));
        }

        for (String annotationsRoot : module.getAnnotationsRoots()) {
            compilerConfiguration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, new File(annotationsRoot));
        }

        Disposable parentDisposable = Disposer.newDisposable();
        JetCoreEnvironment moduleEnvironment = null;
        try {
            moduleEnvironment = JetCoreEnvironment.createForProduction(parentDisposable, compilerConfiguration);


            GenerationState generationState = analyzeAndGenerate(moduleEnvironment);
            if (generationState == null) {
                return null;
            }
            return generationState.getFactory();
        } finally {
            if (moduleEnvironment != null) {
                Disposer.dispose(parentDisposable);
            }
        }
    }

    private static void writeOutput(
            CompilerConfiguration configuration,
            ClassFileFactory outputFiles,
            OutputDirector outputDir,
            File jarPath,
            boolean jarRuntime,
            FqName mainClass
    ) {
        MessageCollector messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE);
        CompileEnvironmentUtil.writeOutputToDirOrJar(jarPath, outputDir, jarRuntime, mainClass, outputFiles, messageCollector);
    }

    public static boolean compileModules(
            CompilerConfiguration configuration,
            @NotNull final ModuleChunk chunk,
            @NotNull File directory,
            @Nullable File jarPath,
            boolean jarRuntime
    ) {
        List<Module> modules = chunk.getModules();
        if (COMPILE_CHUNK_AS_ONE_MODULE && modules.size() > 1) {
            modules = Collections.<Module>singletonList(new ChunkAsOneModule(chunk));
        }
        for (Module module : modules) {
            ClassFileFactory outputFiles = compileModule(configuration, module, directory);
            if (outputFiles == null) {
                return false;
            }
            OutputDirector outputDir = new OutputDirector() {
                @NotNull
                @Override
                public File getOutputDirectory(@NotNull Collection<? extends File> sourceFiles) {
                    for (File sourceFile : sourceFiles) {
                        // Note that here we track original modules:
                        Module module = chunk.findModuleBySourceFile(sourceFile);
                        if (module != null) {
                            return new File(module.getOutputDirectory());
                        }
                    }
                    throw new IllegalStateException("No module found for source files: " + sourceFiles);
                }
            };

            writeOutput(configuration, outputFiles, outputDir, jarPath, jarRuntime, null);
        }
        return true;
    }

    @Nullable
    private static FqName findMainClass(@NotNull List<JetFile> files) {
        FqName mainClass = null;
        for (JetFile file : files) {
            if (JetMainDetector.hasMain(file.getDeclarations())) {
                if (mainClass != null) {
                    // more than one main
                    return null;
                }
                FqName fqName = JetPsiUtil.getFQName(file);
                mainClass = PackageClassUtils.getPackageClassFqName(fqName);
            }
        }
        return mainClass;
    }

    public static boolean compileBunchOfSources(
            JetCoreEnvironment environment,
            @Nullable File jar,
            @Nullable File outputDir,
            boolean includeRuntime
    ) {

        FqName mainClass = findMainClass(environment.getSourceFiles());

        GenerationState generationState = analyzeAndGenerate(environment);
        if (generationState == null) {
            return false;
        }

        try {
            OutputDirector outputDirector = outputDir != null ? new SingleDirectoryDirector(outputDir) : null;
            writeOutput(environment.getConfiguration(), generationState.getFactory(), outputDirector, jar, includeRuntime, mainClass);
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
            scriptClass.getConstructor(String[].class).newInstance(new Object[]{scriptArgs.toArray(new String[scriptArgs.size()])});
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
        GenerationState state = analyzeAndGenerate(environment);
        if (state == null) {
            return null;
        }

        GeneratedClassLoader classLoader = null;
        try {
            classLoader = new GeneratedClassLoader(state.getFactory(),
                                                   new URLClassLoader(new URL[] {
                                                           // TODO: add all classpath
                                                           paths.getRuntimePath().toURI().toURL()
                                                   }, AllModules.class.getClassLoader())
            );

            return classLoader.loadClass(ScriptNameUtil.classNameForScript(environment.getSourceFiles().get(0)));
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to evaluate script: " + e, e);
        }
        finally {
            if (classLoader != null) {
                classLoader.dispose();
            }
            state.destroy();
        }
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(@NotNull JetCoreEnvironment environment) {
        AnalyzeExhaust exhaust = analyze(environment);

        if (exhaust == null) {
            return null;
        }

        exhaust.throwIfError();

        return generate(environment, exhaust);
    }

    @Nullable
    private static AnalyzeExhaust analyze(@NotNull final JetCoreEnvironment environment) {
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(
                environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY));
        analyzerWithCompilerReport.analyzeAndReport(
                new Function0<AnalyzeExhaust>() {
                    @NotNull
                    @Override
                    public AnalyzeExhaust invoke() {
                        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject());
                        BindingTrace sharedTrace = support.getTrace();
                        ModuleDescriptorImpl sharedModule = support.getModule();
                        return AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                                environment.getProject(),
                                environment.getSourceFiles(),
                                sharedTrace,
                                environment.getConfiguration().getList(JVMConfigurationKeys.SCRIPT_PARAMETERS),
                                Predicates.<PsiFile>alwaysTrue(),
                                false,
                                sharedModule
                        );
                    }
                }, environment.getSourceFiles()
        );

        return analyzerWithCompilerReport.hasErrors() ? null : analyzerWithCompilerReport.getAnalyzeExhaust();
    }

    @NotNull
    private static GenerationState generate(@NotNull JetCoreEnvironment environment, @NotNull AnalyzeExhaust exhaust) {
        Project project = environment.getProject();
        CompilerConfiguration configuration = environment.getConfiguration();
        GenerationState generationState = new GenerationState(
                project, ClassBuilderFactories.BINARIES, Progress.DEAF, exhaust.getBindingContext(), environment.getSourceFiles(),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, false),
                configuration.get(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, false),
                /*generateDeclaredClasses = */true,
                configuration.get(JVMConfigurationKeys.ENABLE_INLINE, InlineUtil.DEFAULT_INLINE_FLAG)
        );
        KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION);

        CompilerPluginContext context = new CompilerPluginContext(project, exhaust.getBindingContext(), environment.getSourceFiles());
        for (CompilerPlugin plugin : configuration.getList(CLIConfigurationKeys.COMPILER_PLUGINS)) {
            plugin.processFiles(context);
        }
        return generationState;
    }
}
