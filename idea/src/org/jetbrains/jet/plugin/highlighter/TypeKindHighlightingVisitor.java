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

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author Evgeny Gerashchenko
 * @since 3/29/12
 */
class TypeKindHighlightingVisitor extends HighlightingVisitor {
    protected TypeKindHighlightingVisitor(AnnotationHolder holder) {
        super(holder);
    }

    @Override
    public void visitAnnotationEntry(JetAnnotationEntry annotationEntry) {
        JetTypeReference typeReference = annotationEntry.getTypeReference();
        if (typeReference == null) return;
        JetTypeElement typeElement = typeReference.getTypeElement();
        if (!(typeElement instanceof JetUserType)) return;
        JetUserType userType = (JetUserType)typeElement;
        if (userType.getQualifier() != null) return;
        JetSimpleNameExpression referenceExpression = userType.getReferenceExpression();
        if (referenceExpression != null) {
            holder.createInfoAnnotation(referenceExpression.getNode(), null).setTextAttributes(JetHighlightingColors.ANNOTATION);
        }
    }

    private void visitNameExpression(JetExpression expression) {
        PsiReference ref = expression.getReference();
        if (ref == null) return;
        PsiElement target = ref.resolve();
        if (target instanceof JetClass) {
            highlightClassByKind((JetClass)target, expression);
        } else if (target instanceof JetTypeParameter) {
            holder.createInfoAnnotation(expression, null).setTextAttributes(JetHighlightingColors.TYPE_PARAMETER);
        }
    }

    @Override
    public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
        visitNameExpression(expression);
    }

    @Override
    public void visitQualifiedExpression(JetQualifiedExpression expression) {
        visitNameExpression(expression);
    }

    @Override
    public void visitTypeParameter(JetTypeParameter parameter) {
        PsiElement identifier = parameter.getNameIdentifier();
        if (identifier != null) {
            holder.createInfoAnnotation(identifier, null).setTextAttributes(JetHighlightingColors.TYPE_PARAMETER);
        }
    }

    @Override
    public void visitClass(JetClass klass) {
        PsiElement identifier = klass.getNameIdentifier();
        if (identifier != null) {
            highlightClassByKind(klass, identifier);
        }
    }

    private void highlightClassByKind(@NotNull JetClass klass, @NotNull PsiElement whatToHighlight) {
        TextAttributesKey textAttributes = JetHighlightingColors.CLASS;
        if (klass.isTrait()) {
            textAttributes = JetHighlightingColors.TRAIT;
        } else {
            JetModifierList modifierList = klass.getModifierList();
            if (modifierList != null) {
                if (modifierList.hasModifier(JetTokens.ANNOTATION_KEYWORD)) {
                    textAttributes = JetHighlightingColors.ANNOTATION;
                } else if (modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
                    textAttributes = JetHighlightingColors.ABSTRACT_CLASS;
                }
            }
        }
        Annotation annotation = holder.createInfoAnnotation(whatToHighlight, null);
        annotation.setTextAttributes(textAttributes);
    }
}
