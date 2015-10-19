/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common.messages;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiFormatUtil;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.load.java.JavaBindingContext;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter;
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter.AbiVersionErrorData;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.utils.StringsKt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.openapi.util.io.FileUtil.toSystemDependentName;
import static org.jetbrains.kotlin.diagnostics.DiagnosticUtils.sortedDiagnostics;

public final class AnalyzerWithCompilerReport {

    @NotNull
    public static CompilerMessageSeverity convertSeverity(@NotNull Severity severity) {
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

    private static final DiagnosticFactory0<PsiErrorElement> SYNTAX_ERROR_FACTORY = DiagnosticFactory0.create(Severity.ERROR);

    private final MessageSeverityCollector messageCollector;
    private AnalysisResult analysisResult = null;

    public AnalyzerWithCompilerReport(@NotNull MessageCollector collector) {
        messageCollector = new MessageSeverityCollector(collector);
    }

    private static boolean reportDiagnostic(
            @NotNull Diagnostic diagnostic,
            @NotNull DiagnosticMessageReporter reporter,
            boolean incompatibleFilesFound
    ) {
        if (!diagnostic.isValid()) return false;

        String render;
        if (diagnostic instanceof MyDiagnostic) {
            render = ((MyDiagnostic) diagnostic).message;
        }
        else {
            render = DefaultErrorMessages.render(diagnostic);
        }

        if (incompatibleFilesFound && Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
            render += "\n(note: this may be caused by the fact that some classes compiled with an incompatible version of Kotlin " +
                      "were found in the classpath. Such classes cannot be loaded properly by this version of Kotlin compiler. " +
                      "See below for more information)";
        }

        PsiFile file = diagnostic.getPsiFile();
        reporter.report(diagnostic, file, render);

        return diagnostic.getSeverity() == Severity.ERROR;
    }

    private void reportIncompleteHierarchies() {
        assert analysisResult != null;
        BindingContext bindingContext = analysisResult.getBindingContext();
        Collection<ClassDescriptor> classes = bindingContext.getKeys(TraceBasedErrorReporter.INCOMPLETE_HIERARCHY);
        if (!classes.isEmpty()) {
            StringBuilder message = new StringBuilder("Supertypes of the following classes cannot be resolved. " +
                                                      "Please make sure you have the required dependencies in the classpath:\n");
            for (ClassDescriptor descriptor : classes) {
                String fqName = DescriptorUtils.getFqName(descriptor).asString();
                List<String> unresolved = bindingContext.get(TraceBasedErrorReporter.INCOMPLETE_HIERARCHY, descriptor);
                assert unresolved != null && !unresolved.isEmpty() :
                        "Incomplete hierarchy should be reported with names of unresolved superclasses: " + fqName;
                message.append("    class ").append(fqName)
                        .append(", unresolved supertypes: ").append(StringsKt.join(unresolved, ", "))
                        .append("\n");
            }
            messageCollector.report(CompilerMessageSeverity.ERROR, message.toString(), CompilerMessageLocation.NO_LOCATION);
        }
    }

    private void reportAlternativeSignatureErrors() {
        assert analysisResult != null;
        BindingContext bc = analysisResult.getBindingContext();
        Collection<DeclarationDescriptor> descriptorsWithErrors = bc.getKeys(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS);
        if (!descriptorsWithErrors.isEmpty()) {
            StringBuilder message = new StringBuilder("The following Java entities have annotations with wrong Kotlin signatures:\n");
            for (DeclarationDescriptor descriptor : descriptorsWithErrors) {
                PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
                assert declaration instanceof PsiModifierListOwner;

                List<String> errors = bc.get(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor);
                assert errors != null && !errors.isEmpty();

                String externalName = PsiFormatUtil.getExternalName((PsiModifierListOwner) declaration);
                message.append(externalName).append(":\n");

                for (String error : errors) {
                    message.append("    ").append(error).append("\n");
                }
            }
            messageCollector.report(CompilerMessageSeverity.ERROR, message.toString(), CompilerMessageLocation.NO_LOCATION);
        }
    }

    @NotNull
    private List<AbiVersionErrorData> getAbiVersionErrors() {
        assert analysisResult != null;
        BindingContext bindingContext = analysisResult.getBindingContext();

        Collection<String> errorClasses = bindingContext.getKeys(TraceBasedErrorReporter.ABI_VERSION_ERRORS);
        List<AbiVersionErrorData> result = new ArrayList<AbiVersionErrorData>(errorClasses.size());
        for (String kotlinClass : errorClasses) {
            result.add(bindingContext.get(TraceBasedErrorReporter.ABI_VERSION_ERRORS, kotlinClass));
        }

        return result;
    }

    private void reportAbiVersionErrors(@NotNull List<AbiVersionErrorData> errors) {
        for (AbiVersionErrorData data : errors) {
            String path = toSystemDependentName(data.getFilePath());
            messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Class '" + JvmClassName.byClassId(data.getClassId()) + "' was compiled with an incompatible version of Kotlin. " +
                    "Its ABI version is " + data.getActualVersion() + ", expected ABI version is " + JvmAbi.VERSION,
                    CompilerMessageLocation.create(path, -1, -1, null)
            );
        }
    }

