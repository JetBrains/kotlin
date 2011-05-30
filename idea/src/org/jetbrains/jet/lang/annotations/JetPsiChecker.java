package org.jetbrains.jet.lang.annotations;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.progress.ProcessCanceledException;
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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author abreslav
 */
public class JetPsiChecker implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        if (element instanceof JetFile) {
            JetFile file = (JetFile) element;
            try {
                final BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache(file);

                ErrorHandler errorHandler = new ErrorHandler() {
                    private final Set<DeclarationDescriptor> redeclarations = new HashSet<DeclarationDescriptor>();

                    @Override
                    public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
                        PsiReference reference = referenceExpression.getReference();
                        if (reference instanceof MultiRangeReference) {
                            MultiRangeReference mrr = (MultiRangeReference) reference;
                            for (TextRange range : mrr.getRanges()) {
                                holder.createErrorAnnotation(range.shiftRight(referenceExpression.getTextOffset()), "Unresolved").setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                            }
                        }
                        else {
                            holder.createErrorAnnotation(referenceExpression, "Unresolved").setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                        }
                    }

                    @Override
                    public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
                        holder.createErrorAnnotation(expression, "Type mismatch: inferred type is " + actualType + " but " + expectedType + " was expected");
                    }

                    @Override
                    public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
                        markRedeclaration(existingDescriptor);
                        markRedeclaration(redeclaredDescriptor);
                    }

                    private void markRedeclaration(DeclarationDescriptor redeclaration) {
                        if (!redeclarations.add(redeclaration)) return;
                        PsiElement declarationPsiElement = bindingContext.getDeclarationPsiElement(redeclaration);
                        if (declarationPsiElement instanceof JetNamedDeclaration) {
                            holder.createErrorAnnotation(((JetNamedDeclaration) declarationPsiElement).getNameIdentifier(), "Redeclaration");
                        }
                        else if (declarationPsiElement != null) {
                            holder.createErrorAnnotation(declarationPsiElement, "Redeclaration");
                        }
                    }

                    @Override
                    public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
                        holder.createErrorAnnotation(node, errorMessage);
                    }

                    @Override
                    public void genericWarning(@NotNull ASTNode node, @NotNull String message) {
                        holder.createWarningAnnotation(node, message);
                    }
                };

                AnalyzingUtils.applyHandler(errorHandler, bindingContext);

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
            public void visitJetElement(JetElement element) {
                element.acceptChildren(this);
            }
        });
    }

    private void putBackingfieldAnnotation(AnnotationHolder holder, JetNamedDeclaration element) {
        PsiElement nameIdentifier = element.getNameIdentifier();
        if (nameIdentifier != null) {
            holder.createInfoAnnotation(
                    nameIdentifier,
                    "This property has a backing field")
                .setTextAttributes(JetHighlighter.JET_PROPERTY_WITH_BACKING_FIELD_IDENTIFIER);
        }
    }
}
