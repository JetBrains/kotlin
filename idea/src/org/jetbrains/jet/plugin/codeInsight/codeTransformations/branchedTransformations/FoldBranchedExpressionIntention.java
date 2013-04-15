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

package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.plugin.JetBundle;

public class FoldBranchedExpressionIntention extends BaseIntentionAction {
    public FoldBranchedExpressionIntention() {
        setText(JetBundle.message("fold.branched.expression"));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("fold.branched.expression.family");
    }

    @Nullable
    private static JetExpression getTarget(@NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        return (JetExpression)JetPsiUtil.getParentByTypeAndPredicate(element, JetElement.class, BranchedFoldingUtils.FOLDABLE_EXPRESSION, false);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return getTarget(editor, file) != null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) throws IncorrectOperationException {
        JetExpression target = getTarget(editor, file);

        assert target != null;

        BranchedFoldingUtils.foldExpression(target);
    }
}
