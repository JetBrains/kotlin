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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;

public class FunctionsHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    public FunctionsHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitNamedFunction(@NotNull KtNamedFunction function) {
        PsiElement nameIdentifier = function.getNameIdentifier();
        if (nameIdentifier != null) {
            NameHighlighter.highlightName(holder, nameIdentifier, KotlinHighlightingColors.FUNCTION_DECLARATION);
        }

        super.visitNamedFunction(function);
    }

    @Override
    public void visitSuperTypeCallEntry(@NotNull KtSuperTypeCallEntry call) {
        KtConstructorCalleeExpression calleeExpression = call.getCalleeExpression();
        KtTypeReference typeRef = calleeExpression.getTypeReference();
        if (typeRef != null) {
            KtTypeElement typeElement = typeRef.getTypeElement();
            if (typeElement instanceof KtUserType) {
                KtSimpleNameExpression nameExpression = ((KtUserType)typeElement).getReferenceExpression();
                if (nameExpression != null) {
                    NameHighlighter.highlightName(holder, nameExpression, KotlinHighlightingColors.CONSTRUCTOR_CALL);
                }
            }
        }
        super.visitSuperTypeCallEntry(call);
    }

    @Override
    public void visitCallExpression(@NotNull KtCallExpression expression) {
        KtExpression callee = expression.getCalleeExpression();
        ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expression, bindingContext);
        if (callee instanceof KtReferenceExpression && resolvedCall != null) {
            CallableDescriptor calleeDescriptor = resolvedCall.getResultingDescriptor();

            if (DynamicCallsKt.isDynamic(calleeDescriptor)) {
                NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.DYNAMIC_FUNCTION_CALL);
            }
            else if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                DeclarationDescriptor container = calleeDescriptor.getContainingDeclaration();
                boolean containedInFunctionClassOrSubclass =
                        container instanceof ClassDescriptor &&
                        FunctionTypesKt.isFunctionTypeOrSubtype(((ClassDescriptor) container).getDefaultType());
                NameHighlighter.highlightName(holder, callee, containedInFunctionClassOrSubclass
                                                              ? KotlinHighlightingColors.VARIABLE_AS_FUNCTION_CALL
                                                              : KotlinHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL);
            }
            else {
                if (calleeDescriptor instanceof ConstructorDescriptor) {
                    NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.CONSTRUCTOR_CALL);
                }
                else if (calleeDescriptor instanceof FunctionDescriptor) {
                    FunctionDescriptor fun = (FunctionDescriptor) calleeDescriptor;
                    NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.FUNCTION_CALL);
                    if (DescriptorUtils.isTopLevelDeclaration(fun)) {
                        NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.PACKAGE_FUNCTION_CALL);
                    }
                    if (fun.getExtensionReceiverParameter() != null) {
                        NameHighlighter.highlightName(holder, callee, KotlinHighlightingColors.EXTENSION_FUNCTION_CALL);
                    }
                }
            }
        }

        super.visitCallExpression(expression);
    }
}
