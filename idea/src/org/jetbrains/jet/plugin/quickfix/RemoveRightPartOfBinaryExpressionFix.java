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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.kotlin.psi.JetBinaryExpression;
import org.jetbrains.kotlin.psi.JetBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.jet.plugin.JetBundle;

public class RemoveRightPartOfBinaryExpressionFix<T extends JetExpression> extends JetIntentionAction<T> {
    private final String message;
    
    public RemoveRightPartOfBinaryExpressionFix(@NotNull T element, String message) {
        super(element);
        this.message = message;
    }

    public String getText() {
        return message;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.right.part.of.binary.expression");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        if (element instanceof JetBinaryExpression) {
            JetBinaryExpression newElement = (JetBinaryExpression) element.copy();
            element.replace(newElement.getLeft());
        }
        else if (element instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS newElement = (JetBinaryExpressionWithTypeRHS) element.copy();
            element.replace(newElement.getLeft());
        }
    }

    public static JetSingleIntentionActionFactory createRemoveCastFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetBinaryExpressionWithTypeRHS> createAction(Diagnostic diagnostic) {
                JetBinaryExpressionWithTypeRHS expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpressionWithTypeRHS.class);
                if (expression == null) return null;
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpressionWithTypeRHS>(expression, JetBundle.message("remove.cast"));
            }
        };
    }

    public static JetSingleIntentionActionFactory createRemoveElvisOperatorFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetBinaryExpression> createAction(Diagnostic diagnostic) {
                JetBinaryExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpression.class);
                if (expression == null) return null;
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpression>(expression, JetBundle.message("remove.elvis.operator"));
            }
        };
    }
}

