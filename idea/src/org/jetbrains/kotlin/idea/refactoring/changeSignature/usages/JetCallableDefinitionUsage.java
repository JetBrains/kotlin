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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import kotlin.CollectionsKt;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.JavaResolutionUtils;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.codeInsight.shorten.ShortenWaitingSetKt;
import org.jetbrains.kotlin.idea.core.DescriptorUtilsKt;
import org.jetbrains.kotlin.idea.core.PsiModificationUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ChangeSignatureUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetValVar;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.substitutions.SubstitutionUtilsKt;

import java.util.List;

import static org.jetbrains.kotlin.idea.core.refactoring.JetRefactoringUtilKt.createPrimaryConstructorIfAbsent;

public class JetCallableDefinitionUsage<T extends PsiElement> extends JetUsageInfo<T> {
    @NotNull
    private final CallableDescriptor originalCallableDescriptor;

    private CallableDescriptor currentCallableDescriptor;

    @NotNull
    private final JetCallableDefinitionUsage<? extends PsiElement> baseFunction;

    private final boolean hasExpectedType;

    @Nullable
    private final KotlinType samCallType;

    @Nullable
    private TypeSubstitutor typeSubstitutor;

    public JetCallableDefinitionUsage(
            @NotNull T function,
            @NotNull CallableDescriptor originalCallableDescriptor,
            @Nullable JetCallableDefinitionUsage<PsiElement> baseFunction,
            @Nullable KotlinType samCallType
    ) {
        super(function);
        this.originalCallableDescriptor = originalCallableDescriptor;
        this.baseFunction = baseFunction != null ? baseFunction : this;
        this.hasExpectedType = checkIfHasExpectedType(originalCallableDescriptor, isInherited());
        this.samCallType = samCallType;
    }

    private static boolean checkIfHasExpectedType(@NotNull CallableDescriptor callableDescriptor, boolean isInherited) {
        if (!(callableDescriptor instanceof AnonymousFunctionDescriptor && isInherited)) return false;

        KtFunctionLiteral functionLiteral =
                (KtFunctionLiteral) DescriptorToSourceUtils.descriptorToDeclaration(callableDescriptor);
        assert functionLiteral != null : "No declaration found for " + callableDescriptor;

        PsiElement parent = functionLiteral.getParent();
        if (!(parent instanceof KtFunctionLiteralExpression)) return false;

        KtFunctionLiteralExpression expression = (KtFunctionLiteralExpression) parent;
        return ResolutionUtils.analyze(expression, BodyResolveMode.PARTIAL).get(BindingContext.EXPECTED_EXPRESSION_TYPE, expression) != null;
    }

    @NotNull
    public JetCallableDefinitionUsage<?> getBaseFunction() {
        return baseFunction;
    }

    @NotNull
    public PsiElement getDeclaration() {
        //noinspection ConstantConditions
        return getElement();
    }

    @Nullable
    public TypeSubstitutor getOrCreateTypeSubstitutor() {
        if (!isInherited()) return null;

        if (typeSubstitutor == null) {
            if (samCallType == null) {
                typeSubstitutor = ChangeSignatureUtilsKt.getCallableSubstitutor(baseFunction, this);
            }
            else {
                DeclarationDescriptor currentBaseDescriptor = baseFunction.getCurrentCallableDescriptor();
                DeclarationDescriptor classDescriptor = currentBaseDescriptor != null
                                                        ? currentBaseDescriptor.getContainingDeclaration()
                                                        : null;

                if (!(classDescriptor instanceof ClassDescriptor)) return null;

                typeSubstitutor = SubstitutionUtilsKt.getTypeSubstitutor(
                        ((ClassDescriptor) classDescriptor).getDefaultType(),
                        samCallType
                );
            }
        }
        return typeSubstitutor;
    }

    public final boolean isInherited() {
        return baseFunction != this;
    }

    public boolean hasExpectedType() {
        return hasExpectedType;
    }

    @NotNull
    public final CallableDescriptor getOriginalCallableDescriptor() {
        return originalCallableDescriptor;
    }

    @Nullable
    public final CallableDescriptor getCurrentCallableDescriptor() {
        if (currentCallableDescriptor == null) {
            PsiElement element = getDeclaration();

            if (element instanceof KtFunction || element instanceof KtProperty || element instanceof KtParameter) {
                currentCallableDescriptor = (CallableDescriptor) ResolutionUtils.resolveToDescriptor((KtDeclaration) element);
            }
            else if (element instanceof KtClass) {
                currentCallableDescriptor = ((ClassDescriptor) ResolutionUtils.resolveToDescriptor((KtClass) element)).getUnsubstitutedPrimaryConstructor();
            }
            else if (element instanceof PsiMethod) {
                currentCallableDescriptor = JavaResolutionUtils.getJavaMethodDescriptor((PsiMethod) element);
            }
        }
        return currentCallableDescriptor;
    }

