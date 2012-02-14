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

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetLabelQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetPrefixExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetHighlighter;

public class LabelsAnnotator implements Annotator {
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
//        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        element.accept(new JetVisitorVoid() {
            @Override
            public void visitPrefixExpression(JetPrefixExpression expression) {
                JetSimpleNameExpression operationSign = expression.getOperationReference();
                if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
                    holder.createInfoAnnotation(operationSign, null).setTextAttributes(JetHighlighter.JET_LABEL_IDENTIFIER);
                }
            }

            @Override
            public void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression) {
                JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                if (targetLabel != null) {
                    holder.createInfoAnnotation(targetLabel, null).setTextAttributes(JetHighlighter.JET_LABEL_IDENTIFIER);
                }
            }

        });
    }
}
