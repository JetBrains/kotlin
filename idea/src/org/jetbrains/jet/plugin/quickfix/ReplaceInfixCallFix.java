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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;

public class ReplaceInfixCallFix extends JetIntentionAction<JetBinaryExpression> {
    public ReplaceInfixCallFix(@NotNull JetBinaryExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("replace.with.safe.call");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        String newText = element.getLeft().getText() + "?." + element.getOperationReference().getText()
                         + "(" + element.getRight().getText() + ")";
        JetQualifiedExpression newElement = (JetQualifiedExpression) JetPsiFactory.createExpression(project, newText);
        element.replace(newElement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetBinaryExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpression.class);
                return new ReplaceInfixCallFix(expression);
            }
        };
    }
}
