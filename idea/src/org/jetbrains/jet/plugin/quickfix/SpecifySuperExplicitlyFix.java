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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.plugin.JetBundle;

public class SpecifySuperExplicitlyFix extends JetIntentionAction<JetSuperExpression>{

    public SpecifySuperExplicitlyFix(@NotNull JetSuperExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("specify.super.explicitly");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("specify.super.explicitly");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int beforeSuperExp = element.getTextOffset();
        JetExpression exp = JetPsiFactory.createExpression(project, "super<...>");
        element.replace(exp);


        editor.getCaretModel().moveToOffset(beforeSuperExp + exp.getText().indexOf('>'));
        editor.getSelectionModel().setSelection(beforeSuperExp + exp.getText().indexOf('<') + 1, beforeSuperExp + exp.getText().indexOf('>'));
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetSuperExpression> createAction(Diagnostic diagnostic) {
                JetSuperExpression exp = QuickFixUtil.getParentElementOfType(diagnostic, JetSuperExpression.class);
                if (exp == null) return null;
                return new SpecifySuperExplicitlyFix(exp);
            }
        };
    }
}
