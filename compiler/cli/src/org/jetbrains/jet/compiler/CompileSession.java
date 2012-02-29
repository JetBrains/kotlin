/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The session which handles analyzing and compiling a single module.
 *
 * @author yole
 */
public class CompileSession {
    private final JetCoreEnvironment myEnvironment;
    private final List<JetFile> mySourceFiles = new ArrayList<JetFile>();
    private final FileNameTransformer myFileNameTransformer;
    private List<String> myErrors = new ArrayList<String>();
    private boolean stubs = false;

    public BindingContext getMyBindingContext() {
        return myBindingContext;
    }

    private BindingContext myBindingContext;

    public CompileSession(JetCoreEnvironment environment) {
        this(environment, FileNameTransformer.IDENTITY);
    }

    public CompileSession(JetCoreEnvironment environment, FileNameTransformer fileNameTransformer) {
        myEnvironment = environment;
        myFileNameTransformer = fileNameTransformer;
    }

    public void setStubs(boolean stubs) {
        this.stubs = stubs;
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
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    addSources(child);
                }
            }
        }
        else {
            VirtualFile fileByPath = myEnvironment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
            if (fileByPath != null) {
                PsiFile psiFile = PsiManager.getInstance(myEnvironment.getProject()).findFile(fileByPath);
                if(psiFile instanceof JetFile) {
                    mySourceFiles.add((JetFile) psiFile);
                }
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
                    mySourceFiles.add((JetFile) psiFile);
                }
            }
        }
    }

    public List<JetFile> getSourceFileNamespaces() {
        return mySourceFiles;
    }

    public boolean analyze(@NotNull PrintStream out, @NotNull MessageRenderer renderer) {
        MessageCollector collector = new MessageCollector(renderer);

        for (String error : myErrors) {
            collector.report(Severity.ERROR, error, null, -1, -1);
        }

        reportSyntaxErrors(collector);
        analyzeAndReportSemanticErrors(collector);

        collector.printTo(out);

        return !collector.hasErrors();
    }

    /**
     * @see JetTypeMapper#getFQName(DeclarationDescriptor)
     * TODO possibly duplicates DescriptorUtils#getFQName(DeclarationDescriptor)
     */
    private static String fqName(ClassOrNamespaceDescriptor descriptor) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration == null || containingDeclaration instanceof ModuleDescriptor || containingDeclaration.getName().equals(JavaDescriptorResolver.JAVA_ROOT)) {
            return descriptor.getName();
        } else {
            return fqName((ClassOrNamespaceDescriptor) containingDeclaration) + "." + descriptor.getName();
        }
    }

    private void analyzeAndReportSemanticErrors(MessageCollector collector) {
        Predicate<PsiFile> filesToAnalyzeCompletely =
                stubs ? Predicates.<PsiFile>alwaysFalse() : Predicates.<PsiFile>alwaysTrue();
        myBindingContext = AnalyzerFacade.analyzeFilesWithJavaIntegration(
                myEnvironment.getProject(), mySourceFiles, filesToAnalyzeCompletely, JetControlFlowDataTraceFactory.EMPTY);

        for (Diagnostic diagnostic : myBindingContext.getDiagnostics()) {
            reportDiagnostic(collector, diagnostic);
        }

        reportIncompleteHierarchies(collector);
    }

    private void reportIncompleteHierarchies(MessageCollector collector) {
        Collection<ClassDescriptor> incompletes = myBindingContext.getKeys(BindingContext.INCOMPLETE_HIERARCHY);
        if (!incompletes.isEmpty()) {
            StringBuilder message = new StringBuilder("The following classes have incomplete hierarchies:\n");
            for (ClassDescriptor incomplete : incompletes) {
                message.append("    ").append(fqName(incomplete)).append("\n");
            }
            collector.report(Severity.ERROR, message.toString(), null, -1, -1);
        }
    }

    private void reportSyntaxErrors(final MessageCollector messageCollector) {
        for (JetFile file : mySourceFiles) {
            file.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitErrorElement(PsiErrorElement element) {
                    String description = element.getErrorDescription();
                    String message = StringUtil.isEmpty(description) ? "Syntax error" : description;
                    Diagnostic diagnostic = DiagnosticFactory.create(Severity.ERROR, message).on(element);
                    reportDiagnostic(messageCollector, diagnostic);
                }
            });
        }
    }

    private static void reportDiagnostic(MessageCollector collector, Diagnostic diagnostic) {
        DiagnosticUtils.LineAndColumn lineAndColumn = DiagnosticUtils.getLineAndColumn(diagnostic);
        VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
        String path = virtualFile == null ? null : virtualFile.getPath();
        collector.report(diagnostic.getSeverity(), diagnostic.getMessage(), path, lineAndColumn.getLine(), lineAndColumn.getColumn());
    }

    @NotNull
    public ClassFileFactory generate() {
        Project project = myEnvironment.getProject();
        GenerationState generationState = new GenerationState(project, ClassBuilderFactories.binaries(stubs), myFileNameTransformer);
        generationState.compileCorrectFiles(myBindingContext, mySourceFiles, CompilationErrorHandler.THROW_EXCEPTION, true);
        ClassFileFactory answer = generationState.getFactory();

        List<CompilerPlugin> fileProcessors = myEnvironment.getCompilerPlugins();
        if (fileProcessors != null) {
            for (CompilerPlugin processor : fileProcessors) {
                processor.processFiles(myBindingContext, getSourceFileNamespaces());
            }
        }
        return answer;
    }
}
