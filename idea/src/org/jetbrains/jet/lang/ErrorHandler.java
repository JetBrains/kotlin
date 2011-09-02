package org.jetbrains.jet.lang;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ErrorHandler {
    public static final ErrorHandler DO_NOTHING = new ErrorHandler();
    public static final ErrorHandler THROW_EXCEPTION = new ErrorHandler() {
        @Override
        public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
            throw new IllegalStateException("Unresolved reference: " + referenceExpression.getText() +
                                            atLocation(referenceExpression));
        }

        @Override
        public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
            throw new IllegalStateException(errorMessage + " at " + node.getText());
        }

        @Override
        public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
            throw new IllegalStateException("Type mismatch " + atLocation(expression) + ": inferred type is " + actualType + " but " + expectedType + " was expected");
        }

        @Override
        public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
            throw new IllegalStateException("Redeclaration: " + existingDescriptor.getName());
        }
    };
    public static String atLocation(PsiElement element) {
        Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());
        int offset = element.getTextRange().getStartOffset();
        int lineNumber = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int column = offset - lineStartOffset;

        return "' at line " + (lineNumber+1) + ":" + column;
    }


    public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
    }

    public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
    }

    public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
    }

    public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
    }

    public void genericWarning(@NotNull ASTNode node, @NotNull String message) {
    }

}
