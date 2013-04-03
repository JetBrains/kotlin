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
import org.jetbrains.jet.lang.psi.JetIfExpression;
import org.jetbrains.jet.plugin.JetBundle;

public class IfStatementWithAssignmentsToExpressionIntention extends BaseIntentionAction {
    public IfStatementWithAssignmentsToExpressionIntention() {
        setText(JetBundle.message("transform.if.statement.with.assignments.to.expression"));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("kotlin.code.transformations");
    }

    @Nullable
    private static JetIfExpression getOriginalExpression(@NotNull Editor editor, @NotNull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(element, JetIfExpression.class, false);
    }

    @Override
    public boolean isAvailable(
            @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        JetIfExpression ifExpression = getOriginalExpression(editor, file);
        return (ifExpression != null) && CodeTransformationUtils.checkIfStatementWithAssignments(ifExpression);
    }

    @Override
    public void invoke(
            @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file
    ) throws IncorrectOperationException {
        JetIfExpression ifExpression = getOriginalExpression(editor, file);
        assert ifExpression != null;
        CodeTransformationUtils.transformIfStatementWithAssignmentsToExpression(ifExpression);
    }
}
