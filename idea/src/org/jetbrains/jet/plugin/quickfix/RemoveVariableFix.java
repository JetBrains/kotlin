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
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.plugin.JetBundle;

public class RemoveVariableFix extends JetIntentionAction<JetProperty> {
    public RemoveVariableFix(@NotNull JetProperty element) {
        super(element);
    }

    private String getVariableName() {
        return element.getName();
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.variable.action", getVariableName());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.variable.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetExpression initializer = element.getInitializer();
        if (initializer != null) {
            element.replace(initializer);
        }
        else {
            element.delete();
        }
    }

    public static JetIntentionActionFactory createRemoveVariableFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction createAction(Diagnostic diagnostic) {
                JetProperty expression = QuickFixUtil.getParentElementOfType(diagnostic, JetProperty.class);
                if (expression == null) return null;
                return new RemoveVariableFix(expression);
            }
        };
    }
}

