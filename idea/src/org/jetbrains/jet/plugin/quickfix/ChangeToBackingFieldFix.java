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
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class ChangeToBackingFieldFix extends JetIntentionAction<JetSimpleNameExpression> {
    public ChangeToBackingFieldFix(@NotNull JetSimpleNameExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.to.backing.field");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.backing.field");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetSimpleNameExpression backingField = (JetSimpleNameExpression) JetPsiFactory(file).createExpression("$" + element.getText());
        element.replace(backingField);
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetSimpleNameExpression> createAction(Diagnostic diagnostic) {
                JetSimpleNameExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, JetSimpleNameExpression.class);
                if (expression == null) {
                    PsiElement element = diagnostic.getPsiElement();
                    if (element instanceof JetQualifiedExpression && ((JetQualifiedExpression) element).getReceiverExpression() instanceof JetThisExpression) {
                        JetExpression selector = ((JetQualifiedExpression) element).getSelectorExpression();
                        if (selector instanceof JetSimpleNameExpression) {
                            expression = (JetSimpleNameExpression) selector;
                        }
                    }
                }
                if (expression == null) return null;
                return new ChangeToBackingFieldFix(expression);
            }
        };
    }
}
