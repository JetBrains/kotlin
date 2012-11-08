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

package org.jetbrains.jet.plugin.compiler;

import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.Function;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.compiler.runner.KotlinModuleScriptGenerator;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.project.JsModuleDetector;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

/**
 * @author yole
 */
public class JetCompiler implements TranslatingCompiler {

    private static final boolean RUN_OUT_OF_PROCESS = false;

    @Override
    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext) {
        if (!(virtualFile.getFileType() instanceof JetFileType)) {
            return false;
        }
        Module module = compileContext.getModuleByFile(virtualFile);
        if (module != null && JsModuleDetector.isJsModule(module)) {
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Jet Language Compiler";
    }

    @Override
    public boolean validateConfiguration(CompileScope compileScope) {
        return true;
    }

    @Override
    public void compile(final CompileContext compileContext,
            Chunk<Module> moduleChunk,
            final VirtualFile[] virtualFiles,
            OutputSink outputSink) {
        if (virtualFiles.length == 0) return;

        List<VirtualFile> productionFiles = new ArrayList<VirtualFile>();
        List<VirtualFile> testFiles = new ArrayList<VirtualFile>();
        for (VirtualFile file : virtualFiles) {
            final boolean inTests = ((CompileContextEx)compileContext).isInTestSourceContent(file);
            if (inTests) {
                testFiles.add(file);
            }
            else {
                productionFiles.add(file);
            }
        }

        final Module module = compileContext.getModuleByFile(virtualFiles[0]);

        doCompile(compileContext, moduleChunk, productionFiles, module, outputSink, false);
        doCompile(compileContext, moduleChunk, testFiles, module, outputSink, true);
    }

    private void doCompile(
            final CompileContext compileContext,
            Chunk<Module> moduleChunk,
            List<VirtualFile> files,
            Module module,
            OutputSink outputSink,
            boolean tests) {
        if (files.isEmpty()) return;

        MessageCollector messageCollector = new MessageCollectorAdapter(compileContext);

        CompilerEnvironment environment = CompilerUtils.getEnvironmentFor(compileContext, module, tests);
        if (!environment.success()) {
            environment.reportErrorsTo(messageCollector);
            return;
        }

        File scriptFile = tryToWriteScriptFile(compileContext, moduleChunk, files, module, tests,
                                               compileContext.getModuleOutputDirectory(module),
                                               environment.getOutput());

        if (scriptFile == null) return;

        CompilerUtils.OutputItemsCollectorImpl collector = new CompilerUtils.OutputItemsCollectorImpl(environment.getOutput().getPath());
        runCompiler(messageCollector, environment, scriptFile, collector);
        outputSink.add(environment.getOutput().getPath(), collector.getOutputs(), collector.getSources().toArray(VirtualFile.EMPTY_ARRAY));
    }

    private static void runCompiler(
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File scriptFile,
            CompilerUtils.OutputItemsCollectorImpl collector
    ) {
        runCompiler(messageCollector, environment, scriptFile, collector, RUN_OUT_OF_PROCESS);
    }

    private static void runCompiler(
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File scriptFile,
            CompilerUtils.OutputItemsCollectorImpl collector,
            boolean runOutOfProcess
    ) {
        if (runOutOfProcess) {
            runOutOfProcess(messageCollector, collector, environment, scriptFile);
        }
        else {
            runInProcess(messageCollector, collector, environment, scriptFile);
        }
    }

    public static File tryToWriteScriptFile(
            CompileContext compileContext,
            Chunk<Module> moduleChunk,
            List<VirtualFile> files,
            Module module,
            boolean tests, VirtualFile mainOutput, File outputDir
    ) {
        ArrayList<String> sourceFilePaths = ContainerUtil.newArrayList(paths(files));
        ModuleChunk chunk = new ModuleChunk((CompileContextEx)compileContext, moduleChunk, Collections.<Module, List<VirtualFile>>emptyMap());
        String moduleName = moduleChunk.getNodes().iterator().next().getName();
        String outputDirectoryForTests = path(compileContext.getModuleOutputDirectoryForTests(module));
        String moduleOutputDirectory = path(compileContext.getModuleOutputDirectory(module));

        // Filter the output we are writing to
        Set<String> outputDirectoriesToFilter = ContainerUtil.newHashSet(outputDirectoryForTests);
        if (!tests) {
            outputDirectoriesToFilter.add(moduleOutputDirectory);
        }
        CharSequence script = KotlinModuleScriptGenerator.generateModuleScript(
                moduleName,
                getDependencyProvider(chunk, tests, mainOutput),
                sourceFilePaths,
                tests,
                outputDirectoriesToFilter
        );

        File scriptFile = new File(outputDir, "script.kts");
        try {
            FileUtil.writeToFile(scriptFile, script.toString());
        }
        catch (IOException e) {
            compileContext.addMessage(CompilerMessageCategory.ERROR, "[Internal Error] Cannot write script to " + scriptFile.getAbsolutePath(), "", -1, -1);
            return null;
        }
        return scriptFile;
    }

    private static KotlinModuleScriptGenerator.DependencyProvider getDependencyProvider(
            final ModuleChunk chunk,
            final boolean tests,
            final VirtualFile mainOutputPath
    ) {
        return new KotlinModuleScriptGenerator.DependencyProvider() {
            @Override
            public void processClassPath(@NotNull KotlinModuleScriptGenerator.DependencyProcessor processor) {
                // TODO: have a bootclasspath in script API
                processor.processClassPathSection("Boot classpath", paths(chunk.getCompilationBootClasspathFiles()));

                processor.processClassPathSection("Compilation classpath", paths(chunk.getCompilationClasspathFiles()));

                // This is for java files in same roots
                processor.processClassPathSection("Java classpath (for Java sources)", paths(Arrays.asList(chunk.getSourceRoots())));


                if (tests && mainOutputPath != null) {
                    processor.processClassPathSection("Main output", Arrays.asList(path(mainOutputPath)));
                }

                processor.processAnnotationRoots(getAnnotationRootPaths(chunk));
            }
        };
    }

    private static void runInProcess(final MessageCollector messageCollector,
            OutputItemsCollector collector,
            final CompilerEnvironment environment,
            final File scriptFile) {
        CompilerUtils.outputCompilerMessagesAndHandleExitCode(messageCollector, collector, new Function1<PrintStream, Integer>() {
            @Override
            public Integer invoke(PrintStream stream) {
                return execInProcess(environment, scriptFile, stream, messageCollector);
            }
        });
    }

    private static int execInProcess(CompilerEnvironment environment, File scriptFile, PrintStream out, MessageCollector messageCollector) {
        try {
            String compilerClassName = "org.jetbrains.jet.cli.jvm.K2JVMCompiler";
            String[] arguments = commandLineArguments(environment.getOutput(), scriptFile);
            messageCollector.report(CompilerMessageSeverity.INFO,
                                    "Using kotlinHome=" + environment.getKotlinHome(),
                                    CompilerMessageLocation.NO_LOCATION);
            messageCollector.report(CompilerMessageSeverity.INFO,
                               "Invoking in-process compiler " + compilerClassName + " with arguments " + Arrays.asList(arguments),
                               CompilerMessageLocation.NO_LOCATION);
            Object rc = CompilerUtils.invokeExecMethod(environment, out, messageCollector, arguments, compilerClassName);
            // exec() returns a K2JVMCompiler.ExitCode object, that class is not accessible here,
            // so we take it's contents through reflection
            return CompilerUtils.getReturnCodeFromObject(rc);
        }
        catch (Throwable e) {
            CompilerUtils.reportException(messageCollector, e);
            return -1;
        }
    }

    private static String[] commandLineArguments(File outputDir, File scriptFile) {
        return new String[]{
                "-module", scriptFile.getAbsolutePath(),
                "-output", outputDir.getPath(),
                "-tags", "-verbose", "-version",
                "-noStdlib", "-noJdkAnnotations", "-noJdk"};
    }

    private static void runOutOfProcess(
            final MessageCollector messageCollector,
            final OutputItemsCollector itemCollector,
            CompilerEnvironment environment,
            File scriptFile
    ) {
        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass("org.jetbrains.jet.cli.jvm.K2JVMCompiler");

        for (String arg : commandLineArguments(environment.getOutput(), scriptFile)) {
            params.getProgramParametersList().add(arg);
        }

        for (File jar : CompilerUtils.kompilerClasspath(environment.getKotlinHome(), messageCollector)) {
            params.getClassPath().add(jar);
        }

        params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
        //        params.getVMParametersList().addParametersString("-agentlib:yjpagent=sampling");

        Sdk sdk = params.getJdk();

        final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(
                ((JavaSdkType)sdk.getSdkType()).getVMExecutablePath(sdk), params, false);

        messageCollector.report(CompilerMessageSeverity.INFO,
                                "Invoking out-of-process compiler with arguments: " + commandLine,
                                CompilerMessageLocation.NO_LOCATION);

        try {
            final Process process = commandLine.createProcess();

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    CompilerUtils
                            .parseCompilerMessagesFromReader(messageCollector, new InputStreamReader(process.getInputStream()), itemCollector);
                }
            });

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileUtil.loadBytes(process.getErrorStream());
                    }
                    catch (IOException e) {
                        // Don't care
                    }
                }
            });

            int exitCode = process.waitFor();
            CompilerUtils.handleProcessTermination(exitCode, messageCollector);
        }
        catch (Exception e) {
            messageCollector.report(CompilerMessageSeverity.ERROR,
                                    "[Internal Error] " + e.getLocalizedMessage(),
                                    CompilerMessageLocation.NO_LOCATION);
            return;
        }
    }

    private static List<String> getAnnotationRootPaths(ModuleChunk chunk) {
        List<String> annotationPaths = ContainerUtil.newArrayList();
        for (Module module : chunk.getModules()) {
            for (VirtualFile file : OrderEnumerator.orderEntries(module).roots(AnnotationOrderRootType.getInstance()).getRoots()) {
                annotationPaths.add(path(file));
            }
        }
        return annotationPaths;
    }

    private static Collection<String> paths(Collection<VirtualFile> files) {
        return ContainerUtil.map(files, new Function<VirtualFile, String>() {
            @Override
            public String fun(VirtualFile file) {
                return path(file);
            }
        });
    }

    private static String path(VirtualFile root) {
        String path = root.getPath();
        if (path.endsWith("!/")) {
            return path.substring(0, path.length() - 2);
        }

        return path;
    }
}
