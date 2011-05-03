package org.jetbrains.jet.lang.annotations;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetHighlighter;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.PropertyDescriptor;
import org.jetbrains.jet.lang.types.VariableDescriptor;

import java.util.Collection;
import java.util.HashSet;

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
                final Collection<DeclarationDescriptor> redeclarations = new HashSet<DeclarationDescriptor>();
                final BindingContext bindingContext = AnalyzingUtils.analyzeFile(file, new ErrorHandler() {
                    @Override
                    public void unresolvedReference(JetReferenceExpression referenceExpression) {
                        PsiReference reference = referenceExpression.getReference();
                        if (reference instanceof MultiRangeReference) {
                            MultiRangeReference mrr = (MultiRangeReference) reference;
                            for (TextRange range : mrr.getRanges()) {
                                holder.createErrorAnnotation(range.shiftRight(referenceExpression.getTextOffset()), "Unresolved").setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                            }
                        } else {
                            holder.createErrorAnnotation(referenceExpression, "Unresolved").setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                        }
                    }

                    @Override
                    public void typeMismatch(JetExpression expression, JetType expectedType, JetType actualType) {
                        holder.createErrorAnnotation(expression, "Type mismatch: inferred type is " + actualType + " but " + expectedType + " was expected");
                    }

                    @Override
                    public void redeclaration(DeclarationDescriptor existingDescriptor, DeclarationDescriptor redeclaredDescriptor) {
                        redeclarations.add(existingDescriptor);
                        redeclarations.add(redeclaredDescriptor);
                    }

                    @Override
                    public void genericError(@NotNull ASTNode node, String errorMessage) {
                        holder.createErrorAnnotation(node, errorMessage);
                    }

                    @Override
                    public void genericWarning(ASTNode node, String message) {
                        holder.createWarningAnnotation(node, message);
                    }
                });
                for (DeclarationDescriptor redeclaration : redeclarations) {
                    PsiElement declarationPsiElement = bindingContext.getDeclarationPsiElement(redeclaration);
                    if (declarationPsiElement != null) {
                        holder.createErrorAnnotation(declarationPsiElement, "Redeclaration");
                    }
                }

                highlightBackingFields(holder, file, bindingContext);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                // TODO
                holder.createErrorAnnotation(element, e.getClass().getCanonicalName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void highlightBackingFields(final AnnotationHolder holder, JetFile file, final BindingContext bindingContext) {
        file.acceptChildren(new JetVisitor() {
            @Override
            public void visitProperty(JetProperty property) {
                VariableDescriptor propertyDescriptor = bindingContext.getVariableDescriptor(property);
                if (propertyDescriptor instanceof PropertyDescriptor) {
                    if (bindingContext.hasBackingField((PropertyDescriptor) propertyDescriptor)) {
                        putBackingfieldAnnotation(holder, property);
                    }
                }
            }

            @Override
            public void visitParameter(JetParameter parameter) {
                PropertyDescriptor propertyDescriptor = bindingContext.getPropertyDescriptor(parameter);
                if (propertyDescriptor != null && bindingContext.hasBackingField(propertyDescriptor)) {
                    putBackingfieldAnnotation(holder, parameter);
                }
            }

            @Override
            public void visitJetElement(JetElement elem) {
                elem.acceptChildren(this);
            }
        });
    }

    private void putBackingfieldAnnotation(AnnotationHolder holder, JetNamedDeclaration element) {
        holder.createInfoAnnotation(
                element.getNameIdentifier(),
                "This property has a backing field")
            .setTextAttributes(JetHighlighter.JET_PROPERTY_WITH_BACKING_FIELD_IDENTIFIER);
    }
}