    @Override
    public boolean processUsage(@NotNull JetChangeInfo changeInfo, @NotNull T element, @NotNull UsageInfo[] allUsages) {
        if (!(element instanceof KtNamedDeclaration)) return true;

        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(element.getProject());

        if (changeInfo.isNameChanged()) {
            PsiElement identifier = ((KtCallableDeclaration) element).getNameIdentifier();

            if (identifier != null) {
                identifier.replace(psiFactory.createIdentifier(changeInfo.getNewName()));
            }
        }

        changeReturnTypeIfNeeded(changeInfo, element);

        KtParameterList parameterList = KtPsiUtilKt.getValueParameterList((KtNamedDeclaration) element);

        if (changeInfo.isParameterSetOrOrderChanged()) {
            processParameterListWithStructuralChanges(changeInfo, element, parameterList, psiFactory);
        }
        else if (parameterList != null) {
            int paramIndex = originalCallableDescriptor.getExtensionReceiverParameter() != null ? 1 : 0;

            for (KtParameter parameter : parameterList.getParameters()) {
                JetParameterInfo parameterInfo = changeInfo.getNewParameters()[paramIndex];
                changeParameter(paramIndex, parameter, parameterInfo);
                paramIndex++;
            }

            ShortenWaitingSetKt.addToShorteningWaitSet(parameterList, Options.DEFAULT);
        }

        if (element instanceof KtCallableDeclaration && changeInfo.isReceiverTypeChanged()) {
            //noinspection unchecked
            String receiverTypeText = changeInfo.renderReceiverType((JetCallableDefinitionUsage<PsiElement>) this);
            KtTypeReference receiverTypeRef = receiverTypeText != null ? psiFactory.createType(receiverTypeText) : null;
            KtTypeReference newReceiverTypeRef = TypeRefHelpersKt
                    .setReceiverTypeReference((KtCallableDeclaration) element, receiverTypeRef);
            if (newReceiverTypeRef != null) {
                ShortenWaitingSetKt.addToShorteningWaitSet(newReceiverTypeRef, ShortenReferences.Options.DEFAULT);
            }
        }

        if (changeInfo.isVisibilityChanged() && !KtPsiUtil.isLocal((KtDeclaration) element)) {
            changeVisibility(changeInfo, element);
        }

        return true;
    }

    protected void changeReturnTypeIfNeeded(JetChangeInfo changeInfo, PsiElement element) {
        if (!(element instanceof KtCallableDeclaration)) return;
        if (element instanceof KtConstructor) return;

        KtCallableDeclaration callable = (KtCallableDeclaration) element;

        boolean returnTypeIsNeeded;
        if (element instanceof KtFunction) {
            returnTypeIsNeeded = !(callable instanceof KtFunctionLiteral)
                                 && (changeInfo.isRefactoringTarget(originalCallableDescriptor) || callable.getTypeReference() != null);
        }
        else {
            returnTypeIsNeeded = element instanceof KtProperty || element instanceof KtParameter;
        }

        if (changeInfo.isReturnTypeChanged() && returnTypeIsNeeded) {
            callable.setTypeReference(null);
            String returnTypeText = changeInfo.renderReturnType((JetCallableDefinitionUsage<PsiElement>) this);

            //TODO use ChangeFunctionReturnTypeFix.invoke when JetTypeCodeFragment.getType() is ready
            if (!(returnTypeText.equals("Unit") || returnTypeText.equals("kotlin.Unit"))) {
                ShortenWaitingSetKt.addToShorteningWaitSet(
                        callable.setTypeReference(KtPsiFactoryKt.KtPsiFactory(callable).createType(returnTypeText)),
                        Options.DEFAULT
                );
            }
        }
    }

