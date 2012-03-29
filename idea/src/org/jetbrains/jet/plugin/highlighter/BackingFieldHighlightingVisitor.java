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
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;

class BackingFieldHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    BackingFieldHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitProperty(@NotNull JetProperty property) {
        VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        if (propertyDescriptor instanceof PropertyDescriptor) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor)propertyDescriptor);
            if (Boolean.TRUE.equals(backingFieldRequired)) {
                putBackingFieldAnnotation(holder, property);
            }
        }
    }

    @Override
    public void visitParameter(@NotNull JetParameter parameter) {
        PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
        if (propertyDescriptor != null) {
            Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
            if (Boolean.TRUE.equals(backingFieldRequired)) {
                putBackingFieldAnnotation(holder, parameter);
            }
        }
    }

    @Override
    public void visitJetElement(@NotNull JetElement element) {
        element.acceptChildren(this);
    }

    private static void putBackingFieldAnnotation(@NotNull AnnotationHolder holder, @NotNull JetNamedDeclaration element) {
        PsiElement nameIdentifier = element.getNameIdentifier();
        if (nameIdentifier != null) {
            holder.createInfoAnnotation(
                nameIdentifier,
                "This property has a backing field")
                .setTextAttributes(JetHighlightingColors.INSTANCE_PROPERTY_WITH_BACKING_FIELD);
        }
    }
}
