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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;

class PropertiesHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    PropertiesHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
        // TODO highlight extension properties
        DeclarationDescriptor target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (!(target instanceof PropertyDescriptor)) {
            return;
        }

        boolean namespace = target.getContainingDeclaration() instanceof NamespaceDescriptor;
        if (expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
            holder.createInfoAnnotation(expression, null).setTextAttributes(
                (namespace ?
                 JetHighlightingColors.NAMESPACE_BACKING_FIELD_ACCESS :
                 JetHighlightingColors.INSTANCE_BACKING_FIELD_ACCESS)
            );
        } else {
            putPropertyAnnotation(expression, namespace, false);
        }
    }

    @Override
    public void visitProperty(@NotNull JetProperty property) {
        PsiElement nameIdentifier = property.getNameIdentifier();
        if (nameIdentifier == null) return;
        VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        if (propertyDescriptor instanceof PropertyDescriptor) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor)propertyDescriptor);
            boolean namespace = propertyDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor;
            putPropertyAnnotation(nameIdentifier, namespace, Boolean.TRUE.equals(backingFieldRequired));
        }
    }

    @Override
    public void visitParameter(@NotNull JetParameter parameter) {
        PsiElement nameIdentifier = parameter.getNameIdentifier();
        if (nameIdentifier == null) return;
        PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
        if (propertyDescriptor != null) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
            putPropertyAnnotation(nameIdentifier, false, Boolean.TRUE.equals(backingFieldRequired));
        }
    }

    @Override
    public void visitJetElement(@NotNull JetElement element) {
        element.acceptChildren(this);
    }

    private void putPropertyAnnotation(@NotNull PsiElement elementToHighlight, boolean namespace, boolean withBackingField) {
        holder.createInfoAnnotation(
            elementToHighlight,
            "This property has a backing field")
            .setTextAttributes(withBackingField ?
                (namespace
                 ? JetHighlightingColors.NAMESPACE_PROPERTY_WITH_BACKING_FIELD
                 : JetHighlightingColors.INSTANCE_PROPERTY_WITH_BACKING_FIELD) :
                (namespace
                 ? JetHighlightingColors.NAMESPACE_PROPERTY
                 : JetHighlightingColors.INSTANCE_PROPERTY));
    }
}
