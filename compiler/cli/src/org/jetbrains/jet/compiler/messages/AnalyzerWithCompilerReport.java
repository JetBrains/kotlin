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

package org.jetbrains.jet.compiler.messages;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

import java.util.Collection;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerWithCompilerReport {

    @NotNull
    private static CompilerMessageSeverity convertSeverity(@NotNull Severity severity) {
        switch (severity) {
            case INFO:
                return CompilerMessageSeverity.INFO;
            case ERROR:
                return CompilerMessageSeverity.ERROR;
            case WARNING:
                return CompilerMessageSeverity.WARNING;
        }
        throw new IllegalStateException("Unknown severity: " + severity);
    }

    @NotNull
    private static final SimpleDiagnosticFactory<PsiErrorElement> SYNTAX_ERROR_FACTORY = SimpleDiagnosticFactory.create(Severity.ERROR);

    private boolean hasErrors;
    @NotNull
    private final MessageCollector messageCollectorWrapper;
    @Nullable
    private AnalyzeExhaust analyzeExhaust = null;

    public AnalyzerWithCompilerReport(@NotNull final MessageCollector collector) {
        messageCollectorWrapper = new MessageCollector() {
            @Override
            public void report(@NotNull CompilerMessageSeverity severity,
                    @NotNull String message,
                    @NotNull CompilerMessageLocation location) {
                if (CompilerMessageSeverity.ERRORS.contains(severity)) {
                    hasErrors = true;
                }
                collector.report(severity, message, location);
            }
        };
    }

    private void reportDiagnostic(@NotNull Diagnostic diagnostic) {
        DiagnosticUtils.LineAndColumn lineAndColumn = DiagnosticUtils.getLineAndColumn(diagnostic);
        VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
        String path = virtualFile == null ? null : virtualFile.getPath();
        String render;
        if (diagnostic.getFactory() == SYNTAX_ERROR_FACTORY) {
            render = ((SyntaxErrorDiagnostic)diagnostic).message;
        }
        else {
            render = DefaultErrorMessages.RENDERER.render(diagnostic);
        }
        messageCollectorWrapper.report(convertSeverity(diagnostic.getSeverity()), render,
                                       CompilerMessageLocation.create(path, lineAndColumn.getLine(), lineAndColumn.getColumn()));
    }

    private void reportIncompleteHierarchies() {
        assert analyzeExhaust != null;
        Collection<ClassDescriptor> incompletes = analyzeExhaust.getBindingContext().getKeys(BindingContext.INCOMPLETE_HIERARCHY);
        if (!incompletes.isEmpty()) {
            StringBuilder message = new StringBuilder("The following classes have incomplete hierarchies:\n");
            for (ClassDescriptor incomplete : incompletes) {
                String fqName = DescriptorUtils.getFQName(incomplete).getFqName();
                message.append("    ").append(fqName).append("\n");
            }
            messageCollectorWrapper.report(CompilerMessageSeverity.ERROR, message.toString(), CompilerMessageLocation.NO_LOCATION);
        }
    }

    private void reportDiagnostics() {
        assert analyzeExhaust != null;
        for (Diagnostic diagnostic : analyzeExhaust.getBindingContext().getDiagnostics()) {
            reportDiagnostic(diagnostic);
        }
    }

    private void reportSyntaxErrors(@NotNull Collection<JetFile> files) {
        for (JetFile file : files) {
            file.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitErrorElement(PsiErrorElement element) {
                    String description = element.getErrorDescription();
                    String message = StringUtil.isEmpty(description) ? "Syntax error" : description;
                    Diagnostic diagnostic = new SyntaxErrorDiagnostic(element, Severity.ERROR, message);
                    reportDiagnostic(diagnostic);
                }
            });
        }
    }

    @Nullable
    public AnalyzeExhaust getAnalyzeExhaust() {
        return analyzeExhaust;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void analyzeAndReport(@NotNull AnalyzerWrapper analyzerWrapper, @NotNull Collection<JetFile> files) {
        reportSyntaxErrors(files);
        analyzeExhaust = analyzerWrapper.analyze();
        reportDiagnostics();
        reportIncompleteHierarchies();
    }

    public interface AnalyzerWrapper {
        @NotNull
        AnalyzeExhaust analyze();
    }

    public static class SyntaxErrorDiagnostic extends SimpleDiagnostic<PsiErrorElement> {
        private String message;

        public SyntaxErrorDiagnostic(@NotNull PsiErrorElement psiElement, @NotNull Severity severity, String message) {
            super(psiElement, SYNTAX_ERROR_FACTORY, severity);
            this.message = message;
        }
    }
}