    private void processParameterListWithStructuralChanges(
            JetChangeInfo changeInfo,
            PsiElement element,
            KtParameterList parameterList,
            KtPsiFactory psiFactory
    ) {
        int parametersCount = changeInfo.getNonReceiverParametersCount();
        boolean isLambda = element instanceof KtFunctionLiteral;
        boolean canReplaceEntireList = false;

        KtParameterList newParameterList = null;
        if (isLambda) {
            if (parametersCount == 0) {
                if (parameterList != null) {
                    parameterList.delete();
                    PsiElement arrow = ((KtFunctionLiteral)element).getArrow();
                    if (arrow != null) {
                        arrow.delete();
                    }
                    parameterList = null;
                }
            }
            else {
                newParameterList = psiFactory.createFunctionLiteralParameterList(changeInfo.getNewParametersSignatureWithoutParentheses(
                        (JetCallableDefinitionUsage<PsiElement>) this)
                );
                canReplaceEntireList = true;
            }
        }
        else if (!(element instanceof KtProperty || element instanceof KtParameter)) {
            newParameterList = psiFactory.createParameterList(changeInfo.getNewParametersSignature(
                    (JetCallableDefinitionUsage<PsiElement>) this)
            );
        }

        if (newParameterList == null) return;

        if (parameterList != null) {
            if (canReplaceEntireList) {
                newParameterList = (KtParameterList) parameterList.replace(newParameterList);
            }
            else {
                newParameterList = replaceParameterListAndKeepDelimiters(parameterList, newParameterList);
            }
        }
        else {
            if (element instanceof KtClass) {
                KtPrimaryConstructor constructor = createPrimaryConstructorIfAbsent((KtClass) element);
                KtParameterList oldParameterList = constructor.getValueParameterList();
                assert oldParameterList != null : "primary constructor from factory has parameter list";
                newParameterList = (KtParameterList) oldParameterList.replace(newParameterList);
            }
            else if (isLambda) {
                //noinspection ConstantConditions
                KtFunctionLiteral functionLiteral = (KtFunctionLiteral) element;
                PsiElement anchor = functionLiteral.getLBrace();
                newParameterList = (KtParameterList) element.addAfter(newParameterList, anchor);
                if (functionLiteral.getArrow() == null) {
                    Pair<PsiElement, PsiElement> whitespaceAndArrow = psiFactory.createWhitespaceAndArrow();
                    element.addRangeAfter(whitespaceAndArrow.getFirst(), whitespaceAndArrow.getSecond(), newParameterList);
                }
            }
        }

        if (newParameterList != null) {
            ShortenWaitingSetKt.addToShorteningWaitSet(newParameterList, Options.DEFAULT);
        }
    }

    private static KtParameterList replaceParameterListAndKeepDelimiters(KtParameterList parameterList, KtParameterList newParameterList) {
        List<KtParameter> oldParameters = parameterList.getParameters();
        List<KtParameter> newParameters = newParameterList.getParameters();
        int oldCount = oldParameters.size();
        int newCount = newParameters.size();

        int commonCount = Math.min(oldCount, newCount);
        for (int i = 0; i < commonCount; i++) {
            oldParameters.set(i, (KtParameter) oldParameters.get(i).replace(newParameters.get(i)));
        }

        if (commonCount == 0) return (KtParameterList) parameterList.replace(newParameterList);

        if (oldCount > commonCount) {
            parameterList.deleteChildRange(oldParameters.get(commonCount - 1).getNextSibling(),
                                           CollectionsKt.last(oldParameters));
        }
        else if (newCount > commonCount) {
            parameterList.addRangeAfter(newParameters.get(commonCount - 1).getNextSibling(),
                                        newParameterList.getLastChild().getPrevSibling(),
                                        PsiTreeUtil.skipSiblingsBackward(parameterList.getLastChild(),
                                                                         PsiWhiteSpace.class, PsiComment.class));
        }

        return parameterList;
    }

    private static void changeVisibility(JetChangeInfo changeInfo, PsiElement element) {
        KtModifierKeywordToken newVisibilityToken = DescriptorUtilsKt.toKeywordToken(changeInfo.getNewVisibility());

        if (element instanceof KtCallableDeclaration) {
            PsiModificationUtilsKt.setVisibility((KtCallableDeclaration)element, newVisibilityToken);
        }
        else if (element instanceof KtClass) {
            PsiModificationUtilsKt.setVisibility(createPrimaryConstructorIfAbsent((KtClass) element), newVisibilityToken);
        }
        else throw new AssertionError("Invalid element: " + PsiUtilsKt.getElementTextWithContext(element));
    }

    private void changeParameter(int parameterIndex, KtParameter parameter, JetParameterInfo parameterInfo) {
        PsiElement valOrVarKeyword = parameter.getValOrVarKeyword();
        JetValVar valOrVar = parameterInfo.getValOrVar();

        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(getProject());
        if (valOrVarKeyword != null) {
            PsiElement newKeyword = valOrVar.createKeyword(psiFactory);
            if (newKeyword != null) {
                valOrVarKeyword.replace(newKeyword);
            }
            else {
                valOrVarKeyword.delete();
            }
        }
        else if (valOrVar != JetValVar.None) {
            PsiElement firstChild = parameter.getFirstChild();
            //noinspection ConstantConditions
            parameter.addBefore(valOrVar.createKeyword(psiFactory), firstChild);
            parameter.addBefore(psiFactory.createWhiteSpace(), firstChild);
        }

        if (parameterInfo.isTypeChanged() && parameter.getTypeReference() != null) {
            String renderedType = parameterInfo.renderType(parameterIndex, this);
            parameter.setTypeReference(psiFactory.createType(renderedType));
        }

        PsiElement identifier = parameter.getNameIdentifier();

        if (identifier != null) {
            //noinspection unchecked
            String newName = parameterInfo.getInheritedName(this);
            identifier.replace(psiFactory.createIdentifier(newName));
        }
    }
}
