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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

public class RemoveValVarFromParameterFix extends KotlinQuickFixAction<KtParameter> {
    private final String varOrVal;

    public RemoveValVarFromParameterFix(@NotNull KtParameter element) {
        super(element);
        PsiElement valOrVarNode = element.getValOrVarKeyword();
        assert valOrVarNode != null : "Val or var node not found for " + PsiUtilsKt.getElementTextWithContext(element);
        varOrVal = valOrVarNode.getText();
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("remove.val.var.from.parameter", varOrVal);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("remove.val.var.from.parameter", "val/var");
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        PsiElement keyword = getElement().getValOrVarKeyword();
        if (keyword == null) return;
        keyword.delete();
    }


    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                return new RemoveValVarFromParameterFix((KtParameter) diagnostic.getPsiElement().getParent());
            }
        };
    }
}
