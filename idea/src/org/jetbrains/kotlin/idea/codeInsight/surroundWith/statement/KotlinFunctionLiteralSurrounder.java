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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.KotlinSurrounderUtils;
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.MoveDeclarationsOutHelperKt;
import org.jetbrains.kotlin.psi.*;

public class KotlinFunctionLiteralSurrounder extends KotlinStatementsSurrounder {
    @Nullable
    @Override
    protected TextRange surroundStatements(Project project, Editor editor, PsiElement container, PsiElement[] statements) {
        statements = MoveDeclarationsOutHelperKt.move(container, statements, true);

        if (statements.length == 0) {
            KotlinSurrounderUtils.showErrorHint(project, editor, KotlinSurrounderUtils.SURROUND_WITH_ERROR);
            return null;
        }

        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(project);
        KtCallExpression callExpression = (KtCallExpression) psiFactory.createExpression("run {\n}");
        callExpression = (KtCallExpression) container.addAfter(callExpression, statements[statements.length - 1]);
        container.addBefore(psiFactory.createWhiteSpace(), callExpression);

        KtLambdaExpression bodyExpression = callExpression.getLambdaArguments().get(0).getLambdaExpression();
        assert bodyExpression != null : "Body expression should exists for " + callExpression.getText();
        KtBlockExpression blockExpression = bodyExpression.getBodyExpression();
        assert blockExpression != null : "JetBlockExpression should exists for " + callExpression.getText();
        //Add statements in function literal block
        KotlinSurrounderUtils.addStatementsInBlock(blockExpression, statements);

        //Delete statements from original code
        container.deleteChildRange(statements[0], statements[statements.length - 1]);

        callExpression = CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(callExpression);

        KtExpression literalName = callExpression.getCalleeExpression();
        assert literalName != null : "Run expression should have callee expression " + callExpression.getText();
        return literalName.getTextRange();
    }

    @Override
    public String getTemplateDescription() {
        return KotlinBundle.message("surround.with.function.template");
    }
}
