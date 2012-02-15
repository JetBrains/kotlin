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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 */
public abstract class RemoveRightPartOfBinaryExpressionFix<T extends JetExpression> extends JetIntentionAction<T> {
    public RemoveRightPartOfBinaryExpressionFix(@NotNull T element) {
        super(element);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.right.part.of.binary.expression");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (element instanceof JetBinaryExpression) {
            JetBinaryExpression newElement = (JetBinaryExpression) element.copy();
            element.replace(newElement.getLeft());
        }
        else if (element instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS newElement = (JetBinaryExpressionWithTypeRHS) element.copy();
            element.replace(newElement.getLeft());
        }
    }

    public static JetIntentionActionFactory<JetBinaryExpressionWithTypeRHS> createRemoveCastFactory() {
        return new JetIntentionActionFactory<JetBinaryExpressionWithTypeRHS>() {
            @Override
            public JetIntentionAction<JetBinaryExpressionWithTypeRHS> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetBinaryExpressionWithTypeRHS;
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpressionWithTypeRHS>((JetBinaryExpressionWithTypeRHS) diagnostic.getPsiElement()) {
                    @NotNull
                    @Override
                    public String getText() {
                        return JetBundle.message("remove.cast");
                    }
                };
            }
        };
    }

    public static JetIntentionActionFactory<JetBinaryExpression> createRemoveElvisOperatorFactory() {
        return new JetIntentionActionFactory<JetBinaryExpression>() {
            @Override
            public JetIntentionAction<JetBinaryExpression> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetBinaryExpression;
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpression>((JetBinaryExpression) diagnostic.getPsiElement()) {
                    @NotNull
                    @Override
                    public String getText() {
                        return JetBundle.message("remove.elvis.operator");
                    }
                };
            }
        };
    }
}

