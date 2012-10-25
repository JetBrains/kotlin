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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.plugin.JetBundle;

public class AddWhenElseBranchFix extends JetIntentionAction<JetWhenExpression> {
    public AddWhenElseBranchFix(@NotNull JetWhenExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("add.when.else.branch.action");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.when.else.branch.action.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && element.getCloseBraceNode() != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        PsiElement insertBeforeAnchor = element.getCloseBraceNode();
        if (insertBeforeAnchor != null) {
            PsiElement insertedBranch = element.addBefore(JetPsiFactory.createElseWhenEntry(project), insertBeforeAnchor);
            element.addAfter(JetPsiFactory.createNewLineWhiteSpace(project), insertedBranch);
        }
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                JetWhenExpression whenExpression = PsiTreeUtil.getParentOfType(element, JetWhenExpression.class, false);
                if (whenExpression == null) return null;
                return new AddWhenElseBranchFix(whenExpression);
            }
        };
    }
}
