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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.psi.*;

public class MoveWhenElseBranchFix extends KotlinQuickFixAction<KtWhenExpression> {
    public MoveWhenElseBranchFix(@NotNull KtWhenExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("move.when.else.branch.to.the.end.action");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("move.when.else.branch.to.the.end.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }
        return KtPsiUtil.checkWhenExpressionHasSingleElse(getElement());
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtWhenEntry elseEntry = null;
        KtWhenEntry lastEntry = null;
        for (KtWhenEntry entry : getElement().getEntries()) {
            if (entry.isElse()) {
                elseEntry = entry;
            }
            lastEntry = entry;
        }
        assert (elseEntry != null) : "isAvailable should check whether there is only one else branch";
        int cursorOffset = editor.getCaretModel().getOffset() - elseEntry.getTextOffset();

        PsiElement insertedBranch = getElement().addAfter(elseEntry, lastEntry);
        getElement().addAfter(KtPsiFactoryKt.KtPsiFactory(file).createNewLine(), lastEntry);
        getElement().deleteChildRange(elseEntry, elseEntry);
        KtWhenEntry insertedWhenEntry = (KtWhenEntry) CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(insertedBranch);

        editor.getCaretModel().moveToOffset(insertedWhenEntry.getTextOffset() + cursorOffset);
    }

    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public KotlinQuickFixAction createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                KtWhenExpression whenExpression = PsiTreeUtil.getParentOfType(element, KtWhenExpression.class, false);
                if (whenExpression == null) return null;
                return new MoveWhenElseBranchFix(whenExpression);
            }
        };
    }
}
