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
import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension;
import com.intellij.codeInsight.intention.EmptyIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.containers.MultiMap;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.quickfix.JetIntentionActionsFactory;
import org.jetbrains.jet.plugin.quickfix.QuickFixes;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JetPsiChecker implements Annotator, HighlightRangeExtension {
    private static boolean namesHighlightingTest;
    private static final Logger LOG = Logger.getInstance(JetPsiChecker.class);

    private HighlightingPassCache passCache = null;

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
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!JetPluginUtil.isInSource(element) || JetPluginUtil.isKtFileInGradleProjectInWrongFolder(element)) {
            return;
        }

        for (HighlightingVisitor visitor : getBeforeAnalysisVisitors(holder)) {
            element.accept(visitor);
        }

        JetFile file = (JetFile) element.getContainingFile();

        if (passCache == null || passCache.isOutdated(file)) {
            passCache = new HighlightingPassCache(AnalyzerFacadeWithCache.analyzeFileWithCache(file), file);
        }

        if (!passCache.analyzeExhaust.isError()) {
            BindingContext bindingContext = passCache.analyzeExhaust.getBindingContext();
            for (HighlightingVisitor visitor : getAfterAnalysisVisitor(holder, bindingContext)) {
                element.accept(visitor);
            }

            if (JetPluginUtil.isInSource(element, /* includeLibrarySources = */ false)) {
                for (Diagnostic diagnostic : passCache.elementToDiagnostic.get(element)) {
                    registerDiagnosticAnnotations(diagnostic, passCache.redeclarations, holder);
                }
            }
        }
        else if (element instanceof JetFile) {
            Throwable error = passCache.analyzeExhaust.getError();
            if (JetPluginUtil.isInSource(element, /* includeLibrarySources = */ false)) {
                holder.createErrorAnnotation(file, error.getClass().getCanonicalName() + ": " + error.getMessage());
            }

            LOG.error(error);
        }
    }

    private static void registerDiagnosticAnnotations(@NotNull Diagnostic diagnostic,
                                                      @NotNull Set<PsiElement> redeclarations,
                                                      @NotNull AnnotationHolder holder) {
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

            if (diagnostic.getFactory() == Errors.ILLEGAL_ESCAPE) {
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

    @Override
    public boolean isForceHighlightParents(@NotNull PsiFile file) {
        return file instanceof JetFile;
    }

    private static class HighlightingPassCache {
        private final AnalyzeExhaust analyzeExhaust;
        private final MultiMap<PsiElement, Diagnostic> elementToDiagnostic;

        private final Set<PsiElement> redeclarations = Sets.newHashSet();
        private final JetFile jetFile;
        private final long modificationCount;

        public HighlightingPassCache(AnalyzeExhaust analyzeExhaust, JetFile jetFile) {
            this.analyzeExhaust = analyzeExhaust;
            this.jetFile = jetFile;
            this.elementToDiagnostic = buildElementToDiagnosticCache(analyzeExhaust, jetFile);
            this.modificationCount = PsiManager.getInstance(jetFile.getProject()).getModificationTracker().getModificationCount();
        }

        public boolean isOutdated(JetFile jetFile) {
            return this.jetFile != jetFile || PsiManager.getInstance(jetFile.getProject()).getModificationTracker().getModificationCount() != modificationCount;
        }

        private static MultiMap<PsiElement, Diagnostic> buildElementToDiagnosticCache(AnalyzeExhaust analyzeExhaust, JetFile jetFile) {
            MultiMap<PsiElement, Diagnostic> elementToDiagnostic = MultiMap.create();
            Collection<Diagnostic> diagnostics = Sets.newLinkedHashSet(analyzeExhaust.getBindingContext().getDiagnostics());

            for (Diagnostic diagnostic : diagnostics) {
                if (diagnostic.getPsiFile() == jetFile) {
                    elementToDiagnostic.putValue(diagnostic.getPsiElement(), diagnostic);
                }
            }

            return elementToDiagnostic;
        }
    }
}
