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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.LocalTimeCounter;
import jet.Function0;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.CompilerPluginContext;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.JetMainDetector;
import org.jetbrains.jet.utils.Progress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

/**
 * @author yole
 * @author abreslav
 */
public class KotlinToJVMBytecodeCompiler {

    private KotlinToJVMBytecodeCompiler() {
    }

    @Nullable
    public static ClassFileFactory compileModule(
            CompileEnvironmentConfiguration configuration,
            Module moduleBuilder,
            File directory
    ) {
        if (moduleBuilder.getSourceFiles().isEmpty()) {
            throw new CompileEnvironmentException("No source files where defined");
        }

        for (String sourceFile : moduleBuilder.getSourceFiles()) {
            File source = new File(sourceFile);
            if (!source.isAbsolute()) {
                source = new File(directory, sourceFile);
            }

            if (!source.exists()) {
                throw new CompileEnvironmentException("'" + source + "' does not exist");
            }

            configuration.getEnvironment().addSources(source.getPath());
        }
        for (String classpathRoot : moduleBuilder.getClasspathRoots()) {
            configuration.getEnvironment().addToClasspath(new File(classpathRoot));
        }

        CompileEnvironmentUtil.ensureRuntime(configuration.getEnvironment(), configuration.getEnvironment().getCompilerDependencies());

        GenerationState generationState = analyzeAndGenerate(configuration);
        if (generationState == null) {
            return null;
        }
        return generationState.getFactory();
    }

    public static boolean compileModules(
            CompileEnvironmentConfiguration configuration,

            @NotNull List<Module> modules,

            @NotNull File directory,
            @Nullable String jarPath,
            @Nullable String outputDir,
            boolean jarRuntime) {

        for (Module moduleBuilder : modules) {
            // TODO: this should be done only once for the environment
            if (configuration.getEnvironment().getCompilerDependencies().getRuntimeJar() != null) {
                CompileEnvironmentUtil
                        .addToClasspath(configuration.getEnvironment(), configuration.getEnvironment().getCompilerDependencies().getRuntimeJar());
            }
            ClassFileFactory moduleFactory = compileModule(configuration, moduleBuilder, directory);
            if (moduleFactory == null) {
                return false;
            }
            if (outputDir != null) {
                CompileEnvironmentUtil.writeToOutputDirectory(moduleFactory, outputDir);
            }
            else {
                String path = jarPath != null ? jarPath : new File(directory, moduleBuilder.getModuleName() + ".jar").getPath();
                try {
                    CompileEnvironmentUtil.writeToJar(moduleFactory, new FileOutputStream(path), null, jarRuntime);
                }
                catch (FileNotFoundException e) {
                    throw new CompileEnvironmentException("Invalid jar path " + path, e);
                }
            }
        }
        return true;
    }

    private static boolean compileBunchOfSources(
            CompileEnvironmentConfiguration configuration,
            String jar,
            String outputDir,
            boolean includeRuntime
    ) {
        FqName mainClass = null;
        for (JetFile file : configuration.getEnvironment().getSourceFiles()) {
            if (JetMainDetector.hasMain(file.getDeclarations())) {
                FqName fqName = JetPsiUtil.getFQName(file);
                mainClass = fqName.child(JvmAbi.PACKAGE_CLASS);
                break;
            }
        }

        CompileEnvironmentUtil.ensureRuntime(configuration.getEnvironment(), configuration.getEnvironment().getCompilerDependencies());

        GenerationState generationState = analyzeAndGenerate(configuration);
        if (generationState == null) {
            return false;
        }

        try {
            ClassFileFactory factory = generationState.getFactory();
            if (jar != null) {
                try {
                    CompileEnvironmentUtil.writeToJar(factory, new FileOutputStream(jar), mainClass, includeRuntime);
                }
                catch (FileNotFoundException e) {
                    throw new CompileEnvironmentException("Invalid jar path " + jar, e);
                }
            }
            else if (outputDir != null) {
                CompileEnvironmentUtil.writeToOutputDirectory(factory, outputDir);
            }
            else {
                throw new CompileEnvironmentException("Output directory or jar file is not specified - no files will be saved to the disk");
            }
            return true;
        }
        finally {
            generationState.destroy();
        }
    }

