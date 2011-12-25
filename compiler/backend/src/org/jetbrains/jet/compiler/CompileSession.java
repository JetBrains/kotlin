package org.jetbrains.jet.compiler;

import com.google.common.base.Predicates;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
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
    private final List<JetNamespace> mySourceFileNamespaces = new ArrayList<JetNamespace>();
    private final List<JetNamespace> myLibrarySourceFileNamespaces = new ArrayList<JetNamespace>();
    private List<String> myErrors = new ArrayList<String>();
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

        addSources(new File(path));
    }

    private void addSources(File file) {
        if(file.isDirectory()) {
            for (File child : file.listFiles()) {
                addSources(child);
            }
        }
        else {
            VirtualFile fileByPath = myEnvironment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
            PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(fileByPath);
            if(psiFile instanceof JetFile) {
                mySourceFileNamespaces.add(((JetFile) psiFile).getRootNamespace());
            }
        }
    }

    public void addSources(VirtualFile vFile) {
        if  (vFile.isDirectory())  {
            for (VirtualFile virtualFile : vFile.getChildren()) {
                addSources(virtualFile);
            }
        }
        else {
            if (vFile.getFileType() == JetFileType.INSTANCE) {
                PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(vFile);
                if (psiFile instanceof JetFile) {
                    mySourceFileNamespaces.add(((JetFile) psiFile).getRootNamespace());
                }
            }
        }
    }

    public List<JetNamespace> getSourceFileNamespaces() {
        return mySourceFileNamespaces;
    }

    public boolean analyze(final PrintStream out) {
        if (!myErrors.isEmpty()) {
            for (String error : myErrors) {
                out.println(error);
            }
            return false;
        }
        List<JetNamespace> allNamespaces = new ArrayList<JetNamespace>(mySourceFileNamespaces);
        allNamespaces.addAll(myLibrarySourceFileNamespaces);
        myBindingContext = AnalyzerFacade.analyzeNamespacesWithJavaIntegration(
                myEnvironment.getProject(), allNamespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);
        ErrorCollector errorCollector = new ErrorCollector(myBindingContext);
        errorCollector.report(out);
        return !errorCollector.hasErrors;
    }

    public ClassFileFactory generate() {
        GenerationState generationState = new GenerationState(myEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        generationState.compileCorrectNamespaces(myBindingContext, mySourceFileNamespaces);
        return generationState.getFactory();
    }

    public String generateText() {
        GenerationState generationState = new GenerationState(myEnvironment.getProject(), ClassBuilderFactory.TEXT);
        generationState.compileCorrectNamespaces(myBindingContext, mySourceFileNamespaces);
        return generationState.createText();
    }

    public boolean addStdLibSources() {
        final File unpackedRuntimePath = CompileEnvironment.getUnpackedRuntimePath();
        if (unpackedRuntimePath != null) {
            addSources(new File(unpackedRuntimePath, "../../../stdlib/ktSrc").getAbsoluteFile());
        }
        else {
            final File runtimeJarPath = CompileEnvironment.getRuntimeJarPath();
            if (runtimeJarPath != null && runtimeJarPath.exists()) {
                VirtualFile runtimeJar = myEnvironment.getLocalFileSystem().findFileByPath(runtimeJarPath.getAbsolutePath());
                VirtualFile jarRoot = myEnvironment.getJarFileSystem().findFileByPath(runtimeJar.getPath() + "!/stdlib/ktSrc");
                addSources(jarRoot);
            }
            else {
                return false;
            }
        }
        return true;
    }
}
