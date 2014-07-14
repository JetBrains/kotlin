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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class AddSemicolonAfterFunctionCallFix extends JetIntentionAction<JetCallExpression> {
    JetFunctionLiteralExpression literal;

    public AddSemicolonAfterFunctionCallFix(@NotNull JetCallExpression element, @NotNull JetFunctionLiteralExpression literal) {
        super(element);
        this.literal = literal;
    }

    @NotNull
    @Override
    public String getText() {
        JetExpression callee = element.getCalleeExpression();
        assert callee != null;
        return JetBundle.message("add.semicolon.after.invocation", callee.getText());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.semicolon.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        PsiElement argumentList = element.getValueArgumentList();
        assert argumentList != null;
        PsiElement afterArgumentList = argumentList.getNextSibling();
        int caretOffset = editor.getCaretModel().getOffset();
        element.getParent().addRangeAfter(afterArgumentList, literal, element);
        element.deleteChildRange(afterArgumentList, literal);
        element.getParent().addAfter(JetPsiFactory(file).createSemicolon(), element);
        editor.getCaretModel().moveToOffset(caretOffset + 1);
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction createAction(Diagnostic diagnostic) {
                JetCallExpression callExpression = QuickFixUtil.getParentElementOfType(diagnostic, JetCallExpression.class);
                JetFunctionLiteralExpression literalExpression = QuickFixUtil.getParentElementOfType(diagnostic, JetFunctionLiteralExpression.class);
                if (callExpression == null || literalExpression == null) return null;
                return new AddSemicolonAfterFunctionCallFix(callExpression, literalExpression);
            }
        };
    }
}
