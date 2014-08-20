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

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

public class FunctionsHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    public FunctionsHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitNamedFunction(@NotNull JetNamedFunction function) {
        PsiElement nameIdentifier = function.getNameIdentifier();
        if (nameIdentifier != null) {
            JetPsiChecker.highlightName(holder, nameIdentifier, JetHighlightingColors.FUNCTION_DECLARATION);
        }

        super.visitNamedFunction(function);
    }

    @Override
    public void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call) {
        JetConstructorCalleeExpression calleeExpression = call.getCalleeExpression();
        JetTypeReference typeRef = calleeExpression.getTypeReference();
        if (typeRef != null) {
            JetTypeElement typeElement = typeRef.getTypeElement();
            if (typeElement instanceof JetUserType) {
                JetSimpleNameExpression nameExpression = ((JetUserType)typeElement).getReferenceExpression();
                if (nameExpression != null) {
                    JetPsiChecker.highlightName(holder, nameExpression, JetHighlightingColors.CONSTRUCTOR_CALL);
                }
            }
        }
        super.visitDelegationToSuperCallSpecifier(call);
    }

    @Override
    public void visitCallExpression(@NotNull JetCallExpression expression) {
        JetExpression callee = expression.getCalleeExpression();
        ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(expression, bindingContext);
        if (callee instanceof JetReferenceExpression && resolvedCall != null) {
            DeclarationDescriptor calleeDescriptor = resolvedCall.getResultingDescriptor();
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                JetPsiChecker.highlightName(holder, callee, containedInFunctionClassOrSubclass(calleeDescriptor)
                                                            ? JetHighlightingColors.VARIABLE_AS_FUNCTION_CALL
                                                            : JetHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL);
            }
            else {
                if (calleeDescriptor instanceof ConstructorDescriptor) {
                    JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.CONSTRUCTOR_CALL);
                }
                else if (calleeDescriptor instanceof FunctionDescriptor) {
                    FunctionDescriptor fun = (FunctionDescriptor) calleeDescriptor;
                    JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.FUNCTION_CALL);
                    if (DescriptorUtils.isTopLevelDeclaration(fun)) {
                        JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.PACKAGE_FUNCTION_CALL);
                    }
                    if (fun.getReceiverParameter() != null) {
                        JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.EXTENSION_FUNCTION_CALL);
                    }
                }
            }
        }

        super.visitCallExpression(expression);
    }

    private static boolean containedInFunctionClassOrSubclass(DeclarationDescriptor calleeDescriptor) {
        DeclarationDescriptor parent = calleeDescriptor.getContainingDeclaration();
        if (!(parent instanceof ClassDescriptor)) {
            return false;
        }

        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        JetType defaultType = ((ClassDescriptor) parent).getDefaultType();

        if (builtIns.isFunctionOrExtensionFunctionType(defaultType)) {
            return true;
        }

        for (JetType supertype : TypeUtils.getAllSupertypes(defaultType)) {
            if (builtIns.isFunctionOrExtensionFunctionType(supertype)) {
                return true;
            }
        }

        return false;
    }
}
