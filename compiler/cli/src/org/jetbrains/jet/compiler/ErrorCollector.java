package org.jetbrains.jet.compiler;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithTextRange;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.io.PrintStream;
import java.util.Collection;

/**
* @author alex.tkachman
*/
class ErrorCollector {
    Multimap<PsiFile,DiagnosticWithTextRange> maps = LinkedHashMultimap.<PsiFile, DiagnosticWithTextRange>create();

    boolean hasErrors;

    public ErrorCollector(BindingContext bindingContext) {
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            report(diagnostic);
        }
    }

    private void report(Diagnostic diagnostic) {
        hasErrors |= diagnostic.getSeverity() == Severity.ERROR;
        if(diagnostic instanceof DiagnosticWithTextRange) {
            DiagnosticWithTextRange diagnosticWithTextRange = (DiagnosticWithTextRange) diagnostic;
            maps.put(diagnosticWithTextRange.getPsiFile(), diagnosticWithTextRange);
        }
        else {
            System.out.println(diagnostic.getSeverity().toString() + ": " + diagnostic.getMessage());
        }
    }

    void report(final PrintStream out) {
        if(!maps.isEmpty()) {
            for (PsiFile psiFile : maps.keySet()) {
                out.println(psiFile.getVirtualFile().getPath());
                Collection<DiagnosticWithTextRange> diagnosticWithTextRanges = maps.get(psiFile);
                for (DiagnosticWithTextRange diagnosticWithTextRange : diagnosticWithTextRanges) {
                    String position = DiagnosticUtils.formatPosition(diagnosticWithTextRange);
                    out.println("\t" + diagnosticWithTextRange.getSeverity().toString() + ": " + position + " " + diagnosticWithTextRange.getMessage());
                }
            }
        }
    }

}
