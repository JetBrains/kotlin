package org.jetbrains.jet.lang.annotations;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.Type;

/**
 * @author abreslav
 */
public class JetPsiChecker implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        if (element instanceof JetFile) {
            Project project = element.getProject();

            JetFile file = (JetFile) element;
            try {
                final BindingContext bindingContext = AnalyzingUtils.analyzeFile(file, new ErrorHandler() {
                    @Override
                    public void unresolvedReference(JetReferenceExpression referenceExpression) {
                        holder.createErrorAnnotation(referenceExpression, "Unresolved");
                    }
                });
                file.getRootNamespace().accept(new JetVisitor() {
                    @Override
                    public void visitClass(JetClass klass) {
                        for (JetDelegationSpecifier specifier : klass.getDelegationSpecifiers()) {
                            JetTypeReference typeReference = specifier.getTypeReference();
                            Type type = bindingContext.resolveTypeReference(typeReference);
                            holder.createWeakWarningAnnotation(typeReference, type.toString());
                        }
                    }

                    @Override
                    public void visitNamespace(JetNamespace namespace) {
                        for (JetDeclaration declaration : namespace.getDeclarations()) {
                            declaration.accept(this);
                        }
                    }
                });
            }
            catch (Throwable e) {
                // TODO
                holder.createErrorAnnotation(new TextRange(0, 1), e.getClass().getCanonicalName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
