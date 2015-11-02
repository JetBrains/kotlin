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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.psi.*;

public class AddWhenElseBranchFix extends KotlinQuickFixAction<KtWhenExpression> {
    private static final String ELSE_ENTRY_TEXT = "else -> {}";

    public AddWhenElseBranchFix(@NotNull KtWhenExpression element) {
        super(element);
    }


    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("add.when.else.branch.action");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("add.when.else.branch.action.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && getElement().getCloseBrace() != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        PsiElement whenCloseBrace = getElement().getCloseBrace();
        assert (whenCloseBrace != null) : "isAvailable should check if close brace exist";

        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(file);
        KtWhenEntry entry = psiFactory.createWhenEntry(ELSE_ENTRY_TEXT);

        PsiElement insertedBranch = getElement().addBefore(entry, whenCloseBrace);
        getElement().addAfter(psiFactory.createNewLine(), insertedBranch);

        KtWhenEntry insertedWhenEntry = (KtWhenEntry) CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(insertedBranch);
        TextRange textRange = insertedWhenEntry.getTextRange();

        int indexOfOpenBrace = insertedWhenEntry.getText().indexOf('{');
        editor.getCaretModel().moveToOffset(textRange.getStartOffset() + indexOfOpenBrace + 1);
    }

    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public KotlinQuickFixAction createAction(@NotNull Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                KtWhenExpression whenExpression = PsiTreeUtil.getParentOfType(element, KtWhenExpression.class, false);
                if (whenExpression == null) return null;
                return new AddWhenElseBranchFix(whenExpression);
            }
        };
    }
}
