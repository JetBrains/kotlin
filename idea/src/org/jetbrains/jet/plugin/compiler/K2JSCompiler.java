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

import com.google.common.collect.Lists;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Chunk;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.k2jsrun.K2JSRunnerUtils;
import org.jetbrains.jet.plugin.project.JsModuleDetector;

import java.io.PrintStream;
import java.util.ArrayList;

import static org.jetbrains.jet.plugin.compiler.CompilerUtils.invokeExecMethod;
import static org.jetbrains.jet.plugin.compiler.CompilerUtils.outputCompilerMessagesAndHandleExitCode;

/**
 * @author Pavel Talanov
 */
public final class K2JSCompiler implements TranslatingCompiler {

    @Override
    public boolean isCompilableFile(VirtualFile file, CompileContext context) {
        if (!(file.getFileType() instanceof JetFileType)) {
            return false;
        }
        Project project = context.getProject();
        if (project == null) {
            return false;
        }
        return JsModuleDetector.isJsProject(project);
    }

    @Override
    public void compile(final CompileContext context, Chunk<Module> moduleChunk, final VirtualFile[] files, OutputSink sink) {
        if (files.length == 0) {
            return;
        }

        final Module module = getModule(context, moduleChunk);
        if (module == null) {
            return;
        }

        final CompilerEnvironment environment = CompilerEnvironment.getEnvironmentFor(context, module, /*tests = */ false);
        if (!environment.success()) {
            environment.reportErrorsTo(context);
            return;
        }

        doCompile(context, sink, module, environment);
    }

    private static void doCompile(@NotNull final CompileContext context, @NotNull OutputSink sink, @NotNull final Module module,
            @NotNull final CompilerEnvironment environment) {
        CompilerUtils.OutputItemsCollectorImpl collector = new CompilerUtils.OutputItemsCollectorImpl(environment.getOutput().getPath());
        outputCompilerMessagesAndHandleExitCode(context, collector, new Function1<PrintStream, Integer>() {
            @Override
            public Integer invoke(PrintStream stream) {
                return execInProcess(context, environment, stream, module);
            }
        });
        sink.add(environment.getOutput().getPath(), collector.getOutputs(), collector.getSources().toArray(VirtualFile.EMPTY_ARRAY));
    }

    @Nullable
    private static Module getModule(@NotNull CompileContext context, @NotNull Chunk<Module> moduleChunk) {
        if (moduleChunk.getNodes().size() != 1) {
            context.addMessage(CompilerMessageCategory.ERROR, "K2JSCompiler does not support multiple modules.", null, -1, -1);
            return null;
        }
        return moduleChunk.getNodes().iterator().next();
    }

    @NotNull
    private static Integer execInProcess(@NotNull CompileContext context,
            @NotNull CompilerEnvironment environment, @NotNull PrintStream out, @NotNull Module module) {
        try {
            return doExec(context, environment, out, module);
        }
        catch (Throwable e) {
            context.addMessage(CompilerMessageCategory.ERROR, "Exception while executing compiler:\n" + e.getMessage(), null, -1, -1);
        }
        return -1;
    }

    @NotNull
    private static Integer doExec(@NotNull CompileContext context, @NotNull CompilerEnvironment environment, @NotNull PrintStream out,
            @NotNull Module module) throws Exception {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getSourceRoots();
        if (roots.length != 1) {
            context.addMessage(CompilerMessageCategory.ERROR, "K2JSCompiler does not support multiple module source roots.", null, -1, -1);
            return -1;
        }
        String[] commandLineArgs = constructArguments(context.getProject(), context.getModuleOutputDirectory(module), roots[0]);
        Object rc = invokeExecMethod(environment, out, context, commandLineArgs, "org.jetbrains.jet.cli.js.K2JSCompiler");
        return CompilerUtils.getReturnCodeFromObject(rc);
    }

    @NotNull
    private static String[] constructArguments(@NotNull Project project, @Nullable VirtualFile outDir, @NotNull VirtualFile srcDir) {
        ArrayList<String> args = Lists.newArrayList("-tags", "-verbose", "-version");
        addPathToSourcesDir(args, srcDir);
        addOutputPath(project, outDir, args);
        addPathToZippedLib(project, args);
        return ArrayUtil.toStringArray(args);
    }

    private static void addPathToSourcesDir(@NotNull ArrayList<String> args, @NotNull VirtualFile srcDir) {
        String srcPath = srcDir.getPath();
        args.add("-srcdir");
        args.add(srcPath);
    }

    private static void addOutputPath(@NotNull Project project, @Nullable VirtualFile outDir, @NotNull ArrayList<String> args) {
        if (outDir != null) {
            String outPath = outDir.getPath();
            String outFile = K2JSRunnerUtils.constructPathToGeneratedFile(project, outPath);
            args.add("-output");
            args.add(outFile);
        }
    }

    private static void addPathToZippedLib(@NotNull Project project, @NotNull ArrayList<String> args) {
        String libLocationForProject = JsModuleDetector.getLibLocationForProject(project);
        if (libLocationForProject != null) {
            args.add("-libzip");
            args.add(libLocationForProject);
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Kotlin to JavaScript compiler";
    }

    @Override
    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }
}
