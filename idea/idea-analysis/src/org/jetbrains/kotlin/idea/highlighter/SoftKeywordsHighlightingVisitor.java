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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtFunctionLiteral;
import org.jetbrains.kotlin.psi.KtFunctionLiteralExpression;
import org.jetbrains.kotlin.psi.KtVisitorVoid;

class SoftKeywordsHighlightingVisitor extends KtVisitorVoid {
    private final AnnotationHolder holder;

    SoftKeywordsHighlightingVisitor(AnnotationHolder holder) {
        this.holder = holder;
    }

    @Override
    public void visitElement(PsiElement element) {
        if (element instanceof LeafPsiElement) {
            IElementType elementType = ((LeafPsiElement)element).getElementType();
            if (KtTokens.SOFT_KEYWORDS.contains(elementType)) {
                TextAttributesKey attributes = KotlinHighlightingColors.KEYWORD;
                if (KtTokens.MODIFIER_KEYWORDS.contains(elementType)) {
                    attributes = KotlinHighlightingColors.BUILTIN_ANNOTATION;
                }
                holder.createInfoAnnotation(element, null).setTextAttributes(attributes);
            }
            if (KtTokens.SAFE_ACCESS.equals(elementType)) {
                holder.createInfoAnnotation(element, null).setTextAttributes(KotlinHighlightingColors.SAFE_ACCESS);
            }
        }
    }

    @Override
    public void visitFunctionLiteralExpression(@NotNull KtFunctionLiteralExpression expression) {
        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        KtFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        holder.createInfoAnnotation(functionLiteral.getLBrace(), null).setTextAttributes(KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
        PsiElement closingBrace = functionLiteral.getRBrace();
        if (closingBrace != null) {
            holder.createInfoAnnotation(closingBrace, null).setTextAttributes(KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
        }
        PsiElement arrow = functionLiteral.getArrow();
        if (arrow != null) {
            holder.createInfoAnnotation(arrow, null).setTextAttributes(KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
        }
    }
}
