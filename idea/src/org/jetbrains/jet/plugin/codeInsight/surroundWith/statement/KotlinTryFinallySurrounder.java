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

package org.jetbrains.jet.plugin.codeInsight.surroundWith.statement;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFinallySection;
import org.jetbrains.jet.lang.psi.JetTryExpression;

public class KotlinTryFinallySurrounder extends KotlinTrySurrounderBase {

    @Override
    protected String getCodeTemplate() {
        return "try { \n} finally {\nb\n}";
    }

    @Nullable
    @Override
    protected TextRange surroundStatements(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement container, @NotNull PsiElement[] statements) {
        TextRange textRange = super.surroundStatements(project, editor, container, statements);
        if (textRange == null) {
            return null;
        }
        // Delete dummy "b" element for caret
        editor.getDocument().deleteString(textRange.getStartOffset(), textRange.getEndOffset());
        return new TextRange(textRange.getStartOffset(), textRange.getStartOffset());
    }

    @NotNull
    @Override
    protected TextRange getTextRangeForCaret(@NotNull JetTryExpression expression) {
        JetFinallySection block = expression.getFinallyBlock();
        assert block != null : "Finally block should exists for " + expression.getText();
        JetElement blockExpression = block.getFinalExpression().getStatements().get(0);
        return blockExpression.getTextRange();
    }

    @Override
    public String getTemplateDescription() {
        return CodeInsightBundle.message("surround.with.try.finally.template");
    }
}
