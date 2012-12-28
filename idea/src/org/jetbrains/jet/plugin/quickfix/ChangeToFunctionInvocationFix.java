/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.plugin.JetBundle;

public class ChangeToFunctionInvocationFix extends JetIntentionAction<JetExpression> {

    public ChangeToFunctionInvocationFix(@NotNull JetExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.to.function.invocation");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.function.invocation");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetExpression reference = (JetExpression) element.copy();
        element.replace(JetPsiFactory.createExpression(project, reference.getText() + "()"));
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetExpression> createAction(Diagnostic diagnostic) {
                if (diagnostic.getPsiElement() instanceof JetExpression) {
                    return new ChangeToFunctionInvocationFix((JetExpression) diagnostic.getPsiElement());
                }
                return null;
            }
        };
    }
}
