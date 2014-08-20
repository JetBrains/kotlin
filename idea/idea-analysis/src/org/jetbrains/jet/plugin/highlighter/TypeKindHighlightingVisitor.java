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
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;

class TypeKindHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    TypeKindHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
        PsiReference ref = expression.getReference();
        if (ref == null) return;
        if (JetPsiChecker.isNamesHighlightingEnabled()) {
            DeclarationDescriptor referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
            if (referenceTarget instanceof ConstructorDescriptor) {
                referenceTarget = referenceTarget.getContainingDeclaration();
            }

            if (referenceTarget instanceof ClassDescriptor) {
                highlightName(expression, textAttributesKeyForClass((ClassDescriptor) referenceTarget));
            }
            else if (referenceTarget instanceof TypeParameterDescriptor) {
                highlightName(expression, JetHighlightingColors.TYPE_PARAMETER);
            }
        }
    }

    @Override
    public void visitObjectDeclarationName(@NotNull JetObjectDeclarationName declaration) {
        PsiElement nameIdentifier = declaration.getNameIdentifier();
        if (nameIdentifier != null) {
            highlightName(nameIdentifier, JetHighlightingColors.CLASS);
        }
    }

    @Override
    public void visitTypeParameter(@NotNull JetTypeParameter parameter) {
        PsiElement identifier = parameter.getNameIdentifier();
        if (identifier != null) {
            highlightName(identifier, JetHighlightingColors.TYPE_PARAMETER);
        }
        super.visitTypeParameter(parameter);
    }

    @Override
    public void visitEnumEntry(@NotNull JetEnumEntry enumEntry) {
        // Do nothing, the name was already highlighted in visitObjectDeclarationName
    }

    @Override
    public void visitClass(@NotNull JetClass klass) {
        PsiElement identifier = klass.getNameIdentifier();
        ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, klass);
        if (identifier != null && classDescriptor != null) {
            highlightName(identifier, textAttributesKeyForClass(classDescriptor));
        }
        super.visitClass(klass);
    }

    private void highlightName(@NotNull PsiElement whatToHighlight, @NotNull TextAttributesKey textAttributesKey) {
        JetPsiChecker.highlightName(holder, whatToHighlight, textAttributesKey);
    }

    @NotNull
    private static TextAttributesKey textAttributesKeyForClass(@NotNull ClassDescriptor descriptor) {
        switch (descriptor.getKind()) {
            case TRAIT:
                return JetHighlightingColors.TRAIT;
            case ANNOTATION_CLASS:
                return JetHighlightingColors.ANNOTATION;
            case OBJECT:
                return JetHighlightingColors.OBJECT;
            case ENUM_ENTRY:
                return JetHighlightingColors.INSTANCE_PROPERTY;
            default:
                return descriptor.getModality() == Modality.ABSTRACT
                       ? JetHighlightingColors.ABSTRACT_CLASS
                       : JetHighlightingColors.CLASS;
        }
    }
}
