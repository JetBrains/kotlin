package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public class JetCompiler implements TranslatingCompiler {
    @Override
    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext) {
        return virtualFile.getFileType() instanceof JetFileType;
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
    public void compile(CompileContext compileContext, Chunk<Module> moduleChunk, VirtualFile[] virtualFiles, OutputSink outputSink) {
        Map<Module, ModuleCompileState> moduleMap = new HashMap<Module, ModuleCompileState>();

        for (VirtualFile virtualFile : virtualFiles) {
            Module module = compileContext.getModuleByFile(virtualFile);
            ModuleCompileState state = moduleMap.get(module);
            if (state == null) {
                state = new ModuleCompileState(compileContext, module, outputSink);
                moduleMap.put(module, state);
            }
            state.compile(virtualFile);
        }

        for (ModuleCompileState state : moduleMap.values()) {
            state.done();
        }

    }

    private static class ModuleCompileState {
        private final GenerationState state;
        private final CompileContext compileContext;
        private final Module module;
        private final OutputSink outputSink;

        public ModuleCompileState(CompileContext compileContext, Module module, OutputSink outputSink) {
            this.compileContext = compileContext;
            this.module = module;
            this.outputSink = outputSink;
            state = new GenerationState(compileContext.getProject(), false);
        }

        public void compile(final VirtualFile virtualFile) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(virtualFile);
                    if (psiFile instanceof JetFile) {
                        state.compile((JetFile) psiFile);
                    }
                }
            });
        }

        public void done() {
            VirtualFile outputDir = compileContext.getModuleOutputDirectory(module);
            final ClassFileFactory factory = state.getFactory();
            List<String> files = factory.files();
            for (String file : files) {
                File target = new File(outputDir.getPath(), file);
                try {
                    FileUtil.writeToFile(target, factory.asBytes(file));
                } catch (IOException e) {
                    compileContext.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, 0, 0);
                }
            }
        }
    }
}
