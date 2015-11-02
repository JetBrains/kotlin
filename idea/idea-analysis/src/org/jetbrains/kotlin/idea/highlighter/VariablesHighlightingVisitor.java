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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.CaptureKind;

import static org.jetbrains.kotlin.resolve.BindingContext.*;

class VariablesHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    VariablesHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression) {
        DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, expression);
        if (target == null) {
            return;
        }
        if (target instanceof ValueParameterDescriptor) {
            ValueParameterDescriptor parameterDescriptor = (ValueParameterDescriptor) target;
            if (Boolean.TRUE.equals(bindingContext.get(AUTO_CREATED_IT, parameterDescriptor))) {
                holder.createInfoAnnotation(expression, "Automatically declared based on the expected type").setTextAttributes(
                        KotlinHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER);
            }
        }

        highlightVariable(expression, target);
        super.visitSimpleNameExpression(expression);
    }

    @Override
    public void visitProperty(@NotNull KtProperty property) {
        visitVariableDeclaration(property);
        super.visitProperty(property);
    }

    @Override
    public void visitParameter(@NotNull KtParameter parameter) {
        visitVariableDeclaration(parameter);
        super.visitParameter(parameter);
    }

    @Override
    public void visitExpression(@NotNull KtExpression expression) {
        KotlinType smartCast = bindingContext.get(SMARTCAST, expression);
        if (smartCast != null) {
            holder.createInfoAnnotation(expression, "Smart cast to " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(smartCast)).setTextAttributes(
                    KotlinHighlightingColors.SMART_CAST_VALUE);
        }
        super.visitExpression(expression);
    }

    private void visitVariableDeclaration(KtNamedDeclaration declaration) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, declaration);
        PsiElement nameIdentifier = declaration.getNameIdentifier();
        if (nameIdentifier != null && declarationDescriptor != null) {
            highlightVariable(nameIdentifier, declarationDescriptor);
        }
    }

    private void highlightVariable(@NotNull PsiElement elementToHighlight, @NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof VariableDescriptor) {
            VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;

            if (DynamicCallsKt.isDynamic(variableDescriptor)) {
                NameHighlighter.highlightName(holder, elementToHighlight, KotlinHighlightingColors.DYNAMIC_PROPERTY_CALL);
                return;
            }

            if (variableDescriptor.isVar()) {
                NameHighlighter.highlightName(holder, elementToHighlight, KotlinHighlightingColors.MUTABLE_VARIABLE);
            }

            if (bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor) == CaptureKind.NOT_INLINE) {
                String msg = ((VariableDescriptor) descriptor).isVar()
                             ? "Wrapped into a reference object to be modified when captured in a closure"
                             : "Value captured in a closure";
                holder.createInfoAnnotation(elementToHighlight, msg).setTextAttributes(KotlinHighlightingColors.WRAPPED_INTO_REF);
            }

            if (descriptor instanceof LocalVariableDescriptor) {
                NameHighlighter.highlightName(holder, elementToHighlight, KotlinHighlightingColors.LOCAL_VARIABLE);
            }

            if (descriptor instanceof ValueParameterDescriptor) {
                NameHighlighter.highlightName(holder, elementToHighlight, KotlinHighlightingColors.PARAMETER);
            }
        }
    }
}
