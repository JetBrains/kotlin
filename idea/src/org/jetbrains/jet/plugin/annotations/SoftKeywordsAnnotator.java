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

/*
 * @author max
 */
package org.jetbrains.jet.plugin.annotations;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetHighlighter;

public class SoftKeywordsAnnotator implements Annotator {
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        element.accept(new JetVisitorVoid() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof LeafPsiElement) {
                    if (JetTokens.SOFT_KEYWORDS.contains(((LeafPsiElement) element).getElementType())) {
                        holder.createInfoAnnotation(element, null).setTextAttributes(JetHighlighter.JET_SOFT_KEYWORD);
                    }
                }
            }

            @Override
            public void visitAnnotationEntry(JetAnnotationEntry annotationEntry) {
                JetTypeReference typeReference = annotationEntry.getTypeReference();
                if (typeReference != null) {
                    JetTypeElement typeElement = typeReference.getTypeElement();
                    markAnnotationIdentifiers(typeElement, holder);
                }
            }

            @Override
            public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
                if (ApplicationManager.getApplication().isUnitTestMode()) return;
                JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
                holder.createInfoAnnotation(functionLiteral.getOpenBraceNode(), null).setTextAttributes(JetHighlighter.JET_FUNCTION_LITERAL_DELIMITER);
                ASTNode closingBraceNode = functionLiteral.getClosingBraceNode();
                if (closingBraceNode != null) {
                    holder.createInfoAnnotation(closingBraceNode, null).setTextAttributes(JetHighlighter.JET_FUNCTION_LITERAL_DELIMITER);
                }
                ASTNode arrowNode = functionLiteral.getArrowNode();
                if (arrowNode != null) {
                    holder.createInfoAnnotation(arrowNode, null).setTextAttributes(JetHighlighter.JET_FUNCTION_LITERAL_DELIMITER);
                }
            }
        });
    }

    private void markAnnotationIdentifiers(JetTypeElement typeElement, @NotNull AnnotationHolder holder) {
        if (typeElement instanceof JetUserType) {
            JetUserType userType = (JetUserType) typeElement;
            if (userType.getQualifier() == null) {
                JetSimpleNameExpression referenceExpression = userType.getReferenceExpression();
                if (referenceExpression != null) {
                    holder.createInfoAnnotation(referenceExpression.getNode(), "Annotation").setTextAttributes(JetHighlighter.JET_SOFT_KEYWORD);
                }
            }
        }
    }
}
