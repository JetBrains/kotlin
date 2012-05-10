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

import com.google.common.collect.Sets;
import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.SystemProperties;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.project.JsModuleDetector;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

import static com.intellij.openapi.compiler.CompilerMessageCategory.ERROR;
import static com.intellij.openapi.compiler.CompilerMessageCategory.INFORMATION;

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
        Project project = compileContext.getProject();
        if (project == null || JsModuleDetector.isJsProject(project)) {
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

    private void doCompile(CompileContext compileContext,
            Chunk<Module> moduleChunk,
            List<VirtualFile> files,
            Module module,
            OutputSink outputSink,
            boolean tests) {
        if (files.isEmpty()) return;

        CompilerEnvironment environment = CompilerEnvironment.getEnvironmentFor(compileContext, module, tests);
        if (!environment.success()) {
            environment.reportErrorsTo(compileContext);
            return;
        }

        File scriptFile =
                tryToWriteScriptFile(compileContext, moduleChunk, files, module, tests, compileContext.getModuleOutputDirectory(module),
                                     environment.getOutput());

        if (scriptFile == null) return;

        CompilerUtils.OutputItemsCollectorImpl collector = new CompilerUtils.OutputItemsCollectorImpl(environment.getOutput().getPath());
        runCompiler(compileContext, environment, scriptFile, collector);
        outputSink.add(environment.getOutput().getPath(), collector.getOutputs(), collector.getSources().toArray(VirtualFile.EMPTY_ARRAY));
    }

    private void runCompiler(CompileContext compileContext,
            CompilerEnvironment environment,
            File scriptFile,
            CompilerUtils.OutputItemsCollectorImpl collector) {
        if (RUN_OUT_OF_PROCESS) {
            runOutOfProcess(compileContext, collector, environment, scriptFile);
        }
        else {
            runInProcess(compileContext, collector, environment, scriptFile);
        }
    }

    private static File tryToWriteScriptFile(CompileContext compileContext,
            Chunk<Module> moduleChunk,
            List<VirtualFile> files,
            Module module,
            boolean tests, VirtualFile mainOutput, VirtualFile outputDir) {
        ModuleChunk
                chunk = new ModuleChunk((CompileContextEx)compileContext, moduleChunk, Collections.<Module, List<VirtualFile>>emptyMap());
        String moduleName = moduleChunk.getNodes().iterator().next().getName();

        // Filter the output we are writing to
        Set<VirtualFile> outputDirectoriesToFilter = Sets.newHashSet(compileContext.getModuleOutputDirectoryForTests(module));
        if (!tests) {
            outputDirectoriesToFilter.add(compileContext.getModuleOutputDirectory(module));
        }
        CharSequence script = generateModuleScript(moduleName, chunk, files, tests, mainOutput, outputDirectoriesToFilter);

        File scriptFile = new File(path(outputDir), "script.kts");
        try {
            FileUtil.writeToFile(scriptFile, script.toString());
        }
        catch (IOException e) {
            compileContext.addMessage(ERROR, "[Internal Error] Cannot write script to " + scriptFile.getAbsolutePath(), "", -1, -1);
            return null;
        }
        return scriptFile;
    }

    private static CharSequence generateModuleScript(String moduleName,
            ModuleChunk chunk,
            List<VirtualFile> files,
            boolean tests,
            VirtualFile mainOutput,
            Set<VirtualFile> directoriesToFilterOut) {
        StringBuilder script = new StringBuilder();

        if (tests) {
            script.append("// Module script for tests\n");
        }
        else {
            script.append("// Module script for production\n");
        }

        script.append("import kotlin.modules.*\n");
        script.append("fun project() {\n");
        script.append("    module(\"" + moduleName + "\") {\n");

        for (VirtualFile sourceFile : files) {
            script.append("        sources += \"" + path(sourceFile) + "\"\n");
        }

        // TODO: have a bootclasspath in script API
        script.append("        // Boot classpath\n");
        for (VirtualFile root : chunk.getCompilationBootClasspathFiles()) {
            script.append("        classpath += \"" + path(root) + "\"\n");
        }

        script.append("        // Compilation classpath\n");
        for (VirtualFile root : chunk.getCompilationClasspathFiles()) {
            String path = path(root);
            if (directoriesToFilterOut.contains(root)) {
                // For IDEA's make (incremental compilation) purposes, output directories of the current module and its dependencies
                // appear on the class path, so we are at risk of seeing the results of the previous build, i.e. if some class was
                // removed in the sources, it may still be there in binaries. Thus, we delete these entries from the classpath.
                script.append("        // Output directory, commented out\n");
                script.append("        // ");
            }
            script.append("        classpath += \"" + path + "\"\n");
        }

        // This is for java files in same roots
        script.append("        // Java classpath (for Java sources)\n");
        for (VirtualFile root : chunk.getSourceRoots()) {
            script.append("        classpath += \"" + path(root) + "\"\n");
        }

        script.append("        // Main output\n");
        if (tests && mainOutput != null) {
            script.append("        classpath += \"" + path(mainOutput) + "\"\n");
        }

        script.append("    }\n");
        script.append("}\n");
        return script;
    }

    private static void runInProcess(final CompileContext compileContext,
            OutputItemsCollector collector,
            final CompilerEnvironment environment,
            final File scriptFile) {
        CompilerUtils.outputCompilerMessagesAndHandleExitCode(compileContext, collector, new Function1<PrintStream, Integer>() {
            @Override
            public Integer invoke(PrintStream stream) {
                return execInProcess(environment, scriptFile, stream, compileContext);
            }
        });
    }

    private static int execInProcess(CompilerEnvironment environment, File scriptFile, PrintStream out, CompileContext context) {
        try {
            String compilerClassName = "org.jetbrains.jet.cli.jvm.K2JVMCompiler";
            String[] arguments = commandLineArguments(environment.getOutput(), scriptFile);
            context.addMessage(INFORMATION, "Using kotlinHome=" + environment.getKotlinHome(), "", -1, -1);
            context.addMessage(INFORMATION,
                               "Invoking in-process compiler " + compilerClassName + " with arguments " + Arrays.asList(arguments), "", -1,
                               -1);
            Object rc = CompilerUtils.invokeExecMethod(environment, out, context, arguments, compilerClassName);
            // exec() returns a K2JVMCompiler.ExitCode object, that class is not accessible here,
            // so we take it's contents through reflection
            return CompilerUtils.getReturnCodeFromObject(rc);
        }
        catch (Throwable e) {
            CompilerUtils.LOG.error(e);
            return -1;
        }
    }

    private static String[] commandLineArguments(VirtualFile outputDir, File scriptFile) {
        return new String[]{
                "-module", scriptFile.getAbsolutePath(),
                "-output", path(outputDir),
                "-tags", "-verbose", "-version",
                "-mode", "idea"};
    }

    private static void runOutOfProcess(final CompileContext compileContext,
            final OutputItemsCollector collector,
            CompilerEnvironment environment,
            File scriptFile) {
        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass("org.jetbrains.jet.cli.jvm.K2JVMCompiler");

        for (String arg : commandLineArguments(environment.getOutput(), scriptFile)) {
            params.getProgramParametersList().add(arg);
        }

        for (File jar : CompilerUtils.kompilerClasspath(environment.getKotlinHome(), compileContext)) {
            params.getClassPath().add(jar);
        }

        params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
        //        params.getVMParametersList().addParametersString("-agentlib:yjpagent=sampling");

        Sdk sdk = params.getJdk();

        final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(
                ((JavaSdkType)sdk.getSdkType()).getVMExecutablePath(sdk), params, false);

        compileContext.addMessage(INFORMATION, "Invoking out-of-process compiler with arguments: " + commandLine, "", -1, -1);

        try {
            final Process process = commandLine.createProcess();

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    CompilerUtils
                            .parseCompilerMessagesFromReader(compileContext, new InputStreamReader(process.getInputStream()), collector);
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
            CompilerUtils.handleProcessTermination(exitCode, compileContext);
        }
        catch (Exception e) {
            compileContext.addMessage(ERROR, "[Internal Error] " + e.getLocalizedMessage(), "", -1, -1);
            return;
        }
    }

    private static String path(VirtualFile root) {
        String path = root.getPath();
        if (path.endsWith("!/")) {
            return path.substring(0, path.length() - 2);
        }

        return path;
    }
}
