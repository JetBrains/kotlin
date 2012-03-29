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
import org.jetbrains.jet.lang.resolve.java.AnalyzeExhaust;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.utils.Progress;

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
    private final JetCoreEnvironment environment;
    private final MessageCollector messageCollector;
    private final List<JetFile> sourceFiles = new ArrayList<JetFile>();
    private List<String> errors = new ArrayList<String>();
    private boolean stubs = false;
    private final MessageRenderer messageRenderer;
    private final PrintStream errorStream;
    private final boolean isVerbose;
    private AnalyzeExhaust bindingContext;

    public CompileSession(JetCoreEnvironment environment, MessageRenderer messageRenderer, PrintStream errorStream, boolean verbose) {
        this.environment = environment;
        this.messageRenderer = messageRenderer;
        this.errorStream = errorStream;
        isVerbose = verbose;
        messageCollector = new MessageCollector(this.messageRenderer);
    }

    @NotNull
    public AnalyzeExhaust getBindingContext() {
        return bindingContext;
    }

    public void setStubs(boolean stubs) {
        this.stubs = stubs;
    }

    public void addSources(String path) {
        if(path == null)
            return;

        VirtualFile vFile = environment.getLocalFileSystem().findFileByPath(path);
        if (vFile == null) {
            errors.add("File/directory not found: " + path);
            return;
        }
        if (!vFile.isDirectory() && vFile.getFileType() != JetFileType.INSTANCE) {
            errors.add("Not a Kotlin file: " + path);
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
            VirtualFile fileByPath = environment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
            if (fileByPath != null) {
                PsiFile psiFile = PsiManager.getInstance(environment.getProject()).findFile(fileByPath);
                if(psiFile instanceof JetFile) {
                    sourceFiles.add((JetFile)psiFile);
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
                PsiFile psiFile = PsiManager.getInstance(environment.getProject()).findFile(vFile);
                if (psiFile instanceof JetFile) {
                    sourceFiles.add((JetFile)psiFile);
                }
            }
        }
    }

    public List<JetFile> getSourceFileNamespaces() {
        return sourceFiles;
    }

    public boolean analyze() {
        for (String error : errors) {
            messageCollector.report(Severity.ERROR, error, null, -1, -1);
        }

        reportSyntaxErrors();
        analyzeAndReportSemanticErrors();

        messageCollector.printTo(errorStream);

        return !messageCollector.hasErrors();
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

    private void analyzeAndReportSemanticErrors() {
        Predicate<PsiFile> filesToAnalyzeCompletely =
                stubs ? Predicates.<PsiFile>alwaysFalse() : Predicates.<PsiFile>alwaysTrue();
        bindingContext = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                environment.getProject(), sourceFiles, filesToAnalyzeCompletely, JetControlFlowDataTraceFactory.EMPTY);

        for (Diagnostic diagnostic : bindingContext.getBindingContext().getDiagnostics()) {
            reportDiagnostic(messageCollector, diagnostic);
        }

        reportIncompleteHierarchies(messageCollector);
    }

    private void reportIncompleteHierarchies(MessageCollector collector) {
        Collection<ClassDescriptor> incompletes = bindingContext.getBindingContext().getKeys(BindingContext.INCOMPLETE_HIERARCHY);
        if (!incompletes.isEmpty()) {
            StringBuilder message = new StringBuilder("The following classes have incomplete hierarchies:\n");
            for (ClassDescriptor incomplete : incompletes) {
                message.append("    ").append(fqName(incomplete)).append("\n");
            }
            collector.report(Severity.ERROR, message.toString(), null, -1, -1);
        }
    }

    private void reportSyntaxErrors() {
        for (JetFile file : sourceFiles) {
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
    public GenerationState generate(boolean module) {
        Project project = environment.getProject();
        GenerationState generationState = new GenerationState(project, ClassBuilderFactories.binaries(stubs),
                isVerbose ? new BackendProgress() : Progress.DEAF, bindingContext, sourceFiles);
        generationState.compileCorrectFiles(CompilationErrorHandler.THROW_EXCEPTION);

        List<CompilerPlugin> plugins = environment.getCompilerPlugins();
        if (!module) {
            if (plugins != null) {
                CompilerPluginContext context = new CompilerPluginContext(project, bindingContext.getBindingContext(), getSourceFileNamespaces());
                for (CompilerPlugin plugin : plugins) {
                    plugin.processFiles(context);
                }
            }
        }
        return generationState;
    }

    private class BackendProgress implements Progress {
        @Override
        public void log(String message) {
            errorStream.println(messageRenderer.render(Severity.LOGGING, message, null, -1, -1));
        }
    }
}
