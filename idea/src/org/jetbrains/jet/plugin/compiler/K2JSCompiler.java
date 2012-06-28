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
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Chunk;
import com.intellij.util.StringBuilderSpinAllocator;
import gnu.trove.THashSet;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.project.JsModuleDetector;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

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
        Module module = context.getModuleByFile(file);
        if (module == null) {
            return false;
        }
        return JsModuleDetector.isJsModule(module);
    }

    @Override
    public void compile(final CompileContext context, Chunk<Module> moduleChunk, final VirtualFile[] files, OutputSink sink) {
        if (files.length == 0) {
            return;
        }

        Module module = getModule(context, moduleChunk);
        if (module == null) {
            return;
        }

        CompilerEnvironment environment = CompilerEnvironment.getEnvironmentFor(context, module, /*tests = */ false);
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
        VirtualFile outDir = context.getModuleOutputDirectory(module);
        String outFile = outDir == null ? null : outDir.getPath() + "/" + module.getName() + ".js";
        String[] commandLineArgs = constructArguments(module, outFile);
        Object rc = invokeExecMethod(environment, out, context, commandLineArgs, "org.jetbrains.jet.cli.js.K2JSCompiler");

        if (outDir != null && !ApplicationManager.getApplication().isUnitTestMode()) {
            outDir.refresh(false, true);
        }
        return CompilerUtils.getReturnCodeFromObject(rc);
    }

    @NotNull
    private static String[] constructArguments(@NotNull Module module, @Nullable String outFile) {
        VirtualFile[] sourceFiles = getSourceFiles(module);

        ArrayList<String> args = Lists.newArrayList("-tags", "-verbose", "-version");
        addPathToSourcesDir(sourceFiles, args);
        addOutputPath(outFile, args);
        addLibLocationAndTarget(module, args);
        return ArrayUtil.toStringArray(args);
    }

    // we cannot use OrderEnumerator because it has critical bug — try https://gist.github.com/2953261, processor will never be called for module dependency
    // we don't use context.getCompileScope().getAffectedModules() because we want to know about linkage type (well, we ignore scope right now, but in future...)
    private static void collectModuleDependencies(Module dependentModule, Set<Module> modules) {
        for (OrderEntry entry : ModuleRootManager.getInstance(dependentModule).getOrderEntries()) {
            if (entry instanceof ModuleOrderEntry) {
                ModuleOrderEntry moduleEntry = (ModuleOrderEntry) entry;
                if (!moduleEntry.getScope().isForProductionCompile()) {
                    continue;
                }

                Module module = moduleEntry.getModule();
                if (module == null) {
                    continue;
                }

                if (modules.add(module) && moduleEntry.isExported()) {
                    collectModuleDependencies(module, modules);
                }
            }
        }
    }

    private static VirtualFile[] getSourceFiles(@NotNull Module module) {
        return CompilerManager.getInstance(module.getProject()).createModuleCompileScope(module, false)
                .getFiles(JetFileType.INSTANCE, true);
    }

    private static void addLibLocationAndTarget(@NotNull Module module, @NotNull ArrayList<String> args) {
        Pair<String[], String> libLocationAndTarget = JsModuleDetector.getLibLocationAndTargetForProject(module);

        StringBuilder sb = StringBuilderSpinAllocator.alloc();
        AccessToken token = ReadAction.start();
        try {
            THashSet<Module> modules = new THashSet<Module>();
            collectModuleDependencies(module, modules);
            if (!modules.isEmpty()) {
                for (Module dependency : modules) {
                    sb.append('@').append(dependency.getName()).append(',');

                    for (VirtualFile file : getSourceFiles(dependency)) {
                        sb.append(file.getPath()).append(',');
                    }
                }
            }

            if (libLocationAndTarget.first != null) {
                for (String file : libLocationAndTarget.first) {
                    sb.append(file).append(',');
                }
            }

            if (sb.length() > 0) {
                args.add("-libraryFiles");
                args.add(sb.substring(0, sb.length() - 1));
            }
        }
        finally {
            token.finish();
            StringBuilderSpinAllocator.dispose(sb);
        }

        if (libLocationAndTarget.second != null) {
            args.add("-target");
            args.add(libLocationAndTarget.second);
        }
    }

    private static void addPathToSourcesDir(@NotNull VirtualFile[] sourceFiles, @NotNull ArrayList<String> args) {
        args.add("-sourceFiles");

        StringBuilder sb = StringBuilderSpinAllocator.alloc();
        try {
            for (VirtualFile file : sourceFiles) {
                sb.append(file.getPath()).append(',');
            }
            args.add(sb.substring(0, sb.length() - 1));
        }
        finally {
            StringBuilderSpinAllocator.dispose(sb);
        }
    }

    private static void addOutputPath(@Nullable String outFile, @NotNull ArrayList<String> args) {
        if (outFile != null) {
            args.add("-output");
            args.add(outFile);
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