    public static boolean compileBunchOfSources(
            CompileEnvironmentConfiguration configuration,

            String sourceFileOrDir, String jar, String outputDir, boolean includeRuntime) {
        configuration.getEnvironment().addSources(sourceFileOrDir);

        return compileBunchOfSources(configuration, jar, outputDir, includeRuntime);
    }

    public static boolean compileBunchOfSourceDirectories(
            CompileEnvironmentConfiguration configuration,

            List<String> sources, String jar, String outputDir, boolean includeRuntime) {
        for (String source : sources) {
            configuration.getEnvironment().addSources(source);
        }

        return compileBunchOfSources(configuration, jar, outputDir, includeRuntime);
    }

    @Nullable
    public static ClassLoader compileText(
            CompileEnvironmentConfiguration configuration,
            String code) {
        configuration.getEnvironment()
                .addSources(new LightVirtualFile("script" + LocalTimeCounter.currentTime() + ".kt", JetLanguage.INSTANCE, code));

        GenerationState generationState = analyzeAndGenerate(configuration);
        if (generationState == null) {
            return null;
        }
        return new GeneratedClassLoader(generationState.getFactory());
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(CompileEnvironmentConfiguration configuration) {
        return analyzeAndGenerate(configuration, configuration.getEnvironment().getCompilerDependencies().getCompilerSpecialMode().isStubs());
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(
            CompileEnvironmentConfiguration configuration,
            boolean stubs
    ) {
        AnalyzeExhaust exhaust = analyze(configuration, stubs);

        if (exhaust == null) {
            return null;
        }

        exhaust.throwIfError();

        return generate(configuration, exhaust, stubs);
    }

    @Nullable
    private static AnalyzeExhaust analyze(
            final CompileEnvironmentConfiguration configuration,
            boolean stubs) {
        final JetCoreEnvironment environment = configuration.getEnvironment();
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(configuration.getMessageCollector());
        final Predicate<PsiFile> filesToAnalyzeCompletely =
                stubs ? Predicates.<PsiFile>alwaysFalse() : Predicates.<PsiFile>alwaysTrue();
        analyzerWithCompilerReport.analyzeAndReport(
                new Function0<AnalyzeExhaust>() {
                    @NotNull
                    @Override
                    public AnalyzeExhaust invoke() {
                        return AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                                environment.getProject(), environment.getSourceFiles(), filesToAnalyzeCompletely,
                                JetControlFlowDataTraceFactory.EMPTY,
                                configuration.getEnvironment().getCompilerDependencies());
                    }
                }, environment.getSourceFiles()
        );

        return analyzerWithCompilerReport.hasErrors() ? null : analyzerWithCompilerReport.getAnalyzeExhaust();
    }

    @NotNull
    private static GenerationState generate(
            final CompileEnvironmentConfiguration configuration,
            AnalyzeExhaust exhaust,
            boolean stubs) {
        JetCoreEnvironment environment = configuration.getEnvironment();
        Project project = environment.getProject();
        Progress backendProgress = new Progress() {
            @Override
            public void log(String message) {
                configuration.getMessageCollector().report(CompilerMessageSeverity.LOGGING, message, CompilerMessageLocation.NO_LOCATION);
            }
        };
        GenerationState generationState = new GenerationState(project, ClassBuilderFactories.binaries(stubs), backendProgress,
                                                              exhaust, environment.getSourceFiles(),
                                                              configuration.getEnvironment().getCompilerDependencies().getCompilerSpecialMode());
        generationState.compileCorrectFiles(CompilationErrorHandler.THROW_EXCEPTION);

        List<CompilerPlugin> plugins = configuration.getCompilerPlugins();
        if (plugins != null) {
            CompilerPluginContext context = new CompilerPluginContext(project, exhaust.getBindingContext(), environment.getSourceFiles());
            for (CompilerPlugin plugin : plugins) {
                plugin.processFiles(context);
            }
        }
        return generationState;
    }
}
