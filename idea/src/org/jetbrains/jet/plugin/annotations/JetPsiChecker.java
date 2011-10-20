package org.jetbrains.jet.plugin.annotations;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetHighlighter;
import org.jetbrains.jet.plugin.quickfix.JetIntentionActionFactory;
import org.jetbrains.jet.plugin.quickfix.QuickFixes;

import java.util.Collection;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author abreslav
 */
public class JetPsiChecker implements Annotator {

    private static volatile boolean errorReportingEnabled = true;

    public static void setErrorReportingEnabled(boolean value) {
        errorReportingEnabled = value;
    }

    public static boolean isErrorReportingEnabled() {
        return errorReportingEnabled;
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        if (element instanceof JetFile) {
            JetFile file = (JetFile) element;
            Project project = element.getProject();
            try {
                final BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache(file);

                if (errorReportingEnabled) {
                    Collection<Diagnostic> diagnostics = Sets.newLinkedHashSet(bindingContext.getDiagnostics());
                    Set<PsiElement> redeclarations = Sets.newHashSet();
                    for (Diagnostic diagnostic : diagnostics) {
                        Annotation annotation = null;
                        if (diagnostic.getSeverity() == Severity.ERROR) {
                            if (diagnostic instanceof UnresolvedReferenceDiagnostic) {
                                UnresolvedReferenceDiagnostic unresolvedReferenceDiagnostic = (UnresolvedReferenceDiagnostic) diagnostic;
                                JetReferenceExpression referenceExpression = unresolvedReferenceDiagnostic.getPsiElement();
                                PsiReference reference = referenceExpression.getReference();
                                if (reference instanceof MultiRangeReference) {
                                    MultiRangeReference mrr = (MultiRangeReference) reference;
                                    for (TextRange range : mrr.getRanges()) {
                                        holder.createErrorAnnotation(range.shiftRight(referenceExpression.getTextOffset()), diagnostic.getMessage()).setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                                    }
                                }
                                else {
                                    holder.createErrorAnnotation(referenceExpression, diagnostic.getMessage()).setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                                }
                            }
                            else if (diagnostic instanceof RedeclarationDiagnostic) {
                                RedeclarationDiagnostic redeclarationDiagnostic = (RedeclarationDiagnostic) diagnostic;
                                annotation = markRedeclaration(redeclarations, redeclarationDiagnostic, holder);
                            }
                            else {
                                annotation = holder.createErrorAnnotation(diagnostic.getFactory().getTextRange(diagnostic), getMessage(diagnostic));
                            }
                        }
                        else if (diagnostic.getSeverity() == Severity.WARNING) {
                            annotation = holder.createWarningAnnotation(diagnostic.getFactory().getTextRange(diagnostic), getMessage(diagnostic));
                        }
                        if (annotation != null && diagnostic instanceof DiagnosticWithPsiElementImpl) {
                            DiagnosticWithPsiElement diagnosticWithPsiElement = (DiagnosticWithPsiElement) diagnostic;
                            if (diagnostic.getFactory() instanceof PsiElementOnlyDiagnosticFactory) {
                                PsiElementOnlyDiagnosticFactory factory = (PsiElementOnlyDiagnosticFactory) diagnostic.getFactory();
                                Collection<JetIntentionActionFactory> intentionActionFactories = QuickFixes.get(factory);
                                for (JetIntentionActionFactory intentionActionFactory : intentionActionFactories) {
                                    IntentionAction action = null;
                                    if (intentionActionFactory != null) {
                                        action = intentionActionFactory.createAction(diagnosticWithPsiElement);
                                    }
                                    if (action != null) {
                                        annotation.registerFix(action);
                                    }
                                }
                            }
                        }
                    }
                }

                highlightBackingFields(holder, file, bindingContext);

                file.acceptChildren(new JetVisitorVoid() {
                    @Override
                    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
                        DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, expression);
                        if (target instanceof ValueParameterDescriptor) {
                            ValueParameterDescriptor parameterDescriptor = (ValueParameterDescriptor) target;
                            if (bindingContext.get(AUTO_CREATED_IT, parameterDescriptor)) {
                                holder.createInfoAnnotation(expression, "Automatically declared based on the expected type").setTextAttributes(JetHighlighter.JET_AUTOCREATED_IT);
                            }
                        }

                        markVariableAsWrappedIfNeeded(expression.getNode(), target);
                        super.visitSimpleNameExpression(expression);
                    }

                    private void markVariableAsWrappedIfNeeded(ASTNode node, DeclarationDescriptor target) {
                        if (target instanceof VariableDescriptor) {
                            VariableDescriptor variableDescriptor = (VariableDescriptor) target;
                            if (bindingContext.get(MUST_BE_WRAPPED_IN_A_REF, variableDescriptor)) {
                                holder.createInfoAnnotation(node, "Wrapped into a ref-object to be modifier when captured in a closure").setTextAttributes(JetHighlighter.JET_WRAPPED_INTO_REF);
                            }

                        }
                    }

                    @Override
                    public void visitProperty(JetProperty property) {
                        DeclarationDescriptor declarationDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, property);
                        PsiElement nameIdentifier = property.getNameIdentifier();
                        if (nameIdentifier != null) {
                            markVariableAsWrappedIfNeeded(nameIdentifier.getNode(), declarationDescriptor);
                        }
                        super.visitProperty(property);
                    }

                    @Override
                    public void visitExpression(JetExpression expression) {
                        JetType autoCast = bindingContext.get(AUTOCAST, expression);
                        if (autoCast != null) {
                            holder.createInfoAnnotation(expression, "Automatically cast to " + autoCast).setTextAttributes(JetHighlighter.JET_AUTO_CAST_EXPRESSION);
                        }
                        expression.acceptChildren(this);
                    }

                    @Override
                    public void visitJetElement(JetElement element) {
                        element.acceptChildren(this);
                    }
                });
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

    private String getMessage(Diagnostic diagnostic) {
        if (ApplicationManager.getApplication().isInternal() || ApplicationManager.getApplication().isUnitTestMode()) {
            return "[" + diagnostic.getFactory().getName() + "] " + diagnostic.getMessage();
        }
        return diagnostic.getMessage();
    }

    private Annotation markRedeclaration(Set<PsiElement> redeclarations, RedeclarationDiagnostic diagnostic, AnnotationHolder holder) {
        if (!redeclarations.add(diagnostic.getPsiElement())) return null;
        return holder.createErrorAnnotation(diagnostic.getFactory().getTextRange(diagnostic), getMessage(diagnostic));
    }


    private void highlightBackingFields(final AnnotationHolder holder, JetFile file, final BindingContext bindingContext) {
        file.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitProperty(JetProperty property) {
                VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
                if (propertyDescriptor instanceof PropertyDescriptor) {
                    if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) propertyDescriptor)) {
                        putBackingfieldAnnotation(holder, property);
                    }
                }
            }

            @Override
            public void visitParameter(JetParameter parameter) {
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                if (propertyDescriptor != null && bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
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
