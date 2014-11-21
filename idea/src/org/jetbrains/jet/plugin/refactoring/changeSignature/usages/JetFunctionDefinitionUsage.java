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

package org.jetbrains.jet.plugin.refactoring.changeSignature.usages;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetModifierKeywordToken;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.shorten.ShortenPackage;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeInfo;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetValVar;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class JetFunctionDefinitionUsage extends JetUsageInfo<PsiElement> {
    private final boolean isInherited;
    private final FunctionDescriptor functionDescriptor;
    private final boolean hasExpectedType;

    public JetFunctionDefinitionUsage(
            @NotNull PsiElement function,
            @NotNull FunctionDescriptor functionDescriptor,
            boolean isInherited) {
        super(function);
        this.isInherited = isInherited;
        this.functionDescriptor = functionDescriptor;
        this.hasExpectedType = checkIfHasExpectedType(functionDescriptor);
    }

    private static boolean checkIfHasExpectedType(@NotNull FunctionDescriptor functionDescriptor) {
        if (!(functionDescriptor instanceof AnonymousFunctionDescriptor)) return false;

        JetFunctionLiteral functionLiteral =
                (JetFunctionLiteral) DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor);
        assert functionLiteral != null : "No declaration found for " + functionDescriptor;

        PsiElement parent = functionLiteral.getParent();
        if (!(parent instanceof JetFunctionLiteralExpression)) return false;

        JetFunctionLiteralExpression expression = (JetFunctionLiteralExpression) parent;
        return ResolvePackage.analyze(expression).get(BindingContext.EXPECTED_EXPRESSION_TYPE, expression) != null;
    }

    public final boolean isInherited() {
        return isInherited;
    }

    public final FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
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

            boolean returnTypeIsNeeded = changeInfo.isRefactoringTarget(functionDescriptor)
                                         || !(function instanceof JetFunctionLiteral)
                                         || function.getTypeReference() != null;
            if (changeInfo.isReturnTypeChanged() && returnTypeIsNeeded) {
                function.setTypeReference(null);
                String returnTypeText = changeInfo.getNewReturnTypeText();

                //TODO use ChangeFunctionReturnTypeFix.invoke when JetTypeCodeFragment.getType() is ready
                if (!KotlinBuiltIns.getInstance().getUnitType().toString().equals(returnTypeText)) {
                    ShortenPackage.addToShorteningWaitSet(function.setTypeReference(JetPsiFactory(function).createType(returnTypeText)));
                }
            }
        }
        else {
            parameterList = ((JetClass) element).getPrimaryConstructorParameterList();
        }

        if (changeInfo.isParameterSetOrOrderChanged()) {
            int parametersCount = changeInfo.getNewParametersCount();
            boolean isLambda = element instanceof JetFunctionLiteral;

            JetParameterList newParameterList = null;
            if (isLambda) {
                if (parametersCount == 0 && ((JetFunctionLiteral) element).getTypeReference() == null) {
                    if (parameterList != null) {
                        parameterList.delete();
                        ASTNode arrowNode = ((JetFunctionLiteral)element).getArrowNode();
                        if (arrowNode != null) {
                            arrowNode.getPsi().delete();
                        }
                    }
                }
                else {
                    newParameterList = psiFactory.createFunctionLiteralParameterList(changeInfo.getNewParametersSignature(functionDescriptor, isInherited, hasExpectedType, 0));
                }
            }
            else {
                newParameterList = psiFactory.createParameterList(changeInfo.getNewParametersSignature(functionDescriptor, isInherited, hasExpectedType, 0));
            }

            if (newParameterList != null) {
                if (parameterList != null) {
                    newParameterList = (JetParameterList) parameterList.replace(newParameterList);
                }
                else {
                    if (element instanceof JetClass) {
                        PsiElement anchor = ((JetClass) element).getTypeParameterList();

                        if (anchor == null) {
                            anchor = ((JetClass) element).getNameIdentifier();
                        }
                        if (anchor != null) {
                            newParameterList = (JetParameterList) element.addAfter(newParameterList, anchor);
                        }
                    }
                    else if (isLambda) {
                        //noinspection ConstantConditions
                        JetFunctionLiteral functionLiteral = (JetFunctionLiteral) element;
                        PsiElement anchor = functionLiteral.getLBrace();
                        newParameterList = (JetParameterList) element.addAfter(newParameterList, anchor);
                        if (functionLiteral.getArrowNode() == null) {
                            Pair<PsiElement, PsiElement> whitespaceAndArrow = psiFactory.createWhitespaceAndArrow();
                            element.addRangeAfter(whitespaceAndArrow.getFirst(), whitespaceAndArrow.getSecond(), newParameterList);
                        }
                    }
                }
            }

            if (newParameterList != null) {
                ShortenPackage.addToShorteningWaitSet(newParameterList);
            }
        }
        else if (parameterList != null) {
            int paramIndex = 0;

            for (JetParameter parameter : parameterList.getParameters()) {
                JetParameterInfo parameterInfo = changeInfo.getNewParameters()[paramIndex++];
                changeParameter(changeInfo, parameter, parameterInfo);
            }

            ShortenPackage.addToShorteningWaitSet(parameterList);
        }

        if (changeInfo.isVisibilityChanged() && !JetPsiUtil.isLocal((JetDeclaration) element)) {
            changeVisibility(changeInfo, element);
        }

        return true;
    }

    private static void changeVisibility(JetChangeInfo changeInfo, PsiElement element) {
        JetModifierKeywordToken newVisibilityToken = JetRefactoringUtil.getVisibilityToken(changeInfo.getNewVisibility());

        if (element instanceof JetFunction) {
            ((JetFunction)element).addModifier(newVisibilityToken);
        }
        else {
            ((JetClass)element).addPrimaryConstructorModifier(newVisibilityToken);
        }
    }

    private void changeParameter(JetChangeInfo changeInfo, JetParameter parameter, JetParameterInfo parameterInfo) {
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

        if (parameterInfo.isTypeChanged() && parameter.getTypeReference() != null) {
            JetTypeReference newTypeRef = psiFactory.createType(parameterInfo.getTypeText());
            parameter.setTypeReference(newTypeRef);
        }

        PsiElement identifier = parameter.getNameIdentifier();

        if (identifier != null) {
            String newName = parameterInfo.getInheritedName(isInherited, functionDescriptor, changeInfo.getFunctionDescriptor());
            identifier.replace(psiFactory.createIdentifier(newName));
        }
    }
}
