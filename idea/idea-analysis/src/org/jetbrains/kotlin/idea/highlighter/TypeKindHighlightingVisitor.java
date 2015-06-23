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
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.BindingContext;

class TypeKindHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    TypeKindHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
        PsiReference ref = expression.getReference();
        if (ref == null) return;
        if (JetPsiChecker.Companion.getNamesHighlightingEnabled()) {
            DeclarationDescriptor referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
            if (referenceTarget instanceof ConstructorDescriptor) {
                referenceTarget = referenceTarget.getContainingDeclaration();
            }

            if (referenceTarget instanceof ClassDescriptor) {
                TextAttributesKey textAttributesKey = textAttributesKeyForClass((ClassDescriptor) referenceTarget);
                if (textAttributesKey == JetHighlightingColors.ANNOTATION) {
                    highlightAnnotation(expression);
                }
                else {
                    highlightName(expression, textAttributesKey);
                }
            }
            else if (referenceTarget instanceof TypeParameterDescriptor) {
                highlightName(expression, JetHighlightingColors.TYPE_PARAMETER);
            }
        }
    }

    private void highlightAnnotation(@NotNull JetSimpleNameExpression expression) {
        TextRange toHighlight = PsiUtilPackage.getCalleeHighlightingRange(expression);
        JetPsiChecker.highlightName(holder, toHighlight, JetHighlightingColors.ANNOTATION);
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
    public void visitClassOrObject(@NotNull JetClassOrObject classOrObject) {
        PsiElement identifier = classOrObject.getNameIdentifier();
        ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject);
        if (identifier != null && classDescriptor != null) {
            highlightName(identifier, textAttributesKeyForClass(classDescriptor));
        }
        super.visitClassOrObject(classOrObject);
    }

    @Override
    public void visitDynamicType(@NotNull JetDynamicType type) {
        // Do nothing: 'dynamic' is highlighted as a keyword
    }

    private void highlightName(@NotNull PsiElement whatToHighlight, @NotNull TextAttributesKey textAttributesKey) {
        JetPsiChecker.highlightName(holder, whatToHighlight, textAttributesKey);
    }

    @NotNull
    private static TextAttributesKey textAttributesKeyForClass(@NotNull ClassDescriptor descriptor) {
        switch (descriptor.getKind()) {
            case INTERFACE:
                return JetHighlightingColors.TRAIT;
            case ANNOTATION_CLASS:
                return JetHighlightingColors.ANNOTATION;
            case OBJECT:
                return JetHighlightingColors.OBJECT;
            case ENUM_ENTRY:
                return JetHighlightingColors.ENUM_ENTRY;
            default:
                return descriptor.getModality() == Modality.ABSTRACT
                       ? JetHighlightingColors.ABSTRACT_CLASS
                       : JetHighlightingColors.CLASS;
        }
    }
}
