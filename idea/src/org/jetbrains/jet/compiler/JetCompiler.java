package org.jetbrains.jet.compiler;

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
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.lang.JetFileType;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
        private final Map<String, NamespaceCodegen> ns2codegen = new HashMap<String, NamespaceCodegen>();
        private final CompileContext compileContext;
        private final Module module;
        private final OutputSink outputSink;

        public ModuleCompileState(CompileContext compileContext, Module module, OutputSink outputSink) {
            this.compileContext = compileContext;
            this.module = module;
            this.outputSink = outputSink;
        }

        public void compile(final VirtualFile virtualFile) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(virtualFile);
                    if (psiFile instanceof JetFile) {
                        final JetNamespace namespace = ((JetFile) psiFile).getRootNamespace();
                        String fqName = namespace.getFQName();
                        NamespaceCodegen codegen = ns2codegen.get(fqName);
                        if (codegen == null) {
                            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                            codegen = new NamespaceCodegen(compileContext.getProject(), writer, fqName);
                            ns2codegen.put(fqName, codegen);
                        }
                        codegen.generate(namespace);
                    }
                }
            });
        }

        public void done() {
            VirtualFile outputDir = compileContext.getModuleOutputDirectory(module);

            for (Map.Entry<String, NamespaceCodegen> entry : ns2codegen.entrySet()) {
                NamespaceCodegen codegen = entry.getValue();
                codegen.done();

                try {
                    File nsOutputDir = dirForPackage(outputDir, entry.getKey());
                    ClassWriter writer = (ClassWriter) codegen.getVisitor();
                    File output = new File(nsOutputDir, "namespace.class");
                    FileUtil.writeToFile(output, writer.toByteArray());
                } catch (IOException e) {
                    compileContext.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, 0, 0);
                }
            }
        }

        private static File dirForPackage(VirtualFile root, String fqName) throws IOException {
            File result = new File(root.getPath(), fqName.replace(".", "/"));
            if (!result.exists() && !result.mkdirs()) {
                throw new IOException("Failed to create directory for package");
            }
            return result;
        }
    }
}
