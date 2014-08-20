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

package org.jetbrains.jet.plugin.refactoring.inline;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.lang.Language;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.RefactoringMessageDialog;
import com.intellij.util.Function;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class KotlinInlineValHandler extends InlineActionHandler {
    @Override
    public boolean isEnabledForLanguage(Language l) {
        return l.equals(JetLanguage.INSTANCE);
    }

    @Override
    public boolean canInlineElement(PsiElement element) {
        if (!(element instanceof JetProperty)) {
            return false;
        }
        JetProperty property = (JetProperty) element;
        return !property.isVar() && property.getGetter() == null && property.getReceiverTypeRef() == null;
    }

    @Override
    public void inlineElement(final Project project, final Editor editor, PsiElement element) {
        final JetProperty val = (JetProperty) element;
        String name = val.getName();

        JetExpression initializerInDeclaration = val.getInitializer();

        final List<JetExpression> referenceExpressions = findReferenceExpressions(val);

        final Set<PsiElement> assignments = Sets.newHashSet();
        for (JetExpression expression : referenceExpressions) {
            PsiElement parent = expression.getParent();
            if (parent instanceof JetBinaryExpression &&
                ((JetBinaryExpression) parent).getOperationToken() == JetTokens.EQ &&
                ((JetBinaryExpression) parent).getLeft() == expression) {
                assignments.add(parent);
            }
        }

        final JetExpression initializer;
        if (initializerInDeclaration != null) {
            initializer = initializerInDeclaration;
        }
        else {
            if (assignments.size() == 1) {
                initializer = ((JetBinaryExpression) assignments.iterator().next()).getRight();
            }
            else {
                initializer = null;
            }
            if (initializer == null) {
                String key = assignments.isEmpty() ? "variable.has.no.initializer" : "variable.has.no.dominating.definition";
                String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, name));
                CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.message("inline.variable.title"), HelpID.INLINE_VARIABLE);
                return;
            }
        }

        final String typeArgumentsForCall = getTypeArgumentsStringForCall(initializer);
        final String parametersForFunctionLiteral = getParametersForFunctionLiteral(initializer);

        highlightExpressions(project, editor, referenceExpressions);
        if (!showDialog(project, name, referenceExpressions)) {
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
            }
            return;
        }

        final List<JetExpression> inlinedExpressions = Lists.newArrayList();
        CommandProcessor.getInstance().executeCommand(
                project,
                new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                for (JetExpression referenceExpression : referenceExpressions) {
                                    if (assignments.contains(referenceExpression.getParent())) {
                                        continue;
                                    }

                                    inlinedExpressions.add(replaceExpression(referenceExpression, initializer));
                                }

                                for (PsiElement assignment : assignments) {
                                    assignment.delete();
                                }

                                val.delete();

                                if (typeArgumentsForCall != null) {
                                    addTypeArguments(typeArgumentsForCall, inlinedExpressions);
                                }

                                if (parametersForFunctionLiteral != null) {
                                    addFunctionLiteralParameterTypes(parametersForFunctionLiteral, inlinedExpressions);
                                }

                                highlightExpressions(project, editor, inlinedExpressions);
                            }
                        });
                    }
                },
                RefactoringBundle.message("inline.command", name),
                null);
    }

    private static void highlightExpressions(Project project, Editor editor, List<? extends PsiElement> elements) {
        if (editor == null || ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
        TextAttributes searchResultsAttributes = editorColorsManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        HighlightManager highlightManager = HighlightManager.getInstance(project);

        PsiElement[] elementsArray = elements.toArray(new PsiElement[elements.size()]);

        highlightManager.addOccurrenceHighlights(editor, elementsArray, searchResultsAttributes, true, null);
    }

    private static List<JetExpression> findReferenceExpressions(JetProperty val) {
        List<JetExpression> result = Lists.newArrayList();

        for (PsiReference reference : ReferencesSearch.search(val, GlobalSearchScope.allScope(val.getProject()), false).findAll()) {
            JetExpression expression = (JetExpression) reference.getElement();
            PsiElement parent = expression.getParent();
            if (parent instanceof JetQualifiedExpression && ((JetQualifiedExpression) parent).getSelectorExpression() == expression) {
                result.add((JetQualifiedExpression) parent);
            }
            else {
                result.add(expression);
            }
        }

        return result;
    }

    private static boolean showDialog(Project project, String name, List<JetExpression> referenceExpressions) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return true;
        }

        RefactoringMessageDialog dialog = new RefactoringMessageDialog(
                RefactoringBundle.message("inline.variable.title"),
                RefactoringBundle.message("inline.local.variable.prompt", name) + " " +
                                  RefactoringBundle.message("occurences.string", referenceExpressions.size()),
                HelpID.INLINE_VARIABLE,
                "OptionPane.questionIcon",
                true,
                project);

        dialog.show();
        return dialog.isOK();
    }

    @Nullable
    private static String getParametersForFunctionLiteral(JetExpression initializer) {
        JetFunctionLiteralExpression functionLiteralExpression = getFunctionLiteralExpression(initializer);
        if (functionLiteralExpression == null) {
            return null;
        }

        BindingContext context = AnalyzerFacadeWithCache.getContextForElement(initializer);
        SimpleFunctionDescriptor fun = context.get(BindingContext.FUNCTION, functionLiteralExpression.getFunctionLiteral());
        if (fun == null || ErrorUtils.containsErrorType(fun)) {
            return null;
        }

        return StringUtil.join(fun.getValueParameters(), new Function<ValueParameterDescriptor, String>() {
            @Override
            public String fun(ValueParameterDescriptor descriptor) {
                return descriptor.getName() + ": " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(descriptor.getType());
            }
        }, ", ");
    }

    @Nullable
    private static JetFunctionLiteralExpression getFunctionLiteralExpression(@NotNull JetExpression expression) {
        if (expression instanceof JetParenthesizedExpression) {
            JetExpression inner = ((JetParenthesizedExpression) expression).getExpression();
            return inner == null ? null : getFunctionLiteralExpression(inner);
        }
        if (expression instanceof JetFunctionLiteralExpression) {
            return (JetFunctionLiteralExpression) expression;
        }
        return null;
    }

    private static void addFunctionLiteralParameterTypes(@NotNull String parameters, @NotNull List<JetExpression> inlinedExpressions) {
        JetFile containingFile = inlinedExpressions.get(0).getContainingJetFile();
        List<JetFunctionLiteralExpression> functionsToAddParameters = Lists.newArrayList();

        ResolveSessionForBodies resolveSessionForBodies = ResolvePackage.getLazyResolveSession(containingFile);
        for (JetExpression inlinedExpression : inlinedExpressions) {
            JetFunctionLiteralExpression functionLiteralExpression = getFunctionLiteralExpression(inlinedExpression);
            assert functionLiteralExpression != null : "can't find function literal expression for " + inlinedExpression.getText();

            if (needToAddParameterTypes(functionLiteralExpression, resolveSessionForBodies)) {
                functionsToAddParameters.add(functionLiteralExpression);
            }
        }

        JetPsiFactory psiFactory = JetPsiFactory(containingFile);
        for (JetFunctionLiteralExpression functionLiteralExpression : functionsToAddParameters) {
            JetFunctionLiteral functionLiteral = functionLiteralExpression.getFunctionLiteral();

            JetParameterList currentParameterList = functionLiteral.getValueParameterList();
            JetParameterList newParameterList = psiFactory.createParameterList("(" + parameters + ")");
            if (currentParameterList != null) {
                currentParameterList.replace(newParameterList);
            }
            else {
                PsiElement openBraceElement = functionLiteral.getOpenBraceNode().getPsi();

                PsiElement nextSibling = openBraceElement.getNextSibling();
                PsiElement whitespaceToAdd = nextSibling instanceof PsiWhiteSpace && nextSibling.getText().contains("\n")
                        ? nextSibling.copy() : null;

                Pair<PsiElement, PsiElement> whitespaceAndArrow = psiFactory.createWhitespaceAndArrow();
                functionLiteral.addRangeAfter(whitespaceAndArrow.getFirst(), whitespaceAndArrow.getSecond(), openBraceElement);

                functionLiteral.addAfter(newParameterList, openBraceElement);
                if (whitespaceToAdd != null) {
                    functionLiteral.addAfter(whitespaceToAdd, openBraceElement);
                }
            }
            ShortenReferences.INSTANCE$.process(functionLiteralExpression.getValueParameters());
        }
    }

    private static boolean needToAddParameterTypes(
            @NotNull JetFunctionLiteralExpression functionLiteralExpression,
            @NotNull ResolveSessionForBodies resolveSessionForBodies
    ) {
        JetFunctionLiteral functionLiteral = functionLiteralExpression.getFunctionLiteral();
        BindingContext context = resolveSessionForBodies.resolveToElement(functionLiteralExpression);
        for (Diagnostic diagnostic : context.getDiagnostics()) {
            DiagnosticFactory<?> factory = diagnostic.getFactory();
            PsiElement element = diagnostic.getPsiElement();
            boolean hasCantInferParameter = factory == Errors.CANNOT_INFER_PARAMETER_TYPE && element.getParent().getParent() == functionLiteral;
            boolean hasUnresolvedItOrThis = factory == Errors.UNRESOLVED_REFERENCE && element.getText().equals("it") &&
                         PsiTreeUtil.getParentOfType(element, JetFunctionLiteral.class) == functionLiteral;
            if (hasCantInferParameter || hasUnresolvedItOrThis) {
                return true;
            }
        }
        return false;
    }

    private static void addTypeArguments(@NotNull String typeArguments, @NotNull List<JetExpression> inlinedExpressions) {
        JetFile containingFile = inlinedExpressions.get(0).getContainingJetFile();
        List<JetCallExpression> callsToAddArguments = Lists.newArrayList();

        ResolveSessionForBodies resolveSessionForBodies = ResolvePackage.getLazyResolveSession(containingFile);
        for (JetExpression inlinedExpression : inlinedExpressions) {
            BindingContext context = resolveSessionForBodies.resolveToElement(inlinedExpression);
            Call call = CallUtilPackage.getCallWithAssert(inlinedExpression, context);

            JetElement callElement = call.getCallElement();
            if (callElement instanceof JetCallExpression && hasIncompleteTypeInferenceDiagnostic(call, context) &&
                    call.getTypeArgumentList() == null) {
                callsToAddArguments.add((JetCallExpression) callElement);
            }
        }

        JetPsiFactory psiFactory = JetPsiFactory(containingFile);
        for (JetCallExpression call : callsToAddArguments) {
            call.addAfter(psiFactory.createTypeArguments("<" + typeArguments + ">"), call.getCalleeExpression());
            ShortenReferences.INSTANCE$.process(call.getTypeArgumentList());
        }
    }

    @Nullable
    private static String getTypeArgumentsStringForCall(@NotNull JetExpression initializer) {
        BindingContext context = AnalyzerFacadeWithCache.getContextForElement(initializer);
        ResolvedCall<?> call = CallUtilPackage.getResolvedCall(initializer, context);
        if (call == null) return null;

        List<JetType> typeArguments = Lists.newArrayList();
        Map<TypeParameterDescriptor, JetType> typeArgumentMap = call.getTypeArguments();
        for (TypeParameterDescriptor typeParameter : call.getCandidateDescriptor().getTypeParameters()) {
            typeArguments.add(typeArgumentMap.get(typeParameter));
        }

        return StringUtil.join(typeArguments, new Function<JetType, String>() {
            @Override
            public String fun(JetType type) {
                return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type);
            }
        }, ", ");
    }

    private static boolean hasIncompleteTypeInferenceDiagnostic(
            @NotNull Call call,
            @NotNull BindingContext context
    ) {
        JetExpression callee = call.getCalleeExpression();
        for (Diagnostic diagnostic : context.getDiagnostics()) {
            if (diagnostic.getFactory() == Errors.TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER && diagnostic.getPsiElement() == callee) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static JetExpression replaceExpression(
            @NotNull PsiElement referenceElement,
            @NotNull JetExpression newExpression
    ) {
        PsiElement parent = referenceElement.getParent();
        if (parent instanceof JetSimpleNameStringTemplateEntry &&
            !(newExpression instanceof JetSimpleNameExpression)) {
            JetBlockStringTemplateEntry templateEntry =
                    (JetBlockStringTemplateEntry) parent.replace(
                            JetPsiFactory((JetElement) parent).createBlockStringTemplateEntry(newExpression));
            JetExpression expression = templateEntry.getExpression();
            assert expression != null;
            return expression;
        }
        return (JetExpression) referenceElement.replace(newExpression.copy());
    }
}
