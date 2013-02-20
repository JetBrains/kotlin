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

import com.google.common.collect.Sets;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.quickfix.JetIntentionActionFactory;
import org.jetbrains.jet.plugin.quickfix.QuickFixes;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JetPsiChecker implements Annotator {
    private static volatile boolean errorReportingEnabled = true;
    private static boolean namesHighlightingTest;

    private static final Logger LOG = Logger.getInstance(JetPsiChecker.class);

    public static void setErrorReportingEnabled(boolean value) {
        errorReportingEnabled = value;
    }

    public static boolean isErrorReportingEnabled() {
        return errorReportingEnabled;
    }

    @TestOnly
    public static void setNamesHighlightingTest(boolean namesHighlightingTest) {
        JetPsiChecker.namesHighlightingTest = namesHighlightingTest;
    }

    static boolean isNamesHighlightingEnabled() {
        return !ApplicationManager.getApplication().isUnitTestMode() || namesHighlightingTest;
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
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        for (HighlightingVisitor visitor : getBeforeAnalysisVisitors(holder)) {
            element.accept(visitor);
        }

        if (element instanceof JetFile) {
            JetFile file = (JetFile)element;

            try {
                BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file).getBindingContext();

                boolean isInContent = ProjectFileIndex.SERVICE.getInstance(element.getProject()).isInContent(file.getVirtualFile());
                if (errorReportingEnabled && isInContent) {
                    Collection<Diagnostic> diagnostics = Sets.newLinkedHashSet(bindingContext.getDiagnostics());
                    Set<PsiElement> redeclarations = Sets.newHashSet();
                    for (Diagnostic diagnostic : diagnostics) {
                        // This is needed because we have the same context for all files
                        if (diagnostic.getPsiFile() != file) continue;

                        registerDiagnosticAnnotations(diagnostic, redeclarations, holder);
                    }
                }

                for (HighlightingVisitor visitor : getAfterAnalysisVisitor(holder, bindingContext)) {
                    file.acceptChildren(visitor);
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                // For failing tests and to notify about idea internal error in -ea mode
                holder.createErrorAnnotation(element, e.getClass().getCanonicalName() + ": " + e.getMessage());
                LOG.error(e);
                if (ApplicationManager.getApplication().isUnitTestMode()) {
                    throw ExceptionUtils.rethrow(e);
                }
            }
        }
    }

    private static void registerDiagnosticAnnotations(@NotNull Diagnostic diagnostic,
                                                      @NotNull Set<PsiElement> redeclarations,
                                                      @NotNull final AnnotationHolder holder) {
        if (!diagnostic.isValid()) return;
        List<TextRange> textRanges = diagnostic.getTextRanges();
        if (diagnostic.getSeverity() == Severity.ERROR) {
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                JetReferenceExpression referenceExpression = (JetReferenceExpression)diagnostic.getPsiElement();
                PsiReference reference = referenceExpression.getReference();
                if (reference instanceof MultiRangeReference) {
                    MultiRangeReference mrr = (MultiRangeReference)reference;
                    for (TextRange range : mrr.getRanges()) {
                        Annotation annotation = holder.createErrorAnnotation(range.shiftRight(referenceExpression.getTextOffset()), getDefaultMessage(diagnostic));
                        annotation.setTooltip(getMessage(diagnostic));

                        registerQuickFix(annotation, diagnostic);

                        annotation.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                    }
                }
                else {
                    for (TextRange textRange : textRanges) {
                        Annotation annotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic));
                        annotation.setTooltip(getMessage(diagnostic));
                        registerQuickFix(annotation, diagnostic);
                        annotation.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                    }
                }

                return;
            }

            if (diagnostic.getFactory() == Errors.ILLEGAL_ESCAPE_SEQUENCE) {
                for (TextRange textRange : diagnostic.getTextRanges()) {
                    Annotation annotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic));
                    annotation.setTooltip(getMessage(diagnostic));
                    annotation.setTextAttributes(JetHighlightingColors.INVALID_STRING_ESCAPE);
                }
                return;
            }

            if (Errors.REDECLARATION_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                registerQuickFix(markRedeclaration(redeclarations, diagnostic, holder), diagnostic);
                return;
            }

            // Generic annotation
            for (TextRange textRange : textRanges) {
                Annotation errorAnnotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic));
                errorAnnotation.setTooltip(getMessage(diagnostic));
                registerQuickFix(errorAnnotation, diagnostic);

                if (diagnostic.getFactory() == Errors.INVISIBLE_REFERENCE) {
                    errorAnnotation.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                }
            }
        }
        else if (diagnostic.getSeverity() == Severity.WARNING) {
            for (TextRange textRange : textRanges) {
                Annotation annotation = holder.createWarningAnnotation(textRange, getDefaultMessage(diagnostic));
                annotation.setTooltip(getMessage(diagnostic));
                registerQuickFix(annotation, diagnostic);

                if (Errors.UNUSED_ELEMENT_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                    annotation.setHighlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL);
                }
            }
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
    private static String getMessage(@NotNull Diagnostic diagnostic) {
        String message = IdeErrorMessages.RENDERER.render(diagnostic);
        if (ApplicationManager.getApplication().isInternal() || ApplicationManager.getApplication().isUnitTestMode()) {
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
        if (ApplicationManager.getApplication().isInternal() || ApplicationManager.getApplication().isUnitTestMode()) {
            return String.format("[%s] %s", diagnostic.getFactory().getName(), message);
        }
        return message;
    }

    @Nullable
    private static Annotation markRedeclaration(@NotNull Set<PsiElement> redeclarations,
                                                @NotNull Diagnostic redeclarationDiagnostic,
                                                @NotNull AnnotationHolder holder) {
        if (!redeclarations.add(redeclarationDiagnostic.getPsiElement())) return null;
        List<TextRange> textRanges = redeclarationDiagnostic.getTextRanges();
        if (!redeclarationDiagnostic.isValid()) return null;
        Annotation annotation = holder.createErrorAnnotation(textRanges.get(0), "");
        annotation.setTooltip(getMessage(redeclarationDiagnostic));
        return annotation;
    }
}
