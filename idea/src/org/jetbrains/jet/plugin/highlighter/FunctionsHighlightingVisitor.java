/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;

public class FunctionsHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    public FunctionsHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitNamedFunction(JetNamedFunction function) {
        PsiElement nameIdentifier = function.getNameIdentifier();
        if (nameIdentifier != null) {
            JetPsiChecker.highlightName(holder, nameIdentifier, JetHighlightingColors.FUNCTION_DECLARATION);
        }

        super.visitNamedFunction(function);
    }

    @Override
    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
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
    public void visitCallExpression(JetCallExpression expression) {
        JetExpression callee = expression.getCalleeExpression();
        if (callee instanceof JetReferenceExpression) {
            DeclarationDescriptor calleeDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression)callee);
            if (calleeDescriptor != null) {
                if (calleeDescriptor instanceof ConstructorDescriptor) {
                    JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.CONSTRUCTOR_CALL);
                }
                else if (calleeDescriptor instanceof FunctionDescriptor) {
                    FunctionDescriptor fun = (FunctionDescriptor)calleeDescriptor;
                    JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.FUNCTION_CALL);
                    if (fun.getContainingDeclaration() instanceof NamespaceDescriptor) {
                        JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.NAMESPACE_FUNCTION_CALL);
                    }
                    if (fun.getReceiverParameter() != null) {
                        JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.EXTENSION_FUNCTION_CALL);
                    }
                }
                else if (calleeDescriptor instanceof VariableDescriptor) {
                    String kind = calleeDescriptor instanceof ValueParameterDescriptor
                                  ? "parameter"
                                  : calleeDescriptor instanceof PropertyDescriptor
                                    ? "property"
                                    : "variable";
                    holder.createInfoAnnotation(callee, "Calling " + kind + " as function").setTextAttributes(
                            JetHighlightingColors.VARIABLE_AS_FUNCTION_CALL);
                }
            }
        }

        super.visitCallExpression(expression);
    }
}
