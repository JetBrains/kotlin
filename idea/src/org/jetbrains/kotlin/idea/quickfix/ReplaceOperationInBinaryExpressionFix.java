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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.psi.*;

public abstract class ReplaceOperationInBinaryExpressionFix<T extends KtExpression> extends KotlinQuickFixAction<T> {
    private final String operation;

    public ReplaceOperationInBinaryExpressionFix(@NotNull T element, String operation) {
        super(element);
        this.operation = operation;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("replace.operation.in.binary.expression");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        if (getElement() instanceof KtBinaryExpressionWithTypeRHS) {
            KtExpression left = ((KtBinaryExpressionWithTypeRHS) getElement()).getLeft();
            KtTypeReference right = ((KtBinaryExpressionWithTypeRHS) getElement()).getRight();
            if (right != null) {
                KtExpression expression = KtPsiFactoryKt.KtPsiFactory(file).createExpression(left.getText() + operation + right.getText());
                getElement().replace(expression);
            }
        }
    }
}
