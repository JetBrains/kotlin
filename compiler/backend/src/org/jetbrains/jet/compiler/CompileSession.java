package org.jetbrains.jet.compiler;

import com.google.common.base.Predicates;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The session which handles analyzing and compiling a single module.
 *
 * @author yole
 */
public class CompileSession {
    private final List<JetNamespace> mySourceFileNamespaces = new ArrayList<JetNamespace>();
    private final List<JetNamespace> myLibrarySourceFileNamespaces = new ArrayList<JetNamespace>();
    private List<String> myErrors = new ArrayList<String>();
    private BindingContext myBindingContext;
    private AbstractCompileEnvironment myEnvironment;

    public CompileSession(AbstractCompileEnvironment environment) {
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

        addSources(new File(path), mySourceFileNamespaces);
    }

    private void addSources(File file, List<JetNamespace> namespaces) {
        if(file.isDirectory()) {
            for (File child : file.listFiles()) {
                addSources(child, namespaces);
            }
        }
        else {
            VirtualFile fileByPath = myEnvironment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
            PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(fileByPath);
            if(psiFile instanceof JetFile) {
                namespaces.add(((JetFile) psiFile).getRootNamespace());
            }
        }
    }

    public void addSources(VirtualFile vFile) {
        addSources(vFile, mySourceFileNamespaces);
    }
    
    public void addSources(VirtualFile vFile, List<JetNamespace> namespaces) {
        if  (vFile.isDirectory())  {
            for (VirtualFile virtualFile : vFile.getChildren()) {
                addSources(virtualFile, namespaces);
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

    public void addSources(VirtualFile[] allFiles) {
        for(VirtualFile vFile: allFiles) {
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

    public BindingContext analyze() {
        List<JetNamespace> allNamespaces = new ArrayList<JetNamespace>(mySourceFileNamespaces);
        allNamespaces.addAll(myLibrarySourceFileNamespaces);
        return AnalyzerFacade.analyzeNamespacesWithJavaIntegration(
                myEnvironment.getProject(), allNamespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);
    }

    public boolean analyze(final PrintStream out) {
        if (!myErrors.isEmpty()) {
            for (String error : myErrors) {
                out.println(error);
            }
            return false;
        }
        myBindingContext = analyze();
        ErrorCollector errorCollector = new ErrorCollector(myBindingContext);
        errorCollector.report(out);
        return !errorCollector.hasErrors;
    }

    public ClassFileFactory generate() {
        return generate(CompilationErrorHandler.THROW_EXCEPTION);
    }

    public ClassFileFactory generate(CompilationErrorHandler handler) {
        GenerationState generationState = new GenerationState(myEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        generationState.compileCorrectNamespaces(myBindingContext, mySourceFileNamespaces, handler);
        return generationState.getFactory();
    }

    public String generateText() {
        GenerationState generationState = new GenerationState(myEnvironment.getProject(), ClassBuilderFactory.TEXT);
        generationState.compileCorrectNamespaces(myBindingContext, mySourceFileNamespaces);
        return generationState.createText();
    }

    public boolean addStdLibSources() {
        return addStdLibSources(false);
    }

    public boolean addStdLibSources(boolean forAnalize) {
        /*
        @todo
        We add sources as library sources in case of jar
        but as regular one if there is no jar
         */
        final File unpackedRuntimePath = CoreCompileEnvironment.getUnpackedRuntimePath();
        if (unpackedRuntimePath != null) {
            addSources(new File(unpackedRuntimePath, "stdlib/ktSrc").getAbsoluteFile(), forAnalize ? myLibrarySourceFileNamespaces : mySourceFileNamespaces);
        }
        else {
            final File runtimeJarPath = CoreCompileEnvironment.getRuntimeJarPath();
            if (runtimeJarPath != null && runtimeJarPath.exists()) {
                VirtualFile runtimeJar = myEnvironment.getLocalFileSystem().findFileByPath(runtimeJarPath.getAbsolutePath());
                VirtualFile jarRoot = myEnvironment.getJarFileSystem().findFileByPath(runtimeJar.getPath() + "!/stdlib/ktSrc");
                addSources(jarRoot, myLibrarySourceFileNamespaces);
            }
            else {
                return false;
            }
        }
        return true;
    }

    public List<JetNamespace> getLibrarySourceFileNamespaces() {
        return myLibrarySourceFileNamespaces;
    }
}
