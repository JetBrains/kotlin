/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement;


import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.KotlinSurrounderUtils;
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.MoveDeclarationsOutHelper;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;

public abstract class KotlinIfSurrounderBase extends KotlinStatementsSurrounder {

    @Override
    protected boolean isApplicableWhenUsedAsExpression() {
        return false;
    }

    @Nullable
    @Override
    protected TextRange surroundStatements(Project project, Editor editor, PsiElement container, PsiElement[] statements) {
        statements = MoveDeclarationsOutHelper.move(container, statements, isGenerateDefaultInitializers());

        if (statements.length == 0) {
            KotlinSurrounderUtils.showErrorHint(project, editor, KotlinSurrounderUtils.SURROUND_WITH_ERROR);
            return null;
        }

        KtIfExpression ifExpression = (KtIfExpression) KtPsiFactoryKt.KtPsiFactory(project).createExpression(getCodeTemplate());
        ifExpression = (KtIfExpression) container.addAfter(ifExpression, statements[statements.length - 1]);

        // TODO move a comment for first statement

        KtBlockExpression thenBranch = (KtBlockExpression) ifExpression.getThen();
        assert thenBranch != null : "Then branch should exist for created if expression: " + ifExpression.getText();
        // Add statements in then branch of created if
        KotlinSurrounderUtils.addStatementsInBlock(thenBranch, statements);

        // Delete statements from original code
        container.deleteChildRange(statements[0], statements[statements.length - 1]);

        ifExpression = CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(ifExpression);

        return getRange(editor, ifExpression);
    }

    @NotNull
    public static TextRange getRange(Editor editor, @NotNull KtIfExpression ifExpression) {
        KtExpression condition = ifExpression.getCondition();
        assert condition != null : "Condition should exists for created if expression: " + ifExpression.getText();
        // Delete condition from created if
        TextRange range = condition.getTextRange();
        TextRange textRange = new TextRange(range.getStartOffset(), range.getStartOffset());
        editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());
        return textRange;
    }

    @NotNull
    protected abstract String getCodeTemplate();

    protected abstract boolean isGenerateDefaultInitializers();
}
