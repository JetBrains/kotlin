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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Chunk;
import com.intellij.util.Function;
import com.intellij.util.StringBuilderSpinAllocator;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.compiler.runner.CompilerEnvironment;
import org.jetbrains.jet.compiler.runner.CompilerRunnerUtil;
import org.jetbrains.jet.compiler.runner.OutputItemsCollectorImpl;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.compiler.runner.CompilerRunnerUtil.invokeExecMethod;
import static org.jetbrains.jet.compiler.runner.CompilerRunnerUtil.outputCompilerMessagesAndHandleExitCode;

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
        return KotlinFrameworkDetector.isJsKotlinModule(module);
    }

    @Override
    public void compile(CompileContext context, Chunk<Module> moduleChunk, VirtualFile[] files, OutputSink sink) {
        if (files.length == 0) {
            return;
        }

        Module module = getModule(context, moduleChunk);
        if (module == null) {
            return;
        }

        MessageCollector messageCollector = new MessageCollectorAdapter(context);

        CompilerEnvironment environment = TranslatingCompilerUtils.getEnvironmentFor(context, module, /*tests = */ false);
        if (!environment.success()) {
            environment.reportErrorsTo(messageCollector);
            return;
        }

        doCompile(messageCollector, sink, module, environment);
    }

    private static void doCompile(@NotNull final MessageCollector messageCollector, @NotNull OutputSink sink, @NotNull final Module module,
            @NotNull final CompilerEnvironment environment) {
        OutputItemsCollectorImpl collector = new OutputItemsCollectorImpl(environment.getOutput());
        outputCompilerMessagesAndHandleExitCode(messageCollector, collector, new Function<PrintStream, Integer>() {
            @Override
            public Integer fun(PrintStream stream) {
                return execInProcess(messageCollector, environment, stream, module);
            }
        });
        TranslatingCompilerUtils.reportOutputs(sink, environment.getOutput(), collector);
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
    private static Integer execInProcess(@NotNull MessageCollector messageCollector,
            @NotNull CompilerEnvironment environment, @NotNull PrintStream out, @NotNull Module module) {
        try {
            return doExec(messageCollector, environment, out, module);
        }
        catch (Throwable e) {
            messageCollector.report(CompilerMessageSeverity.ERROR,
                                    "Exception while executing compiler:\n" + e.getMessage(),
                                    CompilerMessageLocation.NO_LOCATION);
        }
        return -1;
    }

    @NotNull
    private static Integer doExec(@NotNull MessageCollector messageCollector, @NotNull CompilerEnvironment environment, @NotNull PrintStream out,
            @NotNull Module module) throws Exception {
        File outDir = environment.getOutput();
        File outFile = new File(outDir, module.getName() + ".js");
        String[] commandLineArgs = constructArguments(module, outFile);
        Object rc = invokeExecMethod(environment, out, messageCollector, commandLineArgs, "org.jetbrains.jet.cli.js.K2JSCompiler");

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(outDir);
            assert virtualFile != null : "Virtual file not found for module output: " + outDir;
            virtualFile.refresh(false, true);
        }
        return CompilerRunnerUtil.getReturnCodeFromObject(rc);
    }

    @NotNull
    private static String[] constructArguments(@NotNull Module module, @NotNull File outFile) {
        VirtualFile[] sourceFiles = getSourceFiles(module);

        ArrayList<String> args = Lists.newArrayList("-tags", "-verbose", "-version");
        addPathToSourcesDir(sourceFiles, args);
        addOutputPath(outFile, args);
        addLibLocationAndTarget(module, args);
        return ArrayUtil.toStringArray(args);
    }

    // we cannot use OrderEnumerator because it has critical bug - try https://gist.github.com/2953261, processor will never be called for module dependency
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
        Pair<List<String>, String> libLocationAndTarget = KotlinFrameworkDetector.getLibLocationAndTargetForProject(module);

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

    private static void addOutputPath(@NotNull File outFile, @NotNull ArrayList<String> args) {
        args.add("-output");
        args.add(outFile.getPath());
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
