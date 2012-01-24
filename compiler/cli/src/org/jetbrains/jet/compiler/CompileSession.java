package org.jetbrains.jet.compiler;

import com.google.common.base.Predicates;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The session which handles analyzing and compiling a single module.
 *
 * @author yole
 */
public class CompileSession {
    private final JetCoreEnvironment myEnvironment;
    private final List<JetFile> mySourceFiles = new ArrayList<JetFile>();
    private final List<JetFile> myLibrarySourceFiles = new ArrayList<JetFile>();
    private List<String> myErrors = new ArrayList<String>();

    public BindingContext getMyBindingContext() {
        return myBindingContext;
    }

    private BindingContext myBindingContext;

    public CompileSession(JetCoreEnvironment environment) {
        myEnvironment = environment;
    }
    
    public void addSources(String path) {
        if(path == null)
            return;

        VirtualFile vFile = myEnvironment.getLocalFileSystem().findFileByPath(path);
        if (vFile == null) {
            myErrors.add("File/directory not found: " + path);
            return;
        }
        if (!vFile.isDirectory() && vFile.getFileType() != JetFileType.INSTANCE) {
            myErrors.add("Not a Kotlin file: " + path);
            return;
        }

        addSources(new File(path), false);
    }

    private void addSources(File file, boolean library) {
        if(file.isDirectory()) {
            for (File child : file.listFiles()) {
                addSources(child, library);
            }
        }
        else {
            VirtualFile fileByPath = myEnvironment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
            PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(fileByPath);
            if(psiFile instanceof JetFile) {
                (library ? myLibrarySourceFiles : mySourceFiles).add((JetFile) psiFile);
            }
        }
    }

    public void addSources(VirtualFile vFile) {
        addSources(vFile, false);
    }

    private void addSources(VirtualFile vFile, boolean library) {
        if  (vFile.isDirectory())  {
            for (VirtualFile virtualFile : vFile.getChildren()) {
                addSources(virtualFile, library);
            }
        }
        else {
            if (vFile.getFileType() == JetFileType.INSTANCE) {
                PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(vFile);
                if (psiFile instanceof JetFile) {
                    (library ? myLibrarySourceFiles : mySourceFiles).add((JetFile) psiFile);
                }
            }
        }
    }

    public List<JetFile> getSourceFileNamespaces() {
        return mySourceFiles;
    }

    public boolean analyze(final PrintStream out) {
        if (!myErrors.isEmpty()) {
            for (String error : myErrors) {
                out.println(error);
            }
            return false;
        }
        List<JetFile> allNamespaces = new ArrayList<JetFile>(mySourceFiles);
        allNamespaces.addAll(myLibrarySourceFiles);
        myBindingContext = AnalyzerFacade.analyzeFilesWithJavaIntegration(
                myEnvironment.getProject(), allNamespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);
        ErrorCollector errorCollector = new ErrorCollector(myBindingContext);
        errorCollector.report(out);
        return !errorCollector.hasErrors;
    }

    @NotNull
    public ClassFileFactory generate() {
        GenerationState generationState = new GenerationState(myEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        generationState.compileCorrectFiles(myBindingContext, mySourceFiles);
        return generationState.getFactory();
    }

    public String generateText() {
        GenerationState generationState = new GenerationState(myEnvironment.getProject(), ClassBuilderFactory.TEXT);
        generationState.compileCorrectFiles(myBindingContext, mySourceFiles);
        return generationState.createText();
    }

    @TestOnly
    public boolean addStdLibSources(boolean toModuleSources) {
        final File unpackedRuntimePath = CompileEnvironment.getUnpackedRuntimePath();
        if (unpackedRuntimePath != null) {
            addSources(new File(unpackedRuntimePath, "../../../stdlib/ktSrc").getAbsoluteFile(), !toModuleSources);
        }
        else {
            final File runtimeJarPath = CompileEnvironment.getRuntimeJarPath();
            if (runtimeJarPath != null && runtimeJarPath.exists()) {
                VirtualFile runtimeJar = myEnvironment.getLocalFileSystem().findFileByPath(runtimeJarPath.getAbsolutePath());
                VirtualFile jarRoot = myEnvironment.getJarFileSystem().findFileByPath(runtimeJar.getPath() + "!/stdlib/ktSrc");
                addSources(jarRoot, !toModuleSources);
            }
            else {
                return false;
            }
        }
        return true;
    }
}
