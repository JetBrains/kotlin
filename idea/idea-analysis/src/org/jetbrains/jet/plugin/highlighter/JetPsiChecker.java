/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension;
import com.intellij.codeInsight.intention.EmptyIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetCodeFragment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.plugin.ProjectRootsUtil;
import org.jetbrains.jet.plugin.actions.internal.KotlinInternalMode;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.configuration.JetModuleTypeManager;
import org.jetbrains.jet.plugin.quickfix.JetIntentionActionsFactory;
import org.jetbrains.jet.plugin.quickfix.QuickFixes;

import java.util.Collection;
import java.util.List;

public class JetPsiChecker implements Annotator, HighlightRangeExtension {
    private static boolean isNamesHighlightingEnabled = true;

    @TestOnly
    public static void setNamesHighlightingEnabled(boolean namesHighlightingEnabled) {
        isNamesHighlightingEnabled = namesHighlightingEnabled;
    }

    public static boolean isNamesHighlightingEnabled() {
        return isNamesHighlightingEnabled;
    }

    static void highlightName(@NotNull AnnotationHolder holder,
                              @NotNull PsiElement psiElement,
                              @NotNull TextAttributesKey attributesKey) {
        if (isNamesHighlightingEnabled()) {
            holder.createInfoAnnotation(psiElement, null).setTextAttributes(attributesKey);
        }
    }

    private static HighlightingVisitor[] getBeforeAnalysisVisitors(AnnotationHolder holder) {
        return new HighlightingVisitor[]{
            new SoftKeywordsHighlightingVisitor(holder),
            new LabelsHighlightingVisitor(holder),
        };
    }

