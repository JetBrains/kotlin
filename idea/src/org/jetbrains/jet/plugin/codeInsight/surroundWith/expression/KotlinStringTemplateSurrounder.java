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

package org.jetbrains.jet.plugin.codeInsight.surroundWith.expression;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetConstantExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetStringTemplateEntry;
import org.jetbrains.jet.lang.psi.JetStringTemplateExpression;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class KotlinStringTemplateSurrounder extends KotlinExpressionSurrounder {
    @Override
    public String getTemplateDescription() {
        return JetBundle.message("surround.with.string.template");
    }

    @Override
    public boolean isApplicable(@NotNull JetExpression expression) {
        return !(expression instanceof JetStringTemplateExpression);
    }

    @Nullable
    @Override
    public TextRange surroundExpression(@NotNull Project project, @NotNull Editor editor, @NotNull JetExpression expression) {
        JetStringTemplateExpression stringTemplateExpression = (JetStringTemplateExpression) JetPsiFactory(expression).createExpression(
                getCodeTemplate(expression)
        );
        JetStringTemplateEntry templateEntry = stringTemplateExpression.getEntries()[0];
        JetExpression innerExpression = templateEntry.getExpression();
        assert innerExpression != null : "JetExpression should exists for " + stringTemplateExpression.toString();
        innerExpression.replace(expression);

        expression = (JetExpression) expression.replace(stringTemplateExpression);

        CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(expression);

        int offset = expression.getTextRange().getEndOffset();
        return new TextRange(offset, offset);
    }

    private String getCodeTemplate(JetExpression expression) {
        if (expression.getChildren().length > 0 ||
            expression instanceof JetConstantExpression) {
            return "\"${a}\"";
        }
        return "\"$a\"";
    }
}
