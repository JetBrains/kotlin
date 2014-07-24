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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetWhenEntry;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class MoveWhenElseBranchFix extends JetIntentionAction<JetWhenExpression> {
    public MoveWhenElseBranchFix(@NotNull JetWhenExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("move.when.else.branch.to.the.end.action");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("move.when.else.branch.to.the.end.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }
        return JetPsiUtil.checkWhenExpressionHasSingleElse(element);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetWhenEntry elseEntry = null;
        JetWhenEntry lastEntry = null;
        for (JetWhenEntry entry : element.getEntries()) {
            if (entry.isElse()) {
                elseEntry = entry;
            }
            lastEntry = entry;
        }
        assert (elseEntry != null) : "isAvailable should check whether there is only one else branch";
        int cursorOffset = editor.getCaretModel().getOffset() - elseEntry.getTextOffset();

        PsiElement insertedBranch = element.addAfter(elseEntry, lastEntry);
        element.addAfter(JetPsiFactory(file).createNewLine(), lastEntry);
        element.deleteChildRange(elseEntry, elseEntry);
        JetWhenEntry insertedWhenEntry = (JetWhenEntry) CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(insertedBranch);

        editor.getCaretModel().moveToOffset(insertedWhenEntry.getTextOffset() + cursorOffset);
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                JetWhenExpression whenExpression = PsiTreeUtil.getParentOfType(element, JetWhenExpression.class, false);
                if (whenExpression == null) return null;
                return new MoveWhenElseBranchFix(whenExpression);
            }
        };
    }
}