    private static HighlightingVisitor[] getAfterAnalysisVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        return new AfterAnalysisHighlightingVisitor[]{
            new PropertiesHighlightingVisitor(holder, bindingContext),
            new FunctionsHighlightingVisitor(holder, bindingContext),
            new VariablesHighlightingVisitor(holder, bindingContext),
            new TypeKindHighlightingVisitor(holder, bindingContext),
            new DeprecatedAnnotationVisitor(holder, bindingContext),
        };
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(ProjectRootsUtil.isInSource(element) || element.getContainingFile() instanceof JetCodeFragment)
                || JetModuleTypeManager.getInstance().isKtFileInGradleProjectInWrongFolder(element)) {
            return;
        }

        for (HighlightingVisitor visitor : getBeforeAnalysisVisitors(holder)) {
            element.accept(visitor);
        }

        JetFile file = (JetFile) element.getContainingFile();

        AnalyzeExhaust analyzeExhaust = ResolvePackage.getAnalysisResults(file);
        if (analyzeExhaust.isError()) {
            throw new ProcessCanceledException(analyzeExhaust.getError());
        }

        BindingContext bindingContext = analyzeExhaust.getBindingContext();

        for (HighlightingVisitor visitor : getAfterAnalysisVisitor(holder, bindingContext)) {
            element.accept(visitor);
        }

        annotateElement(element, holder, bindingContext.getDiagnostics());
    }

    public static void annotateElement(PsiElement element, AnnotationHolder holder, Diagnostics diagnostics) {
        if (ProjectRootsUtil.isInSource(element, /* includeLibrarySources = */ false) || element.getContainingFile() instanceof JetCodeFragment) {
            ElementAnnotator elementAnnotator = new ElementAnnotator(element, holder);
            for (Diagnostic diagnostic : diagnostics.forElement(element)) {
                elementAnnotator.registerDiagnosticAnnotations(diagnostic);
            }
        }
    }

    @Override
    public boolean isForceHighlightParents(@NotNull PsiFile file) {
        return file instanceof JetFile;
    }

    private static class ElementAnnotator {
        private final PsiElement element;
        private final AnnotationHolder holder;

        private boolean isMarkedWithRedeclaration;

        ElementAnnotator(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
            this.element = element;
            this.holder = holder;
        }

        void registerDiagnosticAnnotations(@NotNull Diagnostic diagnostic) {
            if (!diagnostic.isValid()) return;

            assert diagnostic.getPsiElement() == element;

            List<TextRange> textRanges = diagnostic.getTextRanges();
            if (diagnostic.getSeverity() == Severity.ERROR) {
                if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                    JetReferenceExpression referenceExpression = (JetReferenceExpression)diagnostic.getPsiElement();
                    PsiReference reference = referenceExpression.getReference();
                    if (reference instanceof MultiRangeReference) {
                        MultiRangeReference mrr = (MultiRangeReference)reference;
                        for (TextRange range : mrr.getRanges()) {
                            Annotation annotation = holder.createErrorAnnotation(range.shiftRight(referenceExpression.getTextOffset()), getDefaultMessage(diagnostic));
                            setUpAnnotation(diagnostic, annotation, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                        }
                    }
                    else {
                        for (TextRange textRange : textRanges) {
                            Annotation annotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic));
                            setUpAnnotation(diagnostic, annotation, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                        }
                    }

                    return;
                }

                if (diagnostic.getFactory() == Errors.ILLEGAL_ESCAPE) {
                    for (TextRange textRange : diagnostic.getTextRanges()) {
                        Annotation annotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic));
                        annotation.setTooltip(getMessage(diagnostic));
                        annotation.setTextAttributes(JetHighlightingColors.INVALID_STRING_ESCAPE);
                    }
                    return;
                }

                if (diagnostic.getFactory() == Errors.REDECLARATION) {
                    if (!isMarkedWithRedeclaration) {
                        isMarkedWithRedeclaration = true;
                        Annotation annotation = holder.createErrorAnnotation(diagnostic.getTextRanges().get(0), "");
                        setUpAnnotation(diagnostic, annotation, null);
                    }
                    return;
                }

                // Generic annotation
                for (TextRange textRange : textRanges) {
                    Annotation errorAnnotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic));
                    setUpAnnotation(diagnostic, errorAnnotation,
                                    diagnostic.getFactory() == Errors.INVISIBLE_REFERENCE
                                    ? ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                                    : null);
                }
            }
            else if (diagnostic.getSeverity() == Severity.WARNING) {
                for (TextRange textRange : textRanges) {
                    Annotation annotation = holder.createWarningAnnotation(textRange, getDefaultMessage(diagnostic));
                    setUpAnnotation(diagnostic, annotation,
                                    Errors.UNUSED_ELEMENT_DIAGNOSTICS.contains(diagnostic.getFactory())
                                    ? ProblemHighlightType.LIKE_UNUSED_SYMBOL
                                    : null);
                }
            }
        }

        private static void setUpAnnotation(Diagnostic diagnostic, Annotation annotation, @Nullable ProblemHighlightType highlightType) {
            annotation.setTooltip(getMessage(diagnostic));
            registerQuickFix(annotation, diagnostic);

            if (highlightType != null) {
                annotation.setHighlightType(highlightType);
            }
        }

        /*
         * Add a quick fix if and return modified annotation.
         */
        @Nullable
        private static Annotation registerQuickFix(@Nullable Annotation annotation, @NotNull Diagnostic diagnostic) {
            if (annotation == null) {
                return null;
            }

            Collection<JetIntentionActionsFactory> intentionActionsFactories = QuickFixes.getActionsFactories(diagnostic.getFactory());
            for (JetIntentionActionsFactory intentionActionsFactory : intentionActionsFactories) {
                if (intentionActionsFactory != null) {
                    for (IntentionAction action: intentionActionsFactory.createActions(diagnostic)) {
                        annotation.registerFix(action);
                    }
                }
            }

            Collection<IntentionAction> actions = QuickFixes.getActions(diagnostic.getFactory());
            for (IntentionAction action : actions) {
                annotation.registerFix(action);
            }

            // Making warnings suppressable
            if (diagnostic.getSeverity() == Severity.WARNING) {
                annotation.setProblemGroup(new KotlinSuppressableWarningProblemGroup(diagnostic.getFactory()));

                List<Annotation.QuickFixInfo> fixes = annotation.getQuickFixes();
                if (fixes == null || fixes.isEmpty()) {
                    // if there are no quick fixes we need to register an EmptyIntentionAction to enable 'suppress' actions
                    annotation.registerFix(new EmptyIntentionAction(diagnostic.getFactory().getName()));
                }
            }

            return annotation;
        }

        @NotNull
        private static String getMessage(@NotNull Diagnostic diagnostic) {
            String message = IdeErrorMessages.RENDERER.render(diagnostic);
            if (KotlinInternalMode.OBJECT$.getEnabled() || ApplicationManager.getApplication().isUnitTestMode()) {
                String factoryName = diagnostic.getFactory().getName();
                if (message.startsWith("<html>")) {
                    message = String.format("<html>[%s] %s", factoryName, message.substring("<html>".length()));
                } else {
                    message = String.format("[%s] %s", factoryName, message);
                }
            }
            if (!message.startsWith("<html>")) {
                message = "<html><body>" + XmlStringUtil.escapeString(message) + "</body></html>";
            }
            return message;
        }

        @NotNull
        private static String getDefaultMessage(@NotNull Diagnostic diagnostic) {
            String message = DefaultErrorMessages.RENDERER.render(diagnostic);
            if (KotlinInternalMode.OBJECT$.getEnabled() || ApplicationManager.getApplication().isUnitTestMode()) {
                return String.format("[%s] %s", diagnostic.getFactory().getName(), message);
            }
            return message;
        }
    }

}
