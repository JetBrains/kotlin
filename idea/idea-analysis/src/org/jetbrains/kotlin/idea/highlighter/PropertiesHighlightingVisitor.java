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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.psi.KtThisExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;

class PropertiesHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    PropertiesHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression) {
        if (expression.getParent() instanceof KtThisExpression) {
            return;
        }
        DeclarationDescriptor target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (target instanceof SyntheticFieldDescriptor) {
            NameHighlighter.highlightName(holder, expression, KotlinHighlightingColors.BACKING_FIELD_VARIABLE);
            return;
        }
        if (!(target instanceof PropertyDescriptor)) {
            return;
        }

        highlightProperty(expression, (PropertyDescriptor) target, false);
    }

    @Override
    public void visitProperty(@NotNull KtProperty property) {
        PsiElement nameIdentifier = property.getNameIdentifier();
        if (nameIdentifier == null) return;
        VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        if (propertyDescriptor instanceof PropertyDescriptor) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor)propertyDescriptor);
            highlightProperty(nameIdentifier, (PropertyDescriptor) propertyDescriptor, Boolean.TRUE.equals(backingFieldRequired));
        }

        super.visitProperty(property);
    }

    @Override
    public void visitParameter(@NotNull KtParameter parameter) {
        PsiElement nameIdentifier = parameter.getNameIdentifier();
        if (nameIdentifier == null) return;
        PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
        if (propertyDescriptor != null) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
            highlightProperty(nameIdentifier, propertyDescriptor, Boolean.TRUE.equals(backingFieldRequired));
        }

        super.visitParameter(parameter);
    }

    private void highlightProperty(
            @NotNull PsiElement elementToHighlight,
            @NotNull PropertyDescriptor descriptor,
            boolean withBackingField
    ) {
        if (DynamicCallsKt.isDynamic(descriptor)) {
            NameHighlighter.highlightName(holder, elementToHighlight, KotlinHighlightingColors.DYNAMIC_PROPERTY_CALL);
            return;
        }

        boolean isStatic = DescriptorUtils.isStaticDeclaration(descriptor);
        NameHighlighter.highlightName(
                holder, elementToHighlight,
                isStatic ? KotlinHighlightingColors.PACKAGE_PROPERTY : KotlinHighlightingColors.INSTANCE_PROPERTY
        );
        if (descriptor.getExtensionReceiverParameter() != null) {
            NameHighlighter.highlightName(holder, elementToHighlight, KotlinHighlightingColors.EXTENSION_PROPERTY);
        }
        if (withBackingField) {
            holder.createInfoAnnotation(elementToHighlight, "This property has a backing field")
                    .setTextAttributes(KotlinHighlightingColors.PROPERTY_WITH_BACKING_FIELD);
        }
    }
}
