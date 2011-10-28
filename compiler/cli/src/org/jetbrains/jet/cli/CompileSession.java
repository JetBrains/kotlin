package org.jetbrains.jet.cli;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.ArrayList;
import java.util.List;

/**
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
        VirtualFile vFile = myEnvironment.getLocalFileSystem().findFileByPath(path);
        if (vFile == null) {
            myErrors.add("File/directory not found: " + path);
            return;
        }
        if (!vFile.isDirectory() && vFile.getFileType() != JetFileType.INSTANCE) {
            myErrors.add("Not a Kotlin file: " + path);
            return;
        }

        addSources(vFile);
    }

    public void addSources(VirtualFile vFile) {
        if  (vFile.isDirectory())  {
            for (VirtualFile virtualFile : vFile.getChildren()) {
                if (virtualFile.getFileType() == JetFileType.INSTANCE) {
                    addSources(virtualFile);
                }
            }
        }
        else {
            PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(vFile);
            if (psiFile instanceof JetFile) {
                mySourceFileNamespaces.add(((JetFile) psiFile).getRootNamespace());
            }
        }
    }

    public void addLibrarySources(VirtualFile vFile) {
        PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(vFile);
        if (psiFile instanceof JetFile) {
            myLibrarySourceFileNamespaces.add(((JetFile) psiFile).getRootNamespace());
        }
    }

    public List<JetNamespace> getSourceFileNamespaces() {
        return mySourceFileNamespaces;
    }

    public boolean analyze() {
        if (!myErrors.isEmpty()) {
            for (String error : myErrors) {
                System.out.println(error);
            }
            return false;
        }
        final AnalyzingUtils instance = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS);
        List<JetNamespace> allNamespaces = new ArrayList<JetNamespace>(mySourceFileNamespaces);
        allNamespaces.addAll(myLibrarySourceFileNamespaces);
        myBindingContext = instance.analyzeNamespaces(myEnvironment.getProject(), allNamespaces, JetControlFlowDataTraceFactory.EMPTY);
        ErrorCollector errorCollector = new ErrorCollector(myBindingContext);
        errorCollector.report();
        return !errorCollector.hasErrors;
    }

    public ClassFileFactory generate() {
        GenerationState generationState = new GenerationState(myEnvironment.getProject(), false);
        generationState.compileCorrectNamespaces(myBindingContext, mySourceFileNamespaces);
        return generationState.getFactory();
    }

}
