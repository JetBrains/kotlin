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

package org.jetbrains.jet.plugin.compiler;

import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.compiler.runner.*;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JetCompiler implements TranslatingCompiler {

    private static final boolean RUN_OUT_OF_PROCESS = false;

    @Override
    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext) {
        if (!(virtualFile.getFileType() instanceof JetFileType)) {
            return false;
        }
        Module module = compileContext.getModuleByFile(virtualFile);
        if (module != null && KotlinFrameworkDetector.isJsKotlinModule(module)) {
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
    public void compile(
            CompileContext compileContext,
            Chunk<Module> moduleChunk,
            VirtualFile[] virtualFiles,
            OutputSink outputSink) {
        if (virtualFiles.length == 0) return;

        List<VirtualFile> productionFiles = new ArrayList<VirtualFile>();
        List<VirtualFile> testFiles = new ArrayList<VirtualFile>();
        for (VirtualFile file : virtualFiles) {
            boolean inTests = ((CompileContextEx)compileContext).isInTestSourceContent(file);
            if (inTests) {
                testFiles.add(file);
            }
            else {
                productionFiles.add(file);
            }
        }

        Module module = compileContext.getModuleByFile(virtualFiles[0]);

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

        CompilerEnvironment environment = TranslatingCompilerUtils.getEnvironmentFor(compileContext, module, tests);
        if (!environment.success()) {
            environment.reportErrorsTo(messageCollector);
            return;
        }

        final File outputDir = environment.getOutput();

        File scriptFile = tryToWriteScriptFile(compileContext, moduleChunk, files, module, tests,
                                               compileContext.getModuleOutputDirectory(module),
                                               outputDir);

        if (scriptFile == null) return;

        OutputItemsCollectorImpl collector = new OutputItemsCollectorImpl(outputDir) {
            @Override
            public void add(Collection<File> sourceFiles, File outputFile) {
                super.add(sourceFiles, outputFile);
                compileContext.getProgressIndicator().setText("Emitting: " + outputFile);
            }
        };
        runCompiler(messageCollector, environment, scriptFile, collector);

        TranslatingCompilerUtils.reportOutputs(outputSink, outputDir, collector);
    }

    private static void runCompiler(
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File scriptFile,
            OutputItemsCollector outputItemsCollector
    ) {
        KotlinCompilerRunner.runCompiler(messageCollector, environment, scriptFile, outputItemsCollector, RUN_OUT_OF_PROCESS);
    }

    public static File tryToWriteScriptFile(
            CompileContext compileContext,
            Chunk<Module> moduleChunk,
            List<VirtualFile> files,
            Module module,
            boolean tests, VirtualFile mainOutput, File outputDir
    ) {
        List<File> sourceFiles = ContainerUtil.newArrayList(ioFiles(files));
        ModuleChunk chunk = new ModuleChunk((CompileContextEx)compileContext, moduleChunk, Collections.<Module, List<VirtualFile>>emptyMap());
        String moduleName = moduleChunk.getNodes().iterator().next().getName();
        File outputDirectoryForTests = ioFile(compileContext.getModuleOutputDirectoryForTests(module));
        File moduleOutputDirectory = ioFile(compileContext.getModuleOutputDirectory(module));

        // Filter the output we are writing to
        Set<File> outputDirectoriesToFilter = ContainerUtil.newHashSet(outputDirectoryForTests);
        if (!tests) {
            outputDirectoriesToFilter.add(moduleOutputDirectory);
        }
        CharSequence script = KotlinModuleScriptGenerator.generateModuleScript(
                moduleName,
                getDependencyProvider(chunk, tests, mainOutput),
                sourceFiles,
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
                processor.processClassPathSection("Boot classpath", ioFiles(chunk.getCompilationBootClasspathFiles()));

                processor.processClassPathSection("Compilation classpath", ioFiles(chunk.getCompilationClasspathFiles()));

                // This is for java files in same roots
                processor.processClassPathSection("Java classpath (for Java sources)", ioFiles(Arrays.asList(chunk.getSourceRoots())));


                if (tests && mainOutputPath != null) {
                    processor.processClassPathSection("Main output", Arrays.asList(ioFile(mainOutputPath)));
                }

                processor.processAnnotationRoots(getAnnotationRootPaths(chunk));
            }
        };
    }

    private static List<File> getAnnotationRootPaths(ModuleChunk chunk) {
        List<File> annotationPaths = ContainerUtil.newArrayList();
        for (Module module : chunk.getModules()) {
            for (VirtualFile file : OrderEnumerator.orderEntries(module).roots(AnnotationOrderRootType.getInstance()).getRoots()) {
                annotationPaths.add(ioFile(file));
            }
        }
        return annotationPaths;
    }

    private static Collection<File> ioFiles(Collection<VirtualFile> files) {
        return ContainerUtil.map(files, new Function<VirtualFile, File>() {
            @Override
            public File fun(VirtualFile file) {
                return ioFile(file);
            }
        });
    }

    private static File ioFile(VirtualFile file) {
        return new File(path(file));
    }

    private static String path(VirtualFile root) {
        String path = root.getPath();
        if (path.endsWith("!/")) {
            return path.substring(0, path.length() - 2);
        }

        return path;
    }
}
