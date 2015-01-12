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
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.psi.JetBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetTypeReference;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public abstract class ReplaceOperationInBinaryExpressionFix<T extends JetExpression> extends JetIntentionAction<T> {
    private final String operation;

    public ReplaceOperationInBinaryExpressionFix(@NotNull T element, String operation) {
        super(element);
        this.operation = operation;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("replace.operation.in.binary.expression");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        if (element instanceof JetBinaryExpressionWithTypeRHS) {
            JetExpression left = ((JetBinaryExpressionWithTypeRHS) element).getLeft();
            JetTypeReference right = ((JetBinaryExpressionWithTypeRHS) element).getRight();
            if (right != null) {
                JetExpression expression = JetPsiFactory(file).createExpression(left.getText() + operation + right.getText());
                element.replace(expression);
            }
        }
    }

    public static JetSingleIntentionActionFactory createChangeCastToStaticAssertFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetBinaryExpressionWithTypeRHS> createAction(Diagnostic diagnostic) {
                JetBinaryExpressionWithTypeRHS expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpressionWithTypeRHS.class);
                if (expression == null) return null;
                return new ReplaceOperationInBinaryExpressionFix<JetBinaryExpressionWithTypeRHS>(expression, " : ") {
                    @NotNull
                    @Override
                    public String getText() {
                        return JetBundle.message("replace.cast.with.static.assert");
                    }
                };
            }
        };
    }
}
