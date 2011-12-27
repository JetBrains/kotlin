package org.jetbrains.jet.plugin.compiler;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.intellij.openapi.compiler.CompilerMessageCategory.ERROR;

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
    public void compile(final CompileContext compileContext, Chunk<Module> moduleChunk, final VirtualFile[] virtualFiles, OutputSink outputSink) {
        if (virtualFiles.length == 0) return;

        final Module module = compileContext.getModuleByFile(virtualFiles[0]);
        final VirtualFile outputDir = compileContext.getModuleOutputDirectory(module);
        if (outputDir == null) {
            compileContext.addMessage(ERROR, "[Internal Error] No output directory", "", -1, -1);
            return;
        }

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile[] allFiles = compileContext.getCompileScope().getFiles(null, true);

                GenerationState generationState = new GenerationState(compileContext.getProject(), ClassBuilderFactory.BINARIES);
                List<JetFile> files = Lists.newArrayList();
                for (VirtualFile virtualFile : allFiles) {
                    PsiFile psiFile = PsiManager.getInstance(compileContext.getProject()).findFile(virtualFile);
                    if (psiFile instanceof JetFile) {
                        files.add(((JetFile) psiFile));
                    }
                }

                BindingContext bindingContext =
                    AnalyzerFacade.analyzeFilesWithJavaIntegration(compileContext.getProject(), files, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);

                boolean errors = false;
                for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
                    switch (diagnostic.getSeverity()) {
                        case ERROR:
                            errors = true;
                            report(diagnostic, CompilerMessageCategory.ERROR, compileContext);
                            break;
                        case INFO:
                            report(diagnostic, CompilerMessageCategory.INFORMATION, compileContext);                            
                            break;
                        case WARNING:
                            report(diagnostic, CompilerMessageCategory.WARNING, compileContext);                            
                            break;
                    }
                }
                
                if (!errors) {
                    generationState.compileCorrectFiles(bindingContext, files, new CompilationErrorHandler() {
                        @Override
                        public void reportException(Throwable exception, String fileUrl) {
                            if (exception instanceof CompilationException) {
                                report((CompilationException) exception, compileContext);
                            }
                            else {
                                compileContext.addMessage(CompilerMessageCategory.ERROR, exception.getClass().getCanonicalName() + ": " + exception.getMessage(), fileUrl, 0, 0);
                            }
                        }
                    });
///////////
//                    GenerationState generationState2 = new GenerationState(compileContext.getProject(), ClassBuilderFactory.TEXT);
//                    generationState2.compileCorrectFiles(bindingContext, namespaces);
//                    StringBuilder answer = new StringBuilder();
//
//                    final ClassFileFactory factory2 = generationState2.getFactory();
//                    List<String> files2 = factory2.files();
//                    for (String file : files2) {
//                        answer.append("@").append(file).append('\n');
//                        answer.append(factory2.asText(file));
//                    }
//                    System.out.println(answer.toString());
///////////


                    final ClassFileFactory factory = generationState.getFactory();
                    List<String> filesNames = factory.files();
                    for (String file : filesNames) {
                        File target = new File(outputDir.getPath(), file);
                        try {
                            FileUtil.writeToFile(target, factory.asBytes(file));
                        } catch (IOException e) {
                            compileContext.addMessage(ERROR, e.getMessage(), null, 0, 0);
                        }
                    }
                }
            }
        });
        
//        Map<Module, ModuleCompileState> moduleMap = new HashMap<Module, ModuleCompileState>();
//
//        for (VirtualFile virtualFile : virtualFiles) {
//            Module module = compileContext.getModuleByFile(virtualFile);
//            ModuleCompileState state = moduleMap.get(module);
//            if (state == null) {
//                state = new ModuleCompileState(compileContext, module, outputSink);
//                moduleMap.put(module, state);
//            }
//            state.compile(virtualFile);
//        }
//
//        for (ModuleCompileState state : moduleMap.values()) {
//            state.done();
//        }

    }

    private void report(Diagnostic diagnostic, CompilerMessageCategory severity, CompileContext compileContext) {
        PsiFile psiFile = diagnostic.getFactory().getPsiFile(diagnostic);
        TextRange textRange = diagnostic.getFactory().getTextRange(diagnostic);
        Document document = psiFile.getViewProvider().getDocument();
        int line;
        int col;
        if (document != null) {
            line = document.getLineNumber(textRange.getStartOffset());
            col = textRange.getStartOffset() - document.getLineStartOffset(line) + 1;
        }
        else {
            line = -1;
            col = -1;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            compileContext.addMessage(ERROR, "[Internal Error] No virtual file for PsiFile. Diagnostic: " + diagnostic.getMessage(), "", -1, -1);
        }
        else {
            compileContext.addMessage(severity, diagnostic.getMessage(), virtualFile.getUrl(), line + 1, col);
        }
    }

    private void report(CompilationException diagnostic, CompileContext compileContext) {
        PsiFile psiFile = diagnostic.getElement().getContainingFile();
        TextRange textRange = diagnostic.getElement().getTextRange();
        Document document = psiFile.getViewProvider().getDocument();
        int line;
        int col;
        if (document != null) {
            line = document.getLineNumber(textRange.getStartOffset());
            col = textRange.getStartOffset() - document.getLineStartOffset(line) + 1;
        }
        else {
            line = -1;
            col = -1;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            compileContext.addMessage(ERROR, "[Internal Error] No virtual file for PsiFile. Diagnostic: " + diagnostic.getMessage(), "", -1, -1);
        }
        else {
            compileContext.addMessage(ERROR, diagnostic.getMessage(), virtualFile.getUrl(), line + 1, col);
        }
    }

//    private static class ModuleCompileState {
//        private final GenerationState state;
//        private final CompileContext compileContext;
//        private final Module module;
//        private final OutputSink outputSink;
//
//        public ModuleCompileState(final CompileContext compileContext, Module module, OutputSink outputSink) {
//            this.compileContext = compileContext;
//            this.module = module;
//            this.outputSink = outputSink;
//            state = ApplicationManager.getApplication().runReadAction(new Computable<GenerationState>() {
//                @Override
//                public GenerationState compute() {
//                    return new GenerationState(compileContext.getProject(), false);
//                }
//            });
//        }
//
//
//
//        public void compile(final VirtualFile virtualFile) {
//            ApplicationManager.getApplication().runReadAction(new Runnable() {
//                @Override
//                public void run() {
//                    PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(virtualFile);
//                    if (psiFile instanceof JetFile) {
//                        state.compile((JetFile) psiFile);
//                    }
//                }
//            });
//        }
//
//        public void done() {
//            VirtualFile outputDir = compileContext.getModuleOutputDirectory(module);
//            final ClassFileFactory factory = state.getFactory();
//            List<String> files = factory.files();
//            for (String file : files) {
//                File target = new File(outputDir.getPath(), file);
//                try {
//                    FileUtil.writeToFile(target, factory.asBytes(file));
//                } catch (IOException e) {
//                    compileContext.addMessage(ERROR, e.getMessage(), null, 0, 0);
//                }
//            }
//        }
//    }
}
