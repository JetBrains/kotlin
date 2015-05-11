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

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.KotlinPackage;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersPackage;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options;
import org.jetbrains.kotlin.idea.codeInsight.shorten.ShortenPackage;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringUtil;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ChangeSignaturePackage;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetValVar;

import java.util.List;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class JetFunctionDefinitionUsage<T extends PsiElement> extends JetUsageInfo<T> {
    @NotNull
    private final FunctionDescriptor originalFunctionDescriptor;

    private FunctionDescriptor currentFunctionDescriptor;

    @NotNull
    private final JetFunctionDefinitionUsage<? extends PsiElement> baseFunction;

    private final boolean hasExpectedType;

    @Nullable
    private final JetType samCallType;

    @Nullable
    private TypeSubstitutor typeSubstitutor;

    public JetFunctionDefinitionUsage(
            @NotNull T function,
            @NotNull FunctionDescriptor originalFunctionDescriptor,
            @Nullable JetFunctionDefinitionUsage<PsiElement> baseFunction,
            @Nullable JetType samCallType) {
        super(function);
        this.originalFunctionDescriptor = originalFunctionDescriptor;
        this.baseFunction = baseFunction != null ? baseFunction : this;
        this.hasExpectedType = checkIfHasExpectedType(originalFunctionDescriptor, isInherited());
        this.samCallType = samCallType;
    }

    private static boolean checkIfHasExpectedType(@NotNull FunctionDescriptor functionDescriptor, boolean isInherited) {
        if (!(functionDescriptor instanceof AnonymousFunctionDescriptor && isInherited)) return false;

        JetFunctionLiteral functionLiteral =
                (JetFunctionLiteral) DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor);
        assert functionLiteral != null : "No declaration found for " + functionDescriptor;

        PsiElement parent = functionLiteral.getParent();
        if (!(parent instanceof JetFunctionLiteralExpression)) return false;

        JetFunctionLiteralExpression expression = (JetFunctionLiteralExpression) parent;
        return ResolvePackage.analyze(expression, BodyResolveMode.PARTIAL).get(BindingContext.EXPECTED_EXPRESSION_TYPE, expression) != null;
    }

    @NotNull
    public JetFunctionDefinitionUsage getBaseFunction() {
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
                typeSubstitutor = ChangeSignaturePackage.getFunctionSubstitutor(baseFunction, this);
            }
            else {
                DeclarationDescriptor currentBaseDescriptor = baseFunction.getCurrentFunctionDescriptor();
                DeclarationDescriptor classDescriptor = currentBaseDescriptor != null
                                                        ? currentBaseDescriptor.getContainingDeclaration()
                                                        : null;

                if (!(classDescriptor instanceof ClassDescriptor)) return null;

                typeSubstitutor = ChangeSignaturePackage.getTypeSubstitutor(
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
    public final FunctionDescriptor getOriginalFunctionDescriptor() {
        return originalFunctionDescriptor;
    }

    @Nullable
    public final FunctionDescriptor getCurrentFunctionDescriptor() {
        if (currentFunctionDescriptor == null) {
            PsiElement element = getDeclaration();

            if (element instanceof JetFunction) {
                currentFunctionDescriptor = (FunctionDescriptor) ResolvePackage.resolveToDescriptor((JetFunction) element);
            }
            else if (element instanceof JetClass) {
                currentFunctionDescriptor = ((ClassDescriptor) ResolvePackage.resolveToDescriptor((JetClass) element)).getUnsubstitutedPrimaryConstructor();
            }
            else if (element instanceof PsiMethod) {
                currentFunctionDescriptor = ResolvePackage.getJavaMethodDescriptor((PsiMethod) element);
            }
        }
        return currentFunctionDescriptor;
    }

    @Override
    public boolean processUsage(JetChangeInfo changeInfo, PsiElement element) {
        JetParameterList parameterList;

        JetPsiFactory psiFactory = JetPsiFactory(element.getProject());
        if (element instanceof JetFunction) {
            JetFunction function = (JetFunction) element;
            parameterList = function.getValueParameterList();

            if (changeInfo.isNameChanged()) {
                PsiElement identifier = function.getNameIdentifier();

                if (identifier != null) {
                    identifier.replace(psiFactory.createIdentifier(changeInfo.getNewName()));
                }
            }

            boolean returnTypeIsNeeded = (changeInfo.isRefactoringTarget(originalFunctionDescriptor)
                                         || !(function instanceof JetFunctionLiteral)
                                         || function.getTypeReference() != null) &&
                                         !(function instanceof JetSecondaryConstructor);
            if (changeInfo.isReturnTypeChanged() && returnTypeIsNeeded) {
                function.setTypeReference(null);
                String returnTypeText = changeInfo.renderReturnType((JetFunctionDefinitionUsage<PsiElement>) this);

                //TODO use ChangeFunctionReturnTypeFix.invoke when JetTypeCodeFragment.getType() is ready
                if (!KotlinBuiltIns.getInstance().getUnitType().toString().equals(returnTypeText)) {
                    ShortenPackage.addToShorteningWaitSet(
                            function.setTypeReference(JetPsiFactory(function).createType(returnTypeText)),
                            Options.DEFAULT
                    );
                }
            }
        }
        else {
            parameterList = ((JetClass) element).getPrimaryConstructorParameterList();
        }

        if (changeInfo.isParameterSetOrOrderChanged()) {
            processParameterListWithStructuralChanges(changeInfo, element, parameterList, psiFactory);
        }
        else if (parameterList != null) {
            int paramIndex = 0;

            for (JetParameter parameter : parameterList.getParameters()) {
                JetParameterInfo parameterInfo = changeInfo.getNewParameters()[paramIndex];
                changeParameter(paramIndex, parameter, parameterInfo);
                paramIndex++;
            }

            ShortenPackage.addToShorteningWaitSet(parameterList, Options.DEFAULT);
        }

        if (element instanceof JetFunction && changeInfo.isReceiverTypeChanged()) {
            //noinspection unchecked
            String receiverTypeText = changeInfo.renderReceiverType((JetFunctionDefinitionUsage<PsiElement>) this);
            JetTypeReference receiverTypeRef = receiverTypeText != null ? psiFactory.createType(receiverTypeText) : null;
            JetTypeReference newReceiverTypeRef = TypeRefHelpersPackage.setReceiverTypeReference((JetFunction) element, receiverTypeRef);
            if (newReceiverTypeRef != null) {
                ShortenPackage.addToShorteningWaitSet(newReceiverTypeRef, ShortenReferences.Options.DEFAULT);
            }
        }

        if (changeInfo.isVisibilityChanged() && !JetPsiUtil.isLocal((JetDeclaration) element)) {
            changeVisibility(changeInfo, element);
        }

        return true;
    }

    private void processParameterListWithStructuralChanges(
            JetChangeInfo changeInfo,
            PsiElement element,
            JetParameterList parameterList,
            JetPsiFactory psiFactory
    ) {
        int parametersCount = changeInfo.getNonReceiverParametersCount();
        boolean isLambda = element instanceof JetFunctionLiteral;
        boolean canReplaceEntireList = false;

        JetParameterList newParameterList = null;
        if (isLambda) {
            if (parametersCount == 0 && ((JetFunctionLiteral) element).getTypeReference() == null) {
                if (parameterList != null) {
                    parameterList.delete();
                    PsiElement arrow = ((JetFunctionLiteral)element).getArrow();
                    if (arrow != null) {
                        arrow.delete();
                    }
                    parameterList = null;
                }
            }
            else {
                newParameterList = psiFactory.createFunctionLiteralParameterList(changeInfo.getNewParametersSignature(
                        (JetFunctionDefinitionUsage<PsiElement>) this)
                );
                canReplaceEntireList = true;
            }
        }
        else {
            newParameterList = psiFactory.createParameterList(changeInfo.getNewParametersSignature(
                    (JetFunctionDefinitionUsage<PsiElement>) this)
            );
        }

        if (newParameterList == null) return;

        if (parameterList != null) {
            if (canReplaceEntireList) {
                newParameterList = (JetParameterList) parameterList.replace(newParameterList);
            }
            else {
                newParameterList = replaceParameterListAndKeepDelimiters(parameterList, newParameterList);
            }
        }
        else {
            if (element instanceof JetClass) {
                PsiElement anchor = ((JetClass) element).getTypeParameterList();

                if (anchor == null) {
                    anchor = ((JetClass) element).getNameIdentifier();
                }
                if (anchor != null) {
                    JetPrimaryConstructor constructor =
                            (JetPrimaryConstructor) element.addAfter(psiFactory.createPrimaryConstructor(), anchor);
                    JetParameterList oldParameterList = constructor.getValueParameterList();
                    assert oldParameterList != null : "primary constructor from factory has parameter list";
                    newParameterList = (JetParameterList) oldParameterList.replace(newParameterList);
                }
            }
            else if (isLambda) {
                //noinspection ConstantConditions
                JetFunctionLiteral functionLiteral = (JetFunctionLiteral) element;
                PsiElement anchor = functionLiteral.getLBrace();
                newParameterList = (JetParameterList) element.addAfter(newParameterList, anchor);
                if (functionLiteral.getArrow() == null) {
                    Pair<PsiElement, PsiElement> whitespaceAndArrow = psiFactory.createWhitespaceAndArrow();
                    element.addRangeAfter(whitespaceAndArrow.getFirst(), whitespaceAndArrow.getSecond(), newParameterList);
                }
            }
        }

        if (newParameterList != null) {
            ShortenPackage.addToShorteningWaitSet(newParameterList, Options.DEFAULT);
        }
    }

    private static JetParameterList replaceParameterListAndKeepDelimiters(JetParameterList parameterList, JetParameterList newParameterList) {
        List<JetParameter> oldParameters = parameterList.getParameters();
        List<JetParameter> newParameters = newParameterList.getParameters();
        int oldCount = oldParameters.size();
        int newCount = newParameters.size();

        int commonCount = Math.min(oldCount, newCount);
        for (int i = 0; i < commonCount; i++) {
            oldParameters.set(i, (JetParameter) oldParameters.get(i).replace(newParameters.get(i)));
        }

        if (commonCount == 0) return (JetParameterList) parameterList.replace(newParameterList);

        if (oldCount > commonCount) {
            parameterList.deleteChildRange(oldParameters.get(commonCount - 1).getNextSibling(),
                                           KotlinPackage.last(oldParameters));
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
        JetModifierKeywordToken newVisibilityToken = JetRefactoringUtil.getVisibilityToken(changeInfo.getNewVisibility());

        if (element instanceof JetFunction) {
            ((JetFunction)element).addModifier(newVisibilityToken);
        }
        else {
            JetPrimaryConstructor constructor = ((JetClass) element).getPrimaryConstructor();
            assert constructor != null : "Primary constructor should be created before changing visibility";
            constructor.addModifier(newVisibilityToken);
        }
    }

    private void changeParameter(int parameterIndex, JetParameter parameter, JetParameterInfo parameterInfo) {
        ASTNode valOrVarAstNode = parameter.getValOrVarNode();
        PsiElement valOrVarNode = valOrVarAstNode != null ? valOrVarAstNode.getPsi() : null;
        JetValVar valOrVar = parameterInfo.getValOrVar();

        JetPsiFactory psiFactory = JetPsiFactory(getProject());
        if (valOrVarNode != null) {
            if (valOrVar == JetValVar.None) {
                valOrVarNode.delete();
            }
            else {
                valOrVarNode.replace(psiFactory.createValOrVarNode(valOrVar.toString()).getPsi());
            }
        }
        else if (valOrVar != JetValVar.None) {
            PsiElement firstChild = parameter.getFirstChild();
            parameter.addBefore(psiFactory.createValOrVarNode(valOrVar.toString()).getPsi(), firstChild);
            parameter.addBefore(psiFactory.createWhiteSpace(), firstChild);
        }

        if (parameterInfo.getIsTypeChanged() && parameter.getTypeReference() != null) {
            String renderedType = parameterInfo.renderType(parameterIndex, this);
            parameter.setTypeReference(psiFactory.createType(renderedType));
        }

        PsiElement identifier = parameter.getNameIdentifier();

        if (identifier != null) {
            //noinspection unchecked
            String newName = parameterInfo.getInheritedName((JetFunctionDefinitionUsage<PsiElement>) this);
            identifier.replace(psiFactory.createIdentifier(newName));
        }
    }
}
