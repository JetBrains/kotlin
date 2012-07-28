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

package org.jetbrains.jet.cli.common.messages;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetIdeTemplate;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
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
    @NotNull
    private static final SimpleDiagnosticFactory<JetIdeTemplate> UNRESOLVED_IDE_TEMPLATE_ERROR_FACTORY
            = SimpleDiagnosticFactory.create(Severity.ERROR);

    private boolean hasErrors = false;
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

    private static boolean reportDiagnostic(@NotNull Diagnostic diagnostic, @NotNull MessageCollector messageCollector) {
        if (!diagnostic.isValid()) return false;
        DiagnosticUtils.LineAndColumn lineAndColumn = DiagnosticUtils.getLineAndColumn(diagnostic);
        VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
        String path = virtualFile == null ? null : virtualFile.getPath();
        String render;
        if (diagnostic instanceof MyDiagnostic) {
            render = ((MyDiagnostic)diagnostic).message;
        }
        else {
            render = DefaultErrorMessages.RENDERER.render(diagnostic);
        }
        messageCollector.report(convertSeverity(diagnostic.getSeverity()), render,
                CompilerMessageLocation.create(path, lineAndColumn.getLine(), lineAndColumn.getColumn()));
        return diagnostic.getSeverity() == Severity.ERROR;
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

    public static boolean reportDiagnostics(@NotNull BindingContext bindingContext, @NotNull MessageCollector messageCollector) {
        boolean hasErrors = false;
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            hasErrors |= reportDiagnostic(diagnostic, messageCollector);
        }
        return hasErrors;
    }

    private void reportSyntaxErrors(@NotNull Collection<JetFile> files) {
        for (JetFile file : files) {
            reportSyntaxErrors(file, messageCollectorWrapper);
        }
    }

    public static class SyntaxErrorReport {
        private final boolean hasErrors;
        private final boolean onlyErrorAtEof;

        public SyntaxErrorReport(boolean hasErrors, boolean onlyErrorAtEof) {
            this.hasErrors = hasErrors;
            this.onlyErrorAtEof = onlyErrorAtEof;
        }

        public boolean isHasErrors() {
            return hasErrors;
        }

        public boolean isOnlyErrorAtEof() {
            return onlyErrorAtEof;
        }
    }

    public static SyntaxErrorReport reportSyntaxErrors(@NotNull final PsiElement file, @NotNull final MessageCollector messageCollector) {
        class ErrorReportingVisitor extends AnalyzingUtils.PsiErrorElementVisitor {
            boolean hasErrors = false;
            boolean onlyErrorAtEof = false;

            private <E extends PsiElement> void reportDiagnostic(E element, SimpleDiagnosticFactory<E> factory, String message) {
                MyDiagnostic<?> diagnostic = new MyDiagnostic<E>(element, factory, message);
                AnalyzerWithCompilerReport.reportDiagnostic(diagnostic, messageCollector);
                if (element.getTextRange().getStartOffset() == file.getTextRange().getEndOffset()) {
                    onlyErrorAtEof = !hasErrors;
                }
                hasErrors = true;
            }

            @Override
            public void visitIdeTemplate(JetIdeTemplate expression) {
                String placeholderText = expression.getPlaceholderText();
                reportDiagnostic(expression, UNRESOLVED_IDE_TEMPLATE_ERROR_FACTORY,
                                 "Unresolved IDE template" + (StringUtil.isEmpty(placeholderText) ? "" : ": " + placeholderText));
            }

            @Override
            public void visitErrorElement(PsiErrorElement element) {
                String description = element.getErrorDescription();
                reportDiagnostic(element, SYNTAX_ERROR_FACTORY, StringUtil.isEmpty(description) ? "Syntax error" : description);
            }
        }
        ErrorReportingVisitor visitor = new ErrorReportingVisitor();

        file.accept(visitor);

        return new SyntaxErrorReport(visitor.hasErrors, visitor.onlyErrorAtEof);
    }

    @Nullable
    public AnalyzeExhaust getAnalyzeExhaust() {
        return analyzeExhaust;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void analyzeAndReport(@NotNull Function0<AnalyzeExhaust> analyzer, @NotNull Collection<JetFile> files) {
        reportSyntaxErrors(files);
        analyzeExhaust = analyzer.invoke();
        reportDiagnostics(analyzeExhaust.getBindingContext(), messageCollectorWrapper);
        reportIncompleteHierarchies();
    }


    private static class MyDiagnostic<E extends PsiElement> extends SimpleDiagnostic<E> {
        private String message;

        public MyDiagnostic(@NotNull E psiElement, @NotNull SimpleDiagnosticFactory<E> factory, String message) {
            super(psiElement, factory, Severity.ERROR);
            this.message = message;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
