package org.jetbrains.jet.plugin.compiler;

import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.Chunk;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.CompilationException;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
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

        File kotlinHome = PathUtil.getDefaultCompilerPath();
        if (kotlinHome == null) {
            compileContext.addMessage(ERROR, "Cannot file kotlinc home. Make sure plugin is properly installed", "", -1, -1);
            return;
        }
        
        
        StringBuilder script = new StringBuilder();

        script.append("import kotlin.modules.*\n");
        script.append("fun project() {\n");
        script.append("module(\"" + moduleChunk.getNodes().iterator().next().getName() + "\") {\n");

        for (VirtualFile sourceFile : virtualFiles) {
            script.append("sources += \"" + path(sourceFile) + "\"\n");
        }

        ModuleChunk chunk = new ModuleChunk((CompileContextEx) compileContext, moduleChunk, Collections.<Module, List<VirtualFile>>emptyMap());

        // TODO: have a bootclasspath in script API
        for (VirtualFile root : chunk.getCompilationBootClasspathFiles()) {
            script.append("classpath += \"" + path(root) + "\"\n");
        }

        for (VirtualFile root : chunk.getCompilationClasspathFiles()) {
            script.append("classpath += \"" + path(root) + "\"\n");
        }

        script.append("}\n");
        script.append("}\n");

        File scriptFile = new File(path(outputDir), "script.kts");
        try {
            FileUtil.writeToFile(scriptFile, script.toString());
        } catch (IOException e) {
            compileContext.addMessage(ERROR, "[Internal Error] Cannot write script to " + scriptFile.getAbsolutePath(), "", -1, -1);
            return;
        }


        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass("org.jetbrains.jet.cli.KotlinCompiler");
        params.getProgramParametersList().add("-module", scriptFile.getAbsolutePath());
        params.getProgramParametersList().add("-output", path(outputDir));

        File libs = new File(kotlinHome, "lib");

        if (!libs.exists() || libs.isFile()) {
            compileContext.addMessage(ERROR, "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed", "", -1, -1);
            return;
        }

        File[] jars = libs.listFiles();
        if (jars != null) {
            for (File jar : jars) {
                if (jar.isFile() && jar.getName().endsWith(".jar")) {
                    params.getClassPath().add(jar);
                }
            }
        }
        
        params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
        
        
        Sdk sdk = params.getJdk();

        final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(
                ((JavaSdkType) sdk.getSdkType()).getVMExecutablePath(sdk), params, false);
        try {
            final OSProcessHandler processHandler = new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString()) {
              @Override
              public Charset getCharset() {
                return commandLine.getCharset();
              }
            };

            processHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(ProcessEvent event, Key outputType) {
                    System.out.println(event.getText());
                }
            });

            processHandler.startNotify();
            processHandler.waitFor();
        } catch (Exception e) {
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
