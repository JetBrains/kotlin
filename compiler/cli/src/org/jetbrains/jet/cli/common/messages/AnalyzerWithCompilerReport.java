/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiFormatUtil;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.resolver.TraceBasedErrorReporter;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;

import java.util.Collection;
import java.util.List;

import static com.intellij.openapi.util.io.FileUtil.toSystemDependentName;
import static org.jetbrains.jet.lang.diagnostics.DiagnosticUtils.sortedDiagnostics;

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
    private static final DiagnosticFactory0<PsiErrorElement> SYNTAX_ERROR_FACTORY = DiagnosticFactory0.create(Severity.ERROR);

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
        String render;
        if (diagnostic instanceof MyDiagnostic) {
            render = ((MyDiagnostic)diagnostic).message;
        }
        else {
            render = DefaultErrorMessages.RENDERER.render(diagnostic);
        }
        messageCollector.report(convertSeverity(diagnostic.getSeverity()), render,
                MessageUtil.psiFileToMessageLocation(diagnostic.getPsiFile(), null, lineAndColumn.getLine(), lineAndColumn.getColumn()));
        return diagnostic.getSeverity() == Severity.ERROR;
    }

    private void reportIncompleteHierarchies() {
        assert analyzeExhaust != null;
        Collection<ClassDescriptor> incompletes = analyzeExhaust.getBindingContext().getKeys(BindingContext.INCOMPLETE_HIERARCHY);
        if (!incompletes.isEmpty()) {
            StringBuilder message = new StringBuilder("The following classes have incomplete hierarchies:\n");
            for (ClassDescriptor incomplete : incompletes) {
                String fqName = DescriptorUtils.getFqName(incomplete).asString();
                message.append("    ").append(fqName).append("\n");
            }
            messageCollectorWrapper.report(CompilerMessageSeverity.ERROR, message.toString(), CompilerMessageLocation.NO_LOCATION);
        }
    }

    private void reportAlternativeSignatureErrors() {
        assert analyzeExhaust != null;
        BindingContext bc = analyzeExhaust.getBindingContext();
        Collection<DeclarationDescriptor> descriptorsWithErrors = bc.getKeys(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS);
        if (!descriptorsWithErrors.isEmpty()) {
            StringBuilder message = new StringBuilder("The following Java entities have annotations with wrong Kotlin signatures:\n");
            for (DeclarationDescriptor descriptor : descriptorsWithErrors) {
                PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bc, descriptor);
                assert declaration instanceof PsiModifierListOwner;

                List<String> errors = bc.get(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor);
                assert errors != null && !errors.isEmpty();

                String externalName = PsiFormatUtil.getExternalName((PsiModifierListOwner) declaration);
                message.append(externalName).append(":\n");

                for (String error : errors) {
                    message.append("    ").append(error).append("\n");
                }
            }
            messageCollectorWrapper.report(CompilerMessageSeverity.ERROR,
                                           message.toString(), CompilerMessageLocation.NO_LOCATION);
        }
    }

    private void reportAbiVersionErrors() {
        assert analyzeExhaust != null;
        BindingContext bindingContext = analyzeExhaust.getBindingContext();

        Collection<VirtualFileKotlinClass> errorClasses = bindingContext.getKeys(TraceBasedErrorReporter.ABI_VERSION_ERRORS);
        for (VirtualFileKotlinClass kotlinClass : errorClasses) {
            Integer abiVersion = bindingContext.get(TraceBasedErrorReporter.ABI_VERSION_ERRORS, kotlinClass);
            String path = toSystemDependentName(kotlinClass.getFile().getPath());
            messageCollectorWrapper.report(CompilerMessageSeverity.ERROR,
                                           "Class '" + kotlinClass.getClassName() +
                                           "' was compiled with an incompatible version of Kotlin. " +
                                           "Its ABI version is " + abiVersion + ", expected ABI version is " + JvmAbi.VERSION,
                                           CompilerMessageLocation.create(path, 0, 0));
        }
    }

    public static boolean reportDiagnostics(@NotNull BindingContext bindingContext, @NotNull MessageCollector messageCollector) {
        boolean hasErrors = false;
        for (Diagnostic diagnostic : sortedDiagnostics(bindingContext.getDiagnostics().all())) {
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

            private <E extends PsiElement> void reportDiagnostic(E element, DiagnosticFactory0<E> factory, String message) {
                MyDiagnostic<?> diagnostic = new MyDiagnostic<E>(element, factory, message);
                AnalyzerWithCompilerReport.reportDiagnostic(diagnostic, messageCollector);
                if (element.getTextRange().getStartOffset() == file.getTextRange().getEndOffset()) {
                    onlyErrorAtEof = !hasErrors;
                }
                hasErrors = true;
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
        reportAlternativeSignatureErrors();
        reportAbiVersionErrors();
    }


    private static class MyDiagnostic<E extends PsiElement> extends SimpleDiagnostic<E> {
        private String message;

        public MyDiagnostic(@NotNull E psiElement, @NotNull DiagnosticFactory0<E> factory, String message) {
            super(psiElement, factory, Severity.ERROR);
            this.message = message;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
