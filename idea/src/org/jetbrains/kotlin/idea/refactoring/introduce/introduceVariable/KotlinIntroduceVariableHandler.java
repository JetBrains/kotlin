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
import kotlin.KotlinPackage;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.analyzer.AnalyzerPackage;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.core.CorePackage;
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester;
import org.jetbrains.kotlin.idea.intentions.ConvertToBlockBodyIntention;
import org.jetbrains.kotlin.idea.intentions.RemoveCurlyBracesFromTemplateIntention;
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringUtil;
import org.jetbrains.kotlin.idea.refactoring.introduce.IntroducePackage;
import org.jetbrains.kotlin.idea.refactoring.introduce.KotlinIntroduceHandlerBase;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange;
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiUnifier;
import org.jetbrains.kotlin.idea.util.psi.patternMatching.PatternMatchingPackage;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.ObservableBindingTrace;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class KotlinIntroduceVariableHandler extends KotlinIntroduceHandlerBase {

    public static final String INTRODUCE_VARIABLE = JetRefactoringBundle.message("introduce.variable");

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file, DataContext dataContext) {
        JetRefactoringUtil.SelectExpressionCallback callback = new JetRefactoringUtil.SelectExpressionCallback() {
            @Override
            public void run(@Nullable JetExpression expression) {
                doRefactoring(project, editor, expression, null, null);
            }
        };
        try {
            JetRefactoringUtil.selectExpression(editor, file, callback);
        }
        catch (JetRefactoringUtil.IntroduceRefactoringException e) {
            showErrorHint(project, editor, e.getMessage());
        }
    }


    public static void doRefactoring(
            @NotNull final Project project,
            @Nullable final Editor editor,
            @Nullable JetExpression _expression,
            @Nullable List<JetExpression> occurrencesToReplace,
            @Nullable final Function1<JetProperty, Unit> onNonInteractiveFinish
    ) {
        if (_expression == null) {
            showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
            return;
        }
        if (_expression.getParent() instanceof JetParenthesizedExpression) {
            _expression = (JetExpression)_expression.getParent();
        }
        final JetExpression expression = _expression;
        boolean noTypeInference = false;
        if (expression.getParent() instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression)expression.getParent();
            if (qualifiedExpression.getReceiverExpression() != expression) {
                showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
                return;
            }
        }
        else if (expression instanceof JetStatementExpression) {
            showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
            return;
        }
        else if (expression.getParent() instanceof JetOperationExpression) {
            JetOperationExpression operationExpression = (JetOperationExpression)expression.getParent();
            if (operationExpression.getOperationReference() == expression) {
                showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
                return;
            }
        }

        AnalysisResult analysisResult = ResolvePackage.analyzeAndGetResult(expression);
        final BindingContext bindingContext = analysisResult.getBindingContext();
        final JetType expressionType = bindingContext.getType(expression); //can be null or error type
        JetScope scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, expression);
        if (scope != null) {
            DataFlowInfo dataFlowInfo = BindingContextUtilPackage.getDataFlowInfo(bindingContext, expression);

            ObservableBindingTrace bindingTrace = new ObservableBindingTrace(new BindingTraceContext());
            JetType typeNoExpectedType = AnalyzerPackage.computeTypeInfoInContext(
                    expression, scope, bindingTrace, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE, analysisResult.getModuleDescriptor()
            ).getType();
            if (expressionType != null && typeNoExpectedType != null && !JetTypeChecker.DEFAULT.equalTypes(expressionType,
                                                                                                           typeNoExpectedType)) {
                noTypeInference = true;
            }
        }

        if (expressionType == null && bindingContext.get(BindingContext.QUALIFIER, expression) != null) {
            showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.package.expression"));
            return;
        }
        if (expressionType != null &&
            JetTypeChecker.DEFAULT.equalTypes(analysisResult.getModuleDescriptor().getBuiltIns().getUnitType(), expressionType)) {
            showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.expression.has.unit.type"));
            return;
        }
        if (expressionType == null && noTypeInference) {
            showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.expression.should.have.inferred.type"));
            return;
        }
        final PsiElement container = getContainer(expression);
        PsiElement occurrenceContainer = getOccurrenceContainer(expression);
        if (container == null) {
            showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.container"));
            return;
        }
        final boolean isInplaceAvailableOnDataContext =
            editor != null &&
            editor.getSettings().isVariableInplaceRenameEnabled() &&
            !ApplicationManager.getApplication().isUnitTestMode();

        final List<JetExpression> allOccurrences;
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
                List<JetExpression> allReplaces;
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
                        expression, ResolvePackage.analyze(expression, BodyResolveMode.PARTIAL), validator, "value");
                final Ref<JetProperty> propertyRef = new Ref<JetProperty>();
                final ArrayList<JetExpression> references = new ArrayList<JetExpression>();
                final Ref<JetExpression> reference = new Ref<JetExpression>();
                final Runnable introduceRunnable = introduceVariable(
                        expression, suggestedNames.iterator().next(), allReplaces, commonContainer,
                        commonParent, replaceOccurrence, propertyRef, references,
                        reference, finalNoTypeInference, expressionType, bindingContext);
                CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(introduceRunnable);
                        JetProperty property = propertyRef.get();
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
                                                                                references.toArray(new JetExpression[references.size()]),
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
            OccurrencesChooser.<JetExpression>simpleChooser(editor).
                showChooser(expression, allOccurrences, callback);
        }
        else {
            callback.pass(OccurrencesChooser.ReplaceChoice.ALL);
        }
    }

    private static Runnable introduceVariable(
            final JetExpression expression,
            final String nameSuggestion,
            final List<JetExpression> allReplaces,
            final PsiElement commonContainer,
            final PsiElement commonParent,
            final boolean replaceOccurrence,
            final Ref<JetProperty> propertyRef,
            final ArrayList<JetExpression> references,
            final Ref<JetExpression> reference,
            final boolean noTypeInference,
            final JetType expressionType,
            final BindingContext bindingContext
    ) {
        final JetPsiFactory psiFactory = JetPsiFactory(expression);
        return new Runnable() {
            @Override
            public void run() {
                if (commonContainer instanceof JetDeclarationWithBody) {
                    JetDeclarationWithBody originalDeclaration = (JetDeclarationWithBody) commonContainer;
                    final JetExpression originalBody = originalDeclaration.getBodyExpression();
                    assert originalBody != null : "Original body is not found: " + originalDeclaration;

                    Key<Boolean> EXPRESSION_KEY = Key.create("EXPRESSION_KEY");
                    Key<Boolean> REPLACE_KEY = Key.create("REPLACE_KEY");
                    Key<Boolean> COMMON_PARENT_KEY = Key.create("COMMON_PARENT_KEY");
                    expression.putCopyableUserData(EXPRESSION_KEY, true);
                    for (JetExpression replace : allReplaces) {
                        replace.putCopyableUserData(REPLACE_KEY, true);
                    }
                    commonParent.putCopyableUserData(COMMON_PARENT_KEY, true);

                    JetDeclarationWithBody newDeclaration = ConvertToBlockBodyIntention.Companion.convert(originalDeclaration);

                    JetBlockExpression newCommonContainer = (JetBlockExpression) newDeclaration.getBodyExpression();
                    assert newCommonContainer != null : "New body is not found: " + newDeclaration;

                    JetExpression newExpression = IntroducePackage.findExpressionByCopyableDataAndClearIt(newCommonContainer, EXPRESSION_KEY);
                    PsiElement newCommonParent = IntroducePackage.findElementByCopyableDataAndClearIt(newCommonContainer, COMMON_PARENT_KEY);
                    List<JetExpression> newAllReplaces = IntroducePackage.findExpressionsByCopyableDataAndClearIt(newCommonContainer, REPLACE_KEY);

                    run(newExpression, newCommonContainer, newCommonParent, newAllReplaces);
                }
                else {
                    run(expression, commonContainer, commonParent, allReplaces);
                }
            }

            private void run(
                    JetExpression expression,
                    PsiElement commonContainer,
                    PsiElement commonParent,
                    List<JetExpression> allReplaces
            ) {
                String variableText = "val " + nameSuggestion;
                if (noTypeInference) {
                    variableText += ": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(expressionType);
                }
                variableText += " = ";
                if (expression instanceof JetParenthesizedExpression) {
                    JetParenthesizedExpression parenthesizedExpression = (JetParenthesizedExpression)expression;
                    JetExpression innerExpression = parenthesizedExpression.getExpression();
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
                JetProperty property = psiFactory.createProperty(variableText);
                PsiElement anchor = calculateAnchor(commonParent, commonContainer, allReplaces);
                if (anchor == null) return;
                boolean needBraces = !(commonContainer instanceof JetBlockExpression ||
                                       commonContainer instanceof JetClassBody ||
                                       commonContainer instanceof JetClassInitializer);
                if (!needBraces) {
                    property = (JetProperty)commonContainer.addBefore(property, anchor);
                    commonContainer.addBefore(psiFactory.createNewLine(), anchor);
                }
                else {
                    JetExpression emptyBody = psiFactory.createEmptyBody();
                    PsiElement firstChild = emptyBody.getFirstChild();
                    emptyBody.addAfter(psiFactory.createNewLine(), firstChild);

                    if (replaceOccurrence && commonContainer != null) {
                        for (JetExpression replace : allReplaces) {
                            JetExpression exprAfterReplace = replaceExpression(replace);
                            if (anchor == replace) {
                                anchor = exprAfterReplace;
                            }
                        }

                        PsiElement oldElement = commonContainer;
                        if (commonContainer instanceof JetWhenEntry) {
                            JetExpression body = ((JetWhenEntry)commonContainer).getExpression();
                            if (body != null) {
                                oldElement = body;
                            }
                        }
                        else if (commonContainer instanceof JetContainerNode) {
                            JetContainerNode container = (JetContainerNode)commonContainer;
                            PsiElement[] children = container.getChildren();
                            for (PsiElement child : children) {
                                if (child instanceof JetExpression) {
                                    oldElement = child;
                                }
                            }
                        }
                        //ugly logic to make sure we are working with right actual expression
                        JetExpression actualExpression = reference.get();
                        int diff = actualExpression.getTextRange().getStartOffset() - oldElement.getTextRange().getStartOffset();
                        String actualExpressionText = actualExpression.getText();
                        PsiElement newElement = emptyBody.addAfter(oldElement, firstChild);
                        PsiElement elem = findElementByOffsetAndText(diff, actualExpressionText, newElement);
                        if (elem != null) {
                            reference.set((JetExpression)elem);
                        }
                        emptyBody.addAfter(psiFactory.createNewLine(), firstChild);
                        property = (JetProperty)emptyBody.addAfter(property, firstChild);
                        emptyBody.addAfter(psiFactory.createNewLine(), firstChild);
                        actualExpression = reference.get();
                        diff = actualExpression.getTextRange().getStartOffset() - emptyBody.getTextRange().getStartOffset();
                        actualExpressionText = actualExpression.getText();
                        emptyBody = (JetBlockExpression) anchor.replace(emptyBody);
                        elem = findElementByOffsetAndText(diff, actualExpressionText, emptyBody);
                        if (elem != null) {
                            reference.set((JetExpression)elem);
                        }
                    }
                    else {
                        PsiElement parent = anchor.getParent();
                        PsiElement copyTo = parent.getLastChild();
                        PsiElement copyFrom = anchor.getNextSibling();

                        property = (JetProperty)emptyBody.addAfter(property, firstChild);
                        emptyBody.addAfter(psiFactory.createNewLine(), firstChild);
                        if (copyFrom != null && copyTo != null) {
                            emptyBody.addRangeAfter(copyFrom, copyTo, property);
                            parent.deleteChildRange(copyFrom, copyTo);
                        }
                        emptyBody = (JetBlockExpression) anchor.replace(emptyBody);
                    }
                    for (PsiElement child : emptyBody.getChildren()) {
                        if (child instanceof JetProperty) {
                            property = (JetProperty)child;
                        }
                    }
                    if (commonContainer instanceof JetContainerNode) {
                        JetContainerNode node = (JetContainerNode)commonContainer;
                        if (node.getParent() instanceof JetIfExpression) {
                            PsiElement next = node.getNextSibling();
                            if (next != null) {
                                PsiElement nextnext = next.getNextSibling();
                                if (nextnext != null && nextnext.getNode().getElementType() == JetTokens.ELSE_KEYWORD) {
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
                        JetExpression replace = allReplaces.get(i);

                        if (i != 0 ? replaceOccurrence : shouldReplaceOccurrence(replace, bindingContext, commonContainer)) {
                            replaceExpression(replace);
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
                while (elem != null && !(elem instanceof JetExpression && text.equals(elem.getText()))) {
                    elem = elem.getParent();
                }
                return elem;
            }

            private JetExpression replaceExpression(JetExpression replace) {
                boolean isActualExpression = expression == replace;

                JetExpression replacement = psiFactory.createExpression(nameSuggestion);
                JetExpression result;
                if (PsiUtilPackage.isFunctionLiteralOutsideParentheses(replace)) {
                    JetFunctionLiteralArgument functionLiteralArgument =
                            PsiTreeUtil.getParentOfType(replace, JetFunctionLiteralArgument.class);
                    JetCallExpression newCallExpression = CorePackage
                            .moveInsideParenthesesAndReplaceWith(functionLiteralArgument, replacement, bindingContext);
                    result = KotlinPackage.last(newCallExpression.getValueArguments()).getArgumentExpression();
                }
                else {
                    result = (JetExpression)replace.replace(replacement);
                }

                PsiElement parent = result != null ? result.getParent() : null;
                if (parent instanceof JetBlockStringTemplateEntry) {
                    RemoveCurlyBracesFromTemplateIntention intention = new RemoveCurlyBracesFromTemplateIntention();
                    JetBlockStringTemplateEntry entry = (JetBlockStringTemplateEntry) parent;
                    JetStringTemplateEntryWithExpression newEntry = intention.isApplicableTo(entry) ? intention.applyTo(entry) : entry;
                    result = newEntry.getExpression();
                }

                references.add(result);
                if (isActualExpression) reference.set(result);

                return result;
            }
        };
    }

    private static PsiElement calculateAnchor(PsiElement commonParent, PsiElement commonContainer,
                                              List<JetExpression> allReplaces) {
        PsiElement anchor = commonParent;
        if (anchor != commonContainer) {
            while (anchor.getParent() != commonContainer) {
                anchor = anchor.getParent();
            }
        }
        else {
            anchor = commonContainer.getFirstChild();
            int startOffset = commonContainer.getTextRange().getEndOffset();
            for (JetExpression expr : allReplaces) {
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

    private static List<JetExpression> findOccurrences(PsiElement occurrenceContainer, @NotNull JetExpression originalExpression) {
        return KotlinPackage.map(
                PatternMatchingPackage.toRange(originalExpression).match(occurrenceContainer, JetPsiUnifier.DEFAULT),
                new Function1<JetPsiRange.Match, JetExpression>() {
                    @Override
                    public JetExpression invoke(JetPsiRange.Match match) {
                        PsiElement candidate = ((JetPsiRange.ListRange) match.getRange()).getStartElement();
                        if (candidate instanceof JetExpression) return (JetExpression) candidate;
                        if (candidate instanceof JetStringTemplateEntryWithExpression)
                            return ((JetStringTemplateEntryWithExpression) candidate).getExpression();

                        throw new AssertionError("Unexpected candidate element: " + candidate.getText());
                    }
                }
        );
    }

    private static boolean shouldReplaceOccurrence(
            @NotNull JetExpression expression,
            @NotNull BindingContext bindingContext,
            @Nullable PsiElement container
    ) {
        return BindingContextUtilPackage.isUsedAsExpression(expression, bindingContext) || container != expression.getParent();
    }

    @Nullable
    private static PsiElement getContainer(PsiElement place) {
        if (place instanceof JetBlockExpression || place instanceof JetClassBody ||
            place instanceof JetClassInitializer) {
            return place;
        }
        while (place != null) {
            PsiElement parent = place.getParent();
            if (parent instanceof JetContainerNode) {
                if (!isBadContainerNode((JetContainerNode)parent, place)) {
                    return parent;
                }
            }
            if (parent instanceof JetBlockExpression || parent instanceof JetWhenEntry ||
                parent instanceof JetClassBody || parent instanceof JetClassInitializer) {
                return parent;
            }
            if (parent instanceof JetDeclarationWithBody && ((JetDeclarationWithBody) parent).getBodyExpression() == place) {
                return parent;
            }
            place = parent;
        }
        return null;
    }

    private static boolean isBadContainerNode(JetContainerNode parent, PsiElement place) {
        if (parent.getParent() instanceof JetIfExpression &&
            ((JetIfExpression)parent.getParent()).getCondition() == place) {
            return true;
        }
        else if (parent.getParent() instanceof JetLoopExpression &&
                 ((JetLoopExpression)parent.getParent()).getBody() != place) {
            return true;
        }
        else if (parent.getParent() instanceof JetArrayAccessExpression) {
            return true;
        }
        return false;
    }

    @Nullable
    private static PsiElement getOccurrenceContainer(PsiElement place) {
        PsiElement result = null;
        while (place != null) {
            PsiElement parent = place.getParent();
            if (parent instanceof JetContainerNode) {
                if (!(place instanceof JetBlockExpression) && !isBadContainerNode((JetContainerNode)parent, place)) {
                    result = parent;
                }
            }
            else if (parent instanceof JetClassBody || parent instanceof JetFile || parent instanceof JetClassInitializer) {
                if (result == null) {
                    return parent;
                }
                else {
                    return result;
                }
            }
            else if (parent instanceof JetBlockExpression) {
                result = parent;
            }
            else if (parent instanceof JetWhenEntry) {
                if (!(place instanceof JetBlockExpression)) {
                    result = parent;
                }
            }
            else if (parent instanceof JetDeclarationWithBody) {
                JetDeclarationWithBody function = (JetDeclarationWithBody)parent;
                if (function.getBodyExpression() == place) {
                    if (!(place instanceof JetBlockExpression)) {
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
