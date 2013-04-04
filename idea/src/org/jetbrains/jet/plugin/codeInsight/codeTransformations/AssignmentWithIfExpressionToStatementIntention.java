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

package org.jetbrains.jet.plugin.codeInsight.codeTransformations;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.plugin.JetBundle;

public class AssignmentWithIfExpressionToStatementIntention extends BaseIntentionAction {
    public AssignmentWithIfExpressionToStatementIntention() {
        setText(JetBundle.message("transform.assignment.with.if.expression.to.statement"));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("kotlin.code.transformations");
    }

    @Nullable
    private static JetBinaryExpression getAssignment(@NotNull Editor editor, @NotNull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);

        while (element != null) {
            if (CodeTransformationUtils.checkAssignmentWithIfExpression(element)) return (JetBinaryExpression)element;
            PsiElement parent = PsiTreeUtil.getParentOfType(element, JetBinaryExpression.class, false);
            element = (element != parent) ? parent : null;
        }

        return null;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return getAssignment(editor, file) != null;
    }

    @Override
    public void invoke(
            @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file
    ) throws IncorrectOperationException {
        JetBinaryExpression assignment = getAssignment(editor, file);
        assert assignment != null;
        CodeTransformationUtils.transformAssignmentWithIfExpressionToStatement(assignment);
    }
}
