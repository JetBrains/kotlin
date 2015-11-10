/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import kotlin.CollectionsKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.idea.analysis.AnalyzerUtilKt;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester;
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator;
import org.jetbrains.kotlin.idea.core.PsiModificationUtilsKt;
import org.jetbrains.kotlin.idea.intentions.ConvertToBlockBodyIntention;
import org.jetbrains.kotlin.idea.intentions.RemoveCurlyBracesFromTemplateIntention;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil;
import org.jetbrains.kotlin.idea.refactoring.introduce.IntroduceUtilKt;
import org.jetbrains.kotlin.idea.refactoring.introduce.KotlinIntroduceHandlerBase;
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ScopeUtils;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange;
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRangeKt;
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.ObservableBindingTrace;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class KotlinIntroduceVariableHandler extends KotlinIntroduceHandlerBase {

    public static final String INTRODUCE_VARIABLE = KotlinRefactoringBundle.message("introduce.variable");

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file, DataContext dataContext) {
        KotlinRefactoringUtil.SelectExpressionCallback callback = new KotlinRefactoringUtil.SelectExpressionCallback() {
            @Override
            public void run(@Nullable KtExpression expression) {
                doRefactoring(project, editor, expression, null, null);
            }
        };
        try {
            KotlinRefactoringUtil.selectExpression(editor, file, callback);
        }
        catch (KotlinRefactoringUtil.IntroduceRefactoringException e) {
            showErrorHint(project, editor, e.getMessage());
        }
    }


    public static void doRefactoring(
            @NotNull final Project project,
            @Nullable final Editor editor,
            @Nullable KtExpression _expression,
            @Nullable List<KtExpression> occurrencesToReplace,
            @Nullable final Function1<KtProperty, Unit> onNonInteractiveFinish
    ) {
        if (_expression == null) {
            showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"));
            return;
        }
        if (_expression.getParent() instanceof KtParenthesizedExpression) {
            _expression = (KtExpression)_expression.getParent();
        }
        final KtExpression expression = _expression;
        boolean noTypeInference = false;
        if (expression.getParent() instanceof KtQualifiedExpression) {
            KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression)expression.getParent();
            if (qualifiedExpression.getReceiverExpression() != expression) {
                showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"));
                return;
            }
        }
        else if (expression instanceof KtStatementExpression) {
            showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"));
            return;
        }
        else if (expression.getParent() instanceof KtOperationExpression) {
            KtOperationExpression operationExpression = (KtOperationExpression)expression.getParent();
            if (operationExpression.getOperationReference() == expression) {
                showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.expression"));
                return;
            }
        }

        //noinspection unchecked
        if (PsiTreeUtil.getNonStrictParentOfType(expression,
                                                 KtTypeReference.class, KtConstructorCalleeExpression.class, KtSuperExpression.class) != null) {
            showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.container"));
            return;
        }

        ResolutionFacade resolutionFacade = ResolutionUtils.getResolutionFacade(expression);
        final BindingContext bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.FULL);
        final KotlinType expressionType = bindingContext.getType(expression); //can be null or error type
        LexicalScope scope = ScopeUtils.getResolutionScope(expression, bindingContext, resolutionFacade);
        DataFlowInfo dataFlowInfo = BindingContextUtilsKt.getDataFlowInfo(bindingContext, expression);

        ObservableBindingTrace bindingTrace = new ObservableBindingTrace(new BindingTraceContext());
        KotlinType typeNoExpectedType = AnalyzerUtilKt.computeTypeInfoInContext(
                expression, scope, expression, bindingTrace, dataFlowInfo
        ).getType();
        if (expressionType != null && typeNoExpectedType != null && !KotlinTypeChecker.DEFAULT.equalTypes(expressionType,
                                                                                                          typeNoExpectedType)) {
            noTypeInference = true;
        }

        if (expressionType == null && bindingContext.get(BindingContext.QUALIFIER, expression) != null) {
            showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.package.expression"));
            return;
        }
        if (expressionType != null && KotlinBuiltIns.isUnit(expressionType)) {
            showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.expression.has.unit.type"));
            return;
        }
        final PsiElement container = getContainer(expression);
        PsiElement occurrenceContainer = getOccurrenceContainer(expression);
        if (container == null) {
            showErrorHint(project, editor, KotlinRefactoringBundle.message("cannot.refactor.no.container"));
            return;
        }
        final boolean isInplaceAvailableOnDataContext =
            editor != null &&
            editor.getSettings().isVariableInplaceRenameEnabled() &&
            !ApplicationManager.getApplication().isUnitTestMode();

        final List<KtExpression> allOccurrences;
        if (occurrencesToReplace == null) {
            allOccurrences = findOccurrences(occurrenceContainer, expression);
        }
        else {
            allOccurrences = occurrencesToReplace;
        }

        final boolean finalNoTypeInference = noTypeInference;
        Pass<OccurrencesChooser.ReplaceChoice> callback = new Pass<OccurrencesChooser.ReplaceChoice>() {
            @Override
            public void pass(OccurrencesChooser.ReplaceChoice replaceChoice) {
                boolean replaceOccurrence = shouldReplaceOccurrence(expression, bindingContext, container);
                List<KtExpression> allReplaces;
                if (OccurrencesChooser.ReplaceChoice.ALL == replaceChoice) {
                    if (allOccurrences.size() > 1) replaceOccurrence = true;
                    allReplaces = allOccurrences;
                }
                else {
                    allReplaces = Collections.singletonList(expression);
                }

                PsiElement commonParent = PsiTreeUtil.findCommonParent(allReplaces);
                PsiElement commonContainer = getContainer(commonParent);
                NewDeclarationNameValidator validator = new NewDeclarationNameValidator(
                        commonContainer,
                        calculateAnchor(commonParent, commonContainer, allReplaces),
                        NewDeclarationNameValidator.Target.VARIABLES
                );
                final Collection<String> suggestedNames = KotlinNameSuggester.INSTANCE$.suggestNamesByExpressionAndType(
                        expression, ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL), validator, "value");
                final Ref<KtProperty> propertyRef = new Ref<KtProperty>();
                final ArrayList<KtExpression> references = new ArrayList<KtExpression>();
                final Ref<KtExpression> reference = new Ref<KtExpression>();
                final Runnable introduceRunnable = introduceVariable(
                        expression, suggestedNames.iterator().next(), allReplaces, commonContainer,
                        commonParent, replaceOccurrence, propertyRef, references,
                        reference, finalNoTypeInference, expressionType, bindingContext);
                CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(introduceRunnable);
                        KtProperty property = propertyRef.get();
                        if (property != null) {
                            if (editor != null) {
                                editor.getCaretModel().moveToOffset(property.getTextOffset());
                                editor.getSelectionModel().removeSelection();
                                if (isInplaceAvailableOnDataContext) {
                                    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
                                    PsiDocumentManager.getInstance(project).
                                            doPostponedOperationsAndUnblockDocument(editor.getDocument());
                                    KotlinVariableInplaceIntroducer variableIntroducer =
                                            new KotlinVariableInplaceIntroducer(property,
                                                                                reference.get(),
                                                                                references.toArray(new KtExpression[references.size()]),
                                                                                suggestedNames,
                                                                                /*todo*/ false,
                                                                                /*todo*/ false,
                                                                                expressionType,
                                                                                finalNoTypeInference,
                                                                                project,
                                                                                editor);
                                    variableIntroducer.startInplaceIntroduceTemplate();
                                }
                            }
                            else if (onNonInteractiveFinish != null) {
                                onNonInteractiveFinish.invoke(property);
                            }
                        }
                    }
                }, INTRODUCE_VARIABLE, null);
            }
        };
        if (isInplaceAvailableOnDataContext && occurrencesToReplace == null) {
            OccurrencesChooser.<KtExpression>simpleChooser(editor).
                showChooser(expression, allOccurrences, callback);
        }
        else {
            callback.pass(OccurrencesChooser.ReplaceChoice.ALL);
        }
    }

    private static Key<Boolean> OCCURRENCE = Key.create("OCCURRENCE");

    private static Runnable introduceVariable(
            final KtExpression expression,
            final String nameSuggestion,
            final List<KtExpression> allReplaces,
            final PsiElement commonContainer,
            final PsiElement commonParent,
            final boolean replaceOccurrence,
            final Ref<KtProperty> propertyRef,
            final ArrayList<KtExpression> references,
            final Ref<KtExpression> reference,
            final boolean noTypeInference,
            final KotlinType expressionType,
            final BindingContext bindingContext
    ) {
        final KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(expression);
        return new Runnable() {
            @Override
            public void run() {
                if (commonContainer instanceof KtDeclarationWithBody) {
                    KtDeclarationWithBody originalDeclaration = (KtDeclarationWithBody) commonContainer;
                    final KtExpression originalBody = originalDeclaration.getBodyExpression();
                    assert originalBody != null : "Original body is not found: " + originalDeclaration;

                    Key<Boolean> EXPRESSION_KEY = Key.create("EXPRESSION_KEY");
                    Key<Boolean> REPLACE_KEY = Key.create("REPLACE_KEY");
                    Key<Boolean> COMMON_PARENT_KEY = Key.create("COMMON_PARENT_KEY");
                    expression.putCopyableUserData(EXPRESSION_KEY, true);
                    for (KtExpression replace : allReplaces) {
                        replace.putCopyableUserData(REPLACE_KEY, true);
                    }
                    commonParent.putCopyableUserData(COMMON_PARENT_KEY, true);

                    KtDeclarationWithBody newDeclaration = ConvertToBlockBodyIntention.Companion.convert(originalDeclaration);

                    KtBlockExpression newCommonContainer = (KtBlockExpression) newDeclaration.getBodyExpression();
                    assert newCommonContainer != null : "New body is not found: " + newDeclaration;

                    KtExpression newExpression = IntroduceUtilKt.findExpressionByCopyableDataAndClearIt(newCommonContainer, EXPRESSION_KEY);
                    PsiElement newCommonParent = IntroduceUtilKt.findElementByCopyableDataAndClearIt(newCommonContainer, COMMON_PARENT_KEY);
                    List<KtExpression> newAllReplaces = IntroduceUtilKt
                            .findExpressionsByCopyableDataAndClearIt(newCommonContainer, REPLACE_KEY);

                    run(newExpression, newCommonContainer, newCommonParent, newAllReplaces);
                }
                else {
                    run(expression, commonContainer, commonParent, allReplaces);
                }
            }

            private void run(
                    KtExpression expression,
                    PsiElement commonContainer,
                    PsiElement commonParent,
                    List<KtExpression> allReplaces
            ) {
                String variableText = "val " + nameSuggestion;
                if (noTypeInference) {
                    variableText += ": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(expressionType);
                }
                variableText += " = ";
                if (expression instanceof KtParenthesizedExpression) {
                    KtParenthesizedExpression parenthesizedExpression = (KtParenthesizedExpression)expression;
                    KtExpression innerExpression = parenthesizedExpression.getExpression();
                    if (innerExpression != null) {
                        variableText += innerExpression.getText();
                    }
                    else {
                        variableText += expression.getText();
                    }
                }
                else {
                    variableText += expression.getText();
                }
                KtProperty property = psiFactory.createProperty(variableText);
                PsiElement anchor = calculateAnchor(commonParent, commonContainer, allReplaces);
                if (anchor == null) return;
                boolean needBraces = !(commonContainer instanceof KtBlockExpression);
                if (!needBraces) {
                    property = (KtProperty)commonContainer.addBefore(property, anchor);
                    commonContainer.addBefore(psiFactory.createNewLine(), anchor);
                }
                else {
                    KtExpression emptyBody = psiFactory.createEmptyBody();
                    PsiElement firstChild = emptyBody.getFirstChild();
                    emptyBody.addAfter(psiFactory.createNewLine(), firstChild);

                    if (replaceOccurrence && commonContainer != null) {
                        for (KtExpression replace : allReplaces) {
                            KtExpression exprAfterReplace = replaceExpression(replace, false);
                            exprAfterReplace.putCopyableUserData(OCCURRENCE, true);
                            if (anchor == replace) {
                                anchor = exprAfterReplace;
                            }
                        }

                        PsiElement oldElement = commonContainer;
                        if (commonContainer instanceof KtWhenEntry) {
                            KtExpression body = ((KtWhenEntry)commonContainer).getExpression();
                            if (body != null) {
                                oldElement = body;
                            }
                        }
                        else if (commonContainer instanceof KtContainerNode) {
                            KtContainerNode container = (KtContainerNode)commonContainer;
                            PsiElement[] children = container.getChildren();
                            for (PsiElement child : children) {
                                if (child instanceof KtExpression) {
                                    oldElement = child;
                                }
                            }
                        }
                        //ugly logic to make sure we are working with right actual expression
                        KtExpression actualExpression = reference.get();
                        int diff = actualExpression.getTextRange().getStartOffset() - oldElement.getTextRange().getStartOffset();
                        String actualExpressionText = actualExpression.getText();
                        PsiElement newElement = emptyBody.addAfter(oldElement, firstChild);
                        PsiElement elem = findElementByOffsetAndText(diff, actualExpressionText, newElement);
                        if (elem != null) {
                            reference.set((KtExpression)elem);
                        }
                        emptyBody.addAfter(psiFactory.createNewLine(), firstChild);
                        property = (KtProperty)emptyBody.addAfter(property, firstChild);
                        emptyBody.addAfter(psiFactory.createNewLine(), firstChild);
                        actualExpression = reference.get();
                        diff = actualExpression.getTextRange().getStartOffset() - emptyBody.getTextRange().getStartOffset();
                        actualExpressionText = actualExpression.getText();
                        emptyBody = (KtBlockExpression) anchor.replace(emptyBody);
                        elem = findElementByOffsetAndText(diff, actualExpressionText, emptyBody);
                        if (elem != null) {
                            reference.set((KtExpression)elem);
                        }

                        emptyBody.accept(
                                new KtTreeVisitorVoid() {
                                    @Override
                                    public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression) {
                                        if (expression.getCopyableUserData(OCCURRENCE) == null) return;

                                        expression.putCopyableUserData(OCCURRENCE, null);
                                        references.add(expression);
                                    }
                                }
                        );
                    }
                    else {
                        PsiElement parent = anchor.getParent();
                        PsiElement copyTo = parent.getLastChild();
                        PsiElement copyFrom = anchor.getNextSibling();

                        property = (KtProperty)emptyBody.addAfter(property, firstChild);
                        emptyBody.addAfter(psiFactory.createNewLine(), firstChild);
                        if (copyFrom != null && copyTo != null) {
                            emptyBody.addRangeAfter(copyFrom, copyTo, property);
                            parent.deleteChildRange(copyFrom, copyTo);
                        }
                        emptyBody = (KtBlockExpression) anchor.replace(emptyBody);
                    }
                    for (PsiElement child : emptyBody.getChildren()) {
                        if (child instanceof KtProperty) {
                            property = (KtProperty)child;
                        }
                    }
                    if (commonContainer instanceof KtContainerNode) {
                        KtContainerNode node = (KtContainerNode)commonContainer;
                        if (node.getParent() instanceof KtIfExpression) {
                            PsiElement next = node.getNextSibling();
                            if (next != null) {
                                PsiElement nextnext = next.getNextSibling();
                                if (nextnext != null && nextnext.getNode().getElementType() == KtTokens.ELSE_KEYWORD) {
                                    if (next instanceof PsiWhiteSpace) {
                                        next.replace(psiFactory.createWhiteSpace());
                                    }
                                }
                            }
                        }
                    }
                }
                if (!needBraces) {
                    for (int i = 0; i < allReplaces.size(); i++) {
                        KtExpression replace = allReplaces.get(i);

                        if (i != 0 ? replaceOccurrence : shouldReplaceOccurrence(replace, bindingContext, commonContainer)) {
                            replaceExpression(replace, true);
                        }
                        else {
                            PsiElement sibling = PsiTreeUtil.skipSiblingsBackward(replace, PsiWhiteSpace.class);
                            if (sibling == property) {
                                replace.getParent().deleteChildRange(property.getNextSibling(), replace);
                            }
                            else {
                                replace.delete();
                            }
                        }
                    }
                }
                propertyRef.set(property);
                if (noTypeInference) {
                    ShortenReferences.DEFAULT.process(property);
                }
            }

            private PsiElement findElementByOffsetAndText(int offset, String text, PsiElement newContainer) {
                PsiElement elem = newContainer.findElementAt(offset);
                while (elem != null && !(elem instanceof KtExpression && text.equals(elem.getText()))) {
                    elem = elem.getParent();
                }
                return elem;
            }

            private KtExpression replaceExpression(KtExpression replace, boolean addToReferences) {
                boolean isActualExpression = expression == replace;

                KtExpression replacement = psiFactory.createExpression(nameSuggestion);
                KtExpression result;
                if (KtPsiUtilKt.isFunctionLiteralOutsideParentheses(replace)) {
                    KtFunctionLiteralArgument functionLiteralArgument =
                            PsiTreeUtil.getParentOfType(replace, KtFunctionLiteralArgument.class);
                    KtCallExpression newCallExpression = PsiModificationUtilsKt
                            .moveInsideParenthesesAndReplaceWith(functionLiteralArgument, replacement, bindingContext);
                    result = CollectionsKt.last(newCallExpression.getValueArguments()).getArgumentExpression();
                }
                else {
                    result = (KtExpression)replace.replace(replacement);
                }

                PsiElement parent = result != null ? result.getParent() : null;
                if (parent instanceof KtBlockStringTemplateEntry) {
                    RemoveCurlyBracesFromTemplateIntention intention = new RemoveCurlyBracesFromTemplateIntention();
                    KtBlockStringTemplateEntry entry = (KtBlockStringTemplateEntry) parent;
                    KtStringTemplateEntryWithExpression newEntry = intention.isApplicableTo(entry) ? intention.applyTo(entry) : entry;
                    result = newEntry.getExpression();
                }

                if (addToReferences) {
                    references.add(result);
                }
                if (isActualExpression) reference.set(result);

                return result;
            }
        };
    }

    private static PsiElement calculateAnchor(PsiElement commonParent, PsiElement commonContainer,
                                              List<KtExpression> allReplaces) {
        PsiElement anchor = commonParent;
        if (anchor != commonContainer) {
            while (anchor.getParent() != commonContainer) {
                anchor = anchor.getParent();
            }
        }
        else {
            anchor = commonContainer.getFirstChild();
            int startOffset = commonContainer.getTextRange().getEndOffset();
            for (KtExpression expr : allReplaces) {
                int offset = expr.getTextRange().getStartOffset();
                if (offset < startOffset) startOffset = offset;
            }
            while (anchor != null && !anchor.getTextRange().contains(startOffset)) {
                anchor = anchor.getNextSibling();
            }
            if (anchor == null) return null;
        }
        return anchor;
    }

    private static List<KtExpression> findOccurrences(PsiElement occurrenceContainer, @NotNull KtExpression originalExpression) {
        return CollectionsKt.map(
                KotlinPsiRangeKt.toRange(originalExpression).match(occurrenceContainer, KotlinPsiUnifier.DEFAULT),
                new Function1<KotlinPsiRange.Match, KtExpression>() {
                    @Override
                    public KtExpression invoke(KotlinPsiRange.Match match) {
                        PsiElement candidate = ((KotlinPsiRange.ListRange) match.getRange()).getStartElement();
                        if (candidate instanceof KtExpression) return (KtExpression) candidate;
                        if (candidate instanceof KtStringTemplateEntryWithExpression)
                            return ((KtStringTemplateEntryWithExpression) candidate).getExpression();

                        throw new AssertionError("Unexpected candidate element: " + candidate.getText());
                    }
                }
        );
    }

    private static boolean shouldReplaceOccurrence(
            @NotNull KtExpression expression,
            @NotNull BindingContext bindingContext,
            @Nullable PsiElement container
    ) {
        return BindingContextUtilsKt.isUsedAsExpression(expression, bindingContext) || container != expression.getParent();
    }

    @Nullable
    private static PsiElement getContainer(PsiElement place) {
        if (place instanceof KtBlockExpression) {
            return place;
        }
        while (place != null) {
            PsiElement parent = place.getParent();
            if (parent instanceof KtContainerNode) {
                if (!isBadContainerNode((KtContainerNode)parent, place)) {
                    return parent;
                }
            }
            if (parent instanceof KtBlockExpression
                || (parent instanceof KtWhenEntry && place == ((KtWhenEntry) parent).getExpression())) {
                return parent;
            }
            if (parent instanceof KtDeclarationWithBody && ((KtDeclarationWithBody) parent).getBodyExpression() == place) {
                return parent;
            }
            place = parent;
        }
        return null;
    }

    private static boolean isBadContainerNode(KtContainerNode parent, PsiElement place) {
        if (parent.getParent() instanceof KtIfExpression &&
            ((KtIfExpression)parent.getParent()).getCondition() == place) {
            return true;
        }
        else if (parent.getParent() instanceof KtLoopExpression &&
                 ((KtLoopExpression)parent.getParent()).getBody() != place) {
            return true;
        }
        else if (parent.getParent() instanceof KtArrayAccessExpression) {
            return true;
        }
        return false;
    }

    @Nullable
    private static PsiElement getOccurrenceContainer(PsiElement place) {
        PsiElement result = null;
        while (place != null) {
            PsiElement parent = place.getParent();
            if (parent instanceof KtContainerNode) {
                if (!(place instanceof KtBlockExpression) && !isBadContainerNode((KtContainerNode)parent, place)) {
                    result = parent;
                }
            }
            else if (parent instanceof KtClassBody || parent instanceof KtFile) {
                return result;
            }
            else if (parent instanceof KtBlockExpression) {
                result = parent;
            }
            else if (parent instanceof KtWhenEntry) {
                if (!(place instanceof KtBlockExpression)) {
                    result = parent;
                }
            }
            else if (parent instanceof KtDeclarationWithBody) {
                KtDeclarationWithBody function = (KtDeclarationWithBody)parent;
                if (function.getBodyExpression() == place) {
                    if (!(place instanceof KtBlockExpression)) {
                        result = parent;
                    }
                }
            }
            place = parent;
        }
        return null;
    }

    private static void showErrorHint(Project project, Editor editor, String message) {
        CodeInsightUtils.showErrorHint(project, editor, message, INTRODUCE_VARIABLE, HelpID.INTRODUCE_VARIABLE);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
        //do nothing
    }
}
