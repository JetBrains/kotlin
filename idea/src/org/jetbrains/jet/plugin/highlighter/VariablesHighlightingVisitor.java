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

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

class VariablesHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    VariablesHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
        DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, expression);
        if (target instanceof ValueParameterDescriptor) {
            ValueParameterDescriptor parameterDescriptor = (ValueParameterDescriptor) target;
            if (Boolean.TRUE.equals(bindingContext.get(AUTO_CREATED_IT, parameterDescriptor))) {
                holder.createInfoAnnotation(expression, "Automatically declared based on the expected type").setTextAttributes(
                    JetHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER);
            }
        }

        markVariableAsWrappedIfNeeded(expression.getNode(), target);
        super.visitSimpleNameExpression(expression);
    }

    private void markVariableAsWrappedIfNeeded(@NotNull ASTNode node, DeclarationDescriptor target) {
        if (target instanceof VariableDescriptor) {
            VariableDescriptor variableDescriptor = (VariableDescriptor) target;
            if (Boolean.TRUE.equals(bindingContext.get(MUST_BE_WRAPPED_IN_A_REF, variableDescriptor))) {
                holder.createInfoAnnotation(node, "Wrapped into a ref-object to be modifier when captured in a closure").setTextAttributes(
                    JetHighlightingColors.WRAPPED_INTO_REF);
            }

        }
    }

    @Override
    public void visitProperty(@NotNull JetProperty property) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, property);
        PsiElement nameIdentifier = property.getNameIdentifier();
        if (nameIdentifier != null) {
            markVariableAsWrappedIfNeeded(nameIdentifier.getNode(), declarationDescriptor);
        }
        super.visitProperty(property);
    }

    @Override
    public void visitExpression(@NotNull JetExpression expression) {
        JetType autoCast = bindingContext.get(AUTOCAST, expression);
        if (autoCast != null) {
            holder.createInfoAnnotation(expression, "Automatically cast to " + autoCast).setTextAttributes(
                JetHighlightingColors.AUTO_CASTED_VALUE);
        }
        expression.acceptChildren(this);
    }

    @Override
    public void visitJetElement(@NotNull JetElement element) {
        element.acceptChildren(this);
    }
}
