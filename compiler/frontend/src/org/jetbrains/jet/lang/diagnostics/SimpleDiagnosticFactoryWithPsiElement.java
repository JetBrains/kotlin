package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public abstract class SimpleDiagnosticFactoryWithPsiElement<T extends PsiElement>  extends DiagnosticFactoryWithSeverity {

        protected final String message;

        protected SimpleDiagnosticFactoryWithPsiElement(Severity severity, String message) {
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