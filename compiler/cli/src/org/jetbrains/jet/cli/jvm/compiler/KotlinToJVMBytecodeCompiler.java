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
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.CompilerPluginContext;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.util.CompilerPathUtil;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetMainDetector;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.jet.utils.Progress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
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
            K2JVMCompileEnvironmentConfiguration configuration,
            Module moduleBuilder,
            File directory
    ) {
        if (moduleBuilder.getSourceFiles().isEmpty()) {
            throw new CompileEnvironmentException("No source files where defined");
        }

        // TODO creating environment copy each time - not good. original environment shouldn't be passed at all
        CompilerConfiguration compilerConfiguration = configuration.getEnvironment().getConfiguration().copy();
        for (String sourceFile : moduleBuilder.getSourceFiles()) {
            File source = new File(sourceFile);
            if (!source.isAbsolute()) {
                source = new File(directory, sourceFile);
            }

            if (!source.exists()) {
                throw new CompileEnvironmentException("'" + source + "' does not exist");
            }

            compilerConfiguration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, source.getPath());
        }

        for (String classpathRoot : moduleBuilder.getClasspathRoots()) {
            compilerConfiguration.add(JVMConfigurationKeys.CLASSPATH_KEY, new File(classpathRoot));
        }

        for (String annotationsRoot : moduleBuilder.getAnnotationsRoots()) {
            compilerConfiguration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, new File(annotationsRoot));
        }

        Disposable parentDisposable = new Disposable() {
            @Override
            public void dispose() {
            }
        };
        JetCoreEnvironment environment = null;
        try {
            environment = JetCoreEnvironment.createCoreEnvironmentForJVM(parentDisposable,
                                                                         compilerConfiguration);


            K2JVMCompileEnvironmentConfiguration currentK2JVMConfiguration = new K2JVMCompileEnvironmentConfiguration(
                    environment, configuration.getMessageCollector(), configuration.isScript(),
                    configuration.getBuiltinsScopeExtensionMode(), configuration.isStubs(),
                    configuration.getBuiltinToJavaTypesMapping());
            GenerationState generationState = analyzeAndGenerate(currentK2JVMConfiguration);
            if (generationState == null) {
                return null;
            }
            return generationState.getFactory();
        } finally {
            if (environment != null) {
                Disposer.dispose(parentDisposable);
            }
        }
    }

    public static boolean compileModules(
            K2JVMCompileEnvironmentConfiguration configuration,
            @NotNull List<Module> modules,
            @NotNull File directory,
            @Nullable File jarPath,
            @Nullable File outputDir,
            boolean jarRuntime) {

        for (Module moduleBuilder : modules) {
            ClassFileFactory moduleFactory = compileModule(configuration, moduleBuilder, directory);
            if (moduleFactory == null) {
                return false;
            }
            if (outputDir != null) {
                CompileEnvironmentUtil.writeToOutputDirectory(moduleFactory, outputDir);
            }
            else {
                File path = jarPath != null ? jarPath : new File(directory, moduleBuilder.getModuleName() + ".jar");
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(path);
                    CompileEnvironmentUtil.writeToJar(moduleFactory, outputStream, null, jarRuntime);
                    outputStream.close();
                }
                catch (FileNotFoundException e) {
                    throw new CompileEnvironmentException("Invalid jar path " + path, e);
                }
                catch (IOException e) {
                    throw ExceptionUtils.rethrow(e);
                }
                finally {
                    ExceptionUtils.closeQuietly(outputStream);
                }
            }
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
                mainClass = fqName.child(Name.identifier(JvmAbi.PACKAGE_CLASS));
            }
        }
        return mainClass;
    }

    public static boolean compileBunchOfSources(
            K2JVMCompileEnvironmentConfiguration configuration,
            @Nullable File jar,
            @Nullable File outputDir,
            boolean includeRuntime
    ) {

        FqName mainClass = findMainClass(configuration.getEnvironment().getSourceFiles());

        GenerationState generationState = analyzeAndGenerate(configuration);
        if (generationState == null) {
            return false;
        }

        try {
            ClassFileFactory factory = generationState.getFactory();
            if (jar != null) {
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(jar);
                    CompileEnvironmentUtil.writeToJar(factory, new FileOutputStream(jar), mainClass, includeRuntime);
                    os.close();
                }
                catch (FileNotFoundException e) {
                    throw new CompileEnvironmentException("Invalid jar path " + jar, e);
                }
                catch (IOException e) {
                    throw ExceptionUtils.rethrow(e);
                }
                finally {
                    ExceptionUtils.closeQuietly(os);
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

    public static boolean compileAndExecuteScript(
            @NotNull K2JVMCompileEnvironmentConfiguration configuration,
            @NotNull List<String> scriptArgs) {

        GenerationState generationState = analyzeAndGenerate(configuration);
        if (generationState == null) {
            return false;
        }

        try {
            ClassFileFactory factory = generationState.getFactory();
            try {
                GeneratedClassLoader classLoader = new GeneratedClassLoader(factory, new URLClassLoader(new URL[]{
                        // TODO: add all classpath
                        CompilerPathUtil.getRuntimePath().toURI().toURL()
                },
                        AllModules.class.getClassLoader()));
                Class<?> scriptClass = classLoader.loadClass(ScriptCodegen.SCRIPT_DEFAULT_CLASS_NAME.getFqName().getFqName());
                scriptClass.getConstructor(String[].class).newInstance(new Object[]{scriptArgs.toArray(new String[0])});
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to evaluate script: " + e, e);
            }
            return true;
        }
        finally {
            generationState.destroy();
        }
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(K2JVMCompileEnvironmentConfiguration configuration) {
        return analyzeAndGenerate(configuration, configuration.isStubs());
    }

    @Nullable
    public static GenerationState analyzeAndGenerate(
            K2JVMCompileEnvironmentConfiguration configuration,
            boolean stubs
    ) {
        AnalyzeExhaust exhaust = analyze(configuration, configuration.isScript(), stubs);

        if (exhaust == null) {
            return null;
        }

        exhaust.throwIfError();

        return generate(configuration, exhaust, stubs);
    }

    @Nullable
    private static AnalyzeExhaust analyze(
            final K2JVMCompileEnvironmentConfiguration configuration,
            boolean script, boolean stubs) {
        final JetCoreEnvironment environment = configuration.getEnvironment();
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(configuration.getMessageCollector());
        final Predicate<PsiFile> filesToAnalyzeCompletely =
                stubs ? Predicates.<PsiFile>alwaysFalse() : Predicates.<PsiFile>alwaysTrue();
        final List<AnalyzerScriptParameter> scriptParameters =
                script ? CommandLineScriptUtils.scriptParameters() : Collections.<AnalyzerScriptParameter>emptyList();
        analyzerWithCompilerReport.analyzeAndReport(
                new Function0<AnalyzeExhaust>() {
                    @NotNull
                    @Override
                    public AnalyzeExhaust invoke() {
                        return AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                                environment.getProject(),
                                environment.getSourceFiles(),
                                scriptParameters,
                                filesToAnalyzeCompletely,
                                configuration.getBuiltinsScopeExtensionMode());
                    }
                }, environment.getSourceFiles()
        );

        return analyzerWithCompilerReport.hasErrors() ? null : analyzerWithCompilerReport.getAnalyzeExhaust();
    }

    @NotNull
    private static GenerationState generate(
            final K2JVMCompileEnvironmentConfiguration configuration,
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
                                                              configuration.getBuiltinToJavaTypesMapping());
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
