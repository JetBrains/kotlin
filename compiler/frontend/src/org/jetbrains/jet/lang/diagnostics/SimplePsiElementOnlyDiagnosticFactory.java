package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class SimplePsiElementOnlyDiagnosticFactory<T extends PsiElement>  extends DiagnosticFactoryWithSeverity implements PsiElementOnlyDiagnosticFactory<T> {

        public static <T extends PsiElement> SimplePsiElementOnlyDiagnosticFactory<T> create(Severity severity, String message) {
            return new SimplePsiElementOnlyDiagnosticFactory<T>(severity, message);
        }

        protected final String message;

        public SimplePsiElementOnlyDiagnosticFactory(Severity severity, String message) {
            super(severity);
            this.message = message;
        }

        @NotNull
        public Diagnostic on(@NotNull T element, @NotNull ASTNode node) {
            return new DiagnosticWithPsiElement<T>(this, severity, message, element, node.getTextRange());
        }

        @NotNull
        public Diagnostic on(@NotNull T element) {
            return on(element, element.getNode());
        }
}
