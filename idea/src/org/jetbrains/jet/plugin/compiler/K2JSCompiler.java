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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.compiler.CompilerSettings;
import org.jetbrains.jet.compiler.runner.CompilerEnvironment;
import org.jetbrains.jet.compiler.runner.KotlinCompilerRunner;
import org.jetbrains.jet.compiler.runner.OutputItemsCollectorImpl;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.compiler.configuration.Kotlin2JsCompilerArgumentsHolder;
import org.jetbrains.jet.plugin.compiler.configuration.KotlinCommonCompilerArgumentsHolder;
import org.jetbrains.jet.plugin.compiler.configuration.KotlinCompilerSettings;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        doCompile(messageCollector, sink, module, environment, files);
    }

    private static void doCompile(
            @NotNull MessageCollector messageCollector, @NotNull OutputSink sink, @NotNull Module module,
            @NotNull CompilerEnvironment environment, VirtualFile[] files
    ) {
        List<File> srcFiles = ContainerUtil.map(files, new Function<VirtualFile, File>() {
            @Override
            public File fun(VirtualFile file) {
                return new File(file.getPath());
            }
        });
        List<String> libraryFiles = getLibraryFiles(module);
        File outDir = environment.getOutput();
        File outFile = new File(outDir, module.getName() + ".js");

        OutputItemsCollectorImpl outputItemsCollector = new OutputItemsCollectorImpl();

        Project project = module.getProject();
        CommonCompilerArguments commonArguments = KotlinCommonCompilerArgumentsHolder.getInstance(project).getSettings();
        K2JSCompilerArguments k2jsArguments = Kotlin2JsCompilerArgumentsHolder.getInstance(project).getSettings();
        CompilerSettings compilerSettings = KotlinCompilerSettings.getInstance(project).getSettings();

        KotlinCompilerRunner.runK2JsCompiler(commonArguments, k2jsArguments, compilerSettings, messageCollector, environment,
                                             outputItemsCollector, srcFiles, libraryFiles, outFile);

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(outDir);
            assert virtualFile != null : "Virtual file not found for module output: " + outDir;
            virtualFile.refresh(false, true);
        }

        TranslatingCompilerUtils.reportOutputs(sink, environment.getOutput(), outputItemsCollector);
    }

    @Nullable
    private static Module getModule(@NotNull CompileContext context, @NotNull Chunk<Module> moduleChunk) {
        if (moduleChunk.getNodes().size() != 1) {
            context.addMessage(CompilerMessageCategory.ERROR, "K2JSCompiler does not support multiple modules.", null, -1, -1);
            return null;
        }
        return moduleChunk.getNodes().iterator().next();
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

    private static List<String> getLibraryFiles(@NotNull Module module) {
        List<String> result = new ArrayList<String>();

        List<String> libLocationAndTarget = KotlinFrameworkDetector.getLibLocationForProject(module);

        THashSet<Module> modules = new THashSet<Module>();
        collectModuleDependencies(module, modules);
        if (!modules.isEmpty()) {
            for (Module dependency : modules) {
                result.add("@" + dependency.getName());

                for (VirtualFile file : getSourceFiles(dependency)) {
                    result.add(file.getPath());
                }
            }
        }

        for (String file : libLocationAndTarget) {
            result.add(file);
        }

        return result;
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