    public static boolean reportDiagnostics(
            @NotNull Diagnostics diagnostics,
            @NotNull DiagnosticMessageReporter reporter,
            boolean incompatibleFilesFound
    ) {
        boolean hasErrors = false;
        for (Diagnostic diagnostic : sortedDiagnostics(diagnostics.all())) {
            hasErrors |= reportDiagnostic(diagnostic, reporter, incompatibleFilesFound);
        }
        return hasErrors;
    }

    public static boolean reportDiagnostics(
            @NotNull Diagnostics diagnostics,
            @NotNull MessageCollector messageCollector,
            boolean incompatibleFilesFound
    ) {
        return reportDiagnostics(diagnostics, new DefaultDiagnosticReporter(messageCollector), incompatibleFilesFound);
    }

    public static boolean reportDiagnostics(@NotNull Diagnostics diagnostics, @NotNull MessageCollector messageCollector) {
        return reportDiagnostics(diagnostics, new DefaultDiagnosticReporter(messageCollector), false);
    }

    private void reportSyntaxErrors(@NotNull Collection<JetFile> files) {
        for (JetFile file : files) {
            reportSyntaxErrors(file, messageCollector);
        }
    }

    public static class SyntaxErrorReport {
        private final boolean hasErrors;
        private final boolean allErrorsAtEof;

        public SyntaxErrorReport(boolean hasErrors, boolean allErrorsAtEof) {
            this.hasErrors = hasErrors;
            this.allErrorsAtEof = allErrorsAtEof;
        }

        public boolean isHasErrors() {
            return hasErrors;
        }

        public boolean isAllErrorsAtEof() {
            return allErrorsAtEof;
        }
    }

    public static SyntaxErrorReport reportSyntaxErrors(
            @NotNull final PsiElement file,
            @NotNull final DiagnosticMessageReporter reporter
    ) {
        class ErrorReportingVisitor extends AnalyzingUtils.PsiErrorElementVisitor {
            boolean hasErrors = false;
            boolean allErrorsAtEof = true;

            private <E extends PsiElement> void reportDiagnostic(E element, DiagnosticFactory0<E> factory, String message) {
                MyDiagnostic<?> diagnostic = new MyDiagnostic<E>(element, factory, message);
                AnalyzerWithCompilerReport.reportDiagnostic(diagnostic, reporter, false);
                if (element.getTextRange().getStartOffset() != file.getTextRange().getEndOffset()) {
                    allErrorsAtEof = false;
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

        return new SyntaxErrorReport(visitor.hasErrors, visitor.allErrorsAtEof);
    }

    public static SyntaxErrorReport reportSyntaxErrors(@NotNull PsiElement file, @NotNull MessageCollector messageCollector) {
        return reportSyntaxErrors(file, new DefaultDiagnosticReporter(messageCollector));
    }

    @Nullable
    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public boolean hasErrors() {
        return messageCollector.anyReported(CompilerMessageSeverity.ERROR);
    }

    public void analyzeAndReport(@NotNull Collection<JetFile> files, @NotNull Function0<AnalysisResult> analyzer) {
        analysisResult = analyzer.invoke();
        reportSyntaxErrors(files);
        List<AbiVersionErrorData> abiVersionErrors = getAbiVersionErrors();
        reportDiagnostics(analysisResult.getBindingContext().getDiagnostics(), messageCollector, !abiVersionErrors.isEmpty());
        if (hasErrors()) {
            reportAbiVersionErrors(abiVersionErrors);
        }
        reportIncompleteHierarchies();
        reportAlternativeSignatureErrors();
    }

    private static class MyDiagnostic<E extends PsiElement> extends SimpleDiagnostic<E> {
        private final String message;

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
