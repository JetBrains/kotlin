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

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTryExpression;
import org.jetbrains.jet.plugin.codeInsight.surroundWith.KotlinSurrounderUtils;
import org.jetbrains.jet.plugin.codeInsight.surroundWith.MoveDeclarationsOutHelper;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public abstract class KotlinTrySurrounderBase extends KotlinStatementsSurrounder {

    @Nullable
    @Override
    protected TextRange surroundStatements(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement container, @NotNull PsiElement[] statements) {
        statements = MoveDeclarationsOutHelper.move(container, statements, true);

        if (statements.length == 0) {
            KotlinSurrounderUtils.showErrorHint(project, editor, KotlinSurrounderUtils.SURROUND_WITH_ERROR);
            return null;
        }

        JetTryExpression tryExpression = (JetTryExpression) JetPsiFactory(project).createExpression(getCodeTemplate());
        tryExpression = (JetTryExpression) container.addAfter(tryExpression, statements[statements.length - 1]);

        // TODO move a comment for first statement

        JetBlockExpression tryBlock = tryExpression.getTryBlock();
        // Add statements in try block of created try - catch - finally
        KotlinSurrounderUtils.addStatementsInBlock(tryBlock, statements);

        // Delete statements from original code
        container.deleteChildRange(statements[0], statements[statements.length - 1]);

        tryExpression = CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(tryExpression);

        return getTextRangeForCaret(tryExpression);
    }

    protected abstract String getCodeTemplate();

    @NotNull
    protected abstract TextRange getTextRangeForCaret(@NotNull JetTryExpression expression);

    protected static TextRange getCatchTypeParameterTextRange(@NotNull JetTryExpression expression) {
        JetParameter parameter = expression.getCatchClauses().get(0).getCatchParameter();
        assert parameter != null : "Catch parameter should exists for " + expression.getText();
        JetElement typeReference = parameter.getTypeReference();
        assert typeReference != null : "Type reference for parameter should exists for " + expression.getText();
        return typeReference.getTextRange();
    }

}
