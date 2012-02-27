/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetHighlighter;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.quickfix.JetIntentionActionFactory;
import org.jetbrains.jet.plugin.quickfix.QuickFixes;

import java.util.Collection;
import java.util.List;
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

            try {
                final BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);

                if (errorReportingEnabled) {
                    Collection<Diagnostic> diagnostics = Sets.newLinkedHashSet(bindingContext.getDiagnostics());
                    Set<PsiElement> redeclarations = Sets.newHashSet();
                    for (Diagnostic diagnostic : diagnostics) {

                        // This is needed because we have the same context for all files
                        if (diagnostic.getPsiFile() != file) continue;

                        registerDiagnosticAnnotations(diagnostic, redeclarations, holder);
                    }
                }

                highlightBackingFields(holder, file, bindingContext);

                file.acceptChildren(new JetVisitorVoid() {
                    @Override
                    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
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

                    private void markVariableAsWrappedIfNeeded(@NotNull ASTNode node, DeclarationDescriptor target) {
                        if (target instanceof VariableDescriptor) {
                            VariableDescriptor variableDescriptor = (VariableDescriptor) target;
                            if (bindingContext.get(MUST_BE_WRAPPED_IN_A_REF, variableDescriptor)) {
                                holder.createInfoAnnotation(node, "Wrapped into a ref-object to be modifier when captured in a closure").setTextAttributes(JetHighlighter.JET_WRAPPED_INTO_REF);
                            }

                        }
                    }

                    @Override
                    public void visitProperty(@NotNull JetProperty property) {
                        DeclarationDescriptor declarationDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, property);
                        PsiElement nameIdentifier = property.getNameIdentifier();
                        if (nameIdentifier != null) {
                            markVariableAsWrappedIfNeeded(nameIdentifier.getNode(), declarationDescriptor);
                        }
                        super.visitProperty(property);
                    }

                    @Override
                    public void visitExpression(@NotNull JetExpression expression) {
                        JetType autoCast = bindingContext.get(AUTOCAST, expression);
                        if (autoCast != null) {
                            holder.createInfoAnnotation(expression, "Automatically cast to " + autoCast).setTextAttributes(JetHighlighter.JET_AUTO_CAST_EXPRESSION);
                        }
                        expression.acceptChildren(this);
                    }

                    @Override
                    public void visitJetElement(@NotNull JetElement element) {
                        element.acceptChildren(this);
                    }
                });
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (AssertionError e) {
                // For failing tests and to notify about idea internal error in -ea mode
                holder.createErrorAnnotation(element, e.getClass().getCanonicalName() + ": " + e.getMessage());
                throw e;
            }
            catch (Throwable e) {
                // TODO
                holder.createErrorAnnotation(element, e.getClass().getCanonicalName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void registerDiagnosticAnnotations(
            @NotNull Diagnostic diagnostic,
            @NotNull Set<PsiElement> redeclarations,
            @NotNull final AnnotationHolder holder
    ) {
        List<TextRange> textRanges = diagnostic.getTextRanges();
        if (diagnostic.getSeverity() == Severity.ERROR) {
            if (diagnostic.getFactory() == Errors.UNRESOLVED_IDE_TEMPLATE) {
                return;
            }
            if (diagnostic instanceof UnresolvedReferenceDiagnostic) {
                UnresolvedReferenceDiagnostic unresolvedReferenceDiagnostic = (UnresolvedReferenceDiagnostic) diagnostic;
                JetReferenceExpression referenceExpression = unresolvedReferenceDiagnostic.getPsiElement();
                PsiReference reference = referenceExpression.getReference();
                if (reference instanceof MultiRangeReference) {
                    MultiRangeReference mrr = (MultiRangeReference) reference;
                    for (TextRange range : mrr.getRanges()) {
                        Annotation annotation = holder.createErrorAnnotation(
                                range.shiftRight(referenceExpression.getTextOffset()),
                                diagnostic.getMessage());

                        registerQuickFix(annotation, diagnostic);

                        annotation.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                    }
                }
                else {
                    for (TextRange textRange : textRanges) {
                        Annotation annotation = holder.createErrorAnnotation(textRange, diagnostic.getMessage());
                        registerQuickFix(annotation, diagnostic);
                        annotation.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                    }
                }

                return;
            }

            if (diagnostic instanceof RedeclarationDiagnostic) {
                RedeclarationDiagnostic redeclarationDiagnostic = (RedeclarationDiagnostic) diagnostic;
                registerQuickFix(markRedeclaration(redeclarations, redeclarationDiagnostic, holder), diagnostic);
                return;
            }

            // Generic annotation
            for (TextRange textRange : textRanges) {
                Annotation errorAnnotation = holder.createErrorAnnotation(textRange, getMessage(diagnostic));
                registerQuickFix(errorAnnotation, diagnostic);
            }
        }
        else if (diagnostic.getSeverity() == Severity.WARNING) {
            for (TextRange textRange : textRanges) {
                Annotation annotation = holder.createWarningAnnotation(textRange, getMessage(diagnostic));
                registerQuickFix(annotation, diagnostic);

                if (diagnostic.getFactory() instanceof UnusedElementDiagnosticFactory) {
                    annotation.setHighlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL);
                }
            }
        }
    }

    /*
     * Add a quick fix if and return modified annotation.
     */
    @Nullable
    private Annotation registerQuickFix(
            @Nullable Annotation annotation,
            @NotNull Diagnostic diagnostic) {

        if (annotation == null) {
            return null;
        }

        Collection<JetIntentionActionFactory> intentionActionFactories = QuickFixes.getActionFactories(diagnostic.getFactory());
        for (JetIntentionActionFactory intentionActionFactory : intentionActionFactories) {
            IntentionAction action = null;
            if (intentionActionFactory != null) {
                action = intentionActionFactory.createAction(diagnostic);
            }
            if (action != null) {
                annotation.registerFix(action);
            }
        }

        Collection<IntentionAction> actions = QuickFixes.getActions(diagnostic.getFactory());
        for (IntentionAction action : actions) {
            annotation.registerFix(action);
        }

        return annotation;
    }

    @NotNull
    private String getMessage(@NotNull Diagnostic diagnostic) {
        if (ApplicationManager.getApplication().isInternal() || ApplicationManager.getApplication().isUnitTestMode()) {
            return "[" + diagnostic.getFactory().getName() + "] " + diagnostic.getMessage();
        }
        return diagnostic.getMessage();
    }

    @Nullable
    private Annotation markRedeclaration(@NotNull Set<PsiElement> redeclarations, @NotNull RedeclarationDiagnostic diagnostic, @NotNull AnnotationHolder holder) {
        if (!redeclarations.add(diagnostic.getPsiElement())) return null;
        return holder.createErrorAnnotation(diagnostic.getTextRanges().get(0), getMessage(diagnostic));
    }


    private void highlightBackingFields(@NotNull final AnnotationHolder holder, @NotNull JetFile file, @NotNull final BindingContext bindingContext) {
        file.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitProperty(@NotNull JetProperty property) {
                VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
                if (propertyDescriptor instanceof PropertyDescriptor) {
                    if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) propertyDescriptor)) {
                        putBackingfieldAnnotation(holder, property);
                    }
                }
            }

            @Override
            public void visitParameter(@NotNull JetParameter parameter) {
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                if (propertyDescriptor != null && bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                    putBackingfieldAnnotation(holder, parameter);
                }
            }

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                element.acceptChildren(this);
            }
        });
    }

    private void putBackingfieldAnnotation(@NotNull AnnotationHolder holder, @NotNull JetNamedDeclaration element) {
        PsiElement nameIdentifier = element.getNameIdentifier();
        if (nameIdentifier != null) {
            holder.createInfoAnnotation(
                    nameIdentifier,
                    "This property has a backing field")
                .setTextAttributes(JetHighlighter.JET_PROPERTY_WITH_BACKING_FIELD_IDENTIFIER);
        }
    }
}
