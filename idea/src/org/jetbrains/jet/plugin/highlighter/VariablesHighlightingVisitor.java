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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

class VariablesHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    VariablesHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitJetElement(@NotNull JetElement element) {
        element.acceptChildren(this);
    }

    @Override
    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
        DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, expression);
        if (target == null) {
            return;
        }
        if (target instanceof ValueParameterDescriptor) {
            ValueParameterDescriptor parameterDescriptor = (ValueParameterDescriptor) target;
            if (Boolean.TRUE.equals(bindingContext.get(AUTO_CREATED_IT, parameterDescriptor))) {
                holder.createInfoAnnotation(expression, "Automatically declared based on the expected type").setTextAttributes(
                    JetHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER);
            }
        }

        highlightVariable(expression, target);
        super.visitSimpleNameExpression(expression);
    }

    @Override
    public void visitProperty(@NotNull JetProperty property) {
        visitVariableDeclaration(property);
        super.visitProperty(property);
    }

    @Override
    public void visitParameter(JetParameter parameter) {
        visitVariableDeclaration(parameter);
        super.visitParameter(parameter);
    }

    @Override
    public void visitExpression(@NotNull JetExpression expression) {
        JetType autoCast = bindingContext.get(AUTOCAST, expression);
        if (autoCast != null) {
            holder.createInfoAnnotation(expression, "Automatically cast to " + DescriptorRenderer.TEXT.renderType(autoCast)).setTextAttributes(
                JetHighlightingColors.AUTO_CASTED_VALUE);
        }
        super.visitExpression(expression);
    }

    private void visitVariableDeclaration(JetNamedDeclaration declaration) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, declaration);
        PsiElement nameIdentifier = declaration.getNameIdentifier();
        if (nameIdentifier != null && declarationDescriptor != null) {
            highlightVariable(nameIdentifier, declarationDescriptor);
        }
    }

    private void highlightVariable(@NotNull PsiElement elementToHighlight, @NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof VariableDescriptor) {
            VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
            if (variableDescriptor.isVar()) {
                JetPsiChecker.highlightName(holder, elementToHighlight, JetHighlightingColors.MUTABLE_VARIABLE);
            }

            if (Boolean.TRUE.equals(bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor))) {
                holder.createInfoAnnotation(elementToHighlight, "Wrapped into a reference object to be modified when captured in a closure").setTextAttributes(
                    JetHighlightingColors.WRAPPED_INTO_REF);
            }

            if (descriptor instanceof LocalVariableDescriptor) {
                JetPsiChecker.highlightName(holder, elementToHighlight, JetHighlightingColors.LOCAL_VARIABLE);
            }

            if (descriptor instanceof ValueParameterDescriptor) {
                JetPsiChecker.highlightName(holder, elementToHighlight, JetHighlightingColors.PARAMETER);
            }
        }
    }
}
