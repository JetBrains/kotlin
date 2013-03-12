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
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

public class RemovePsiElementSimpleFix extends JetIntentionAction<PsiElement> {

    private final PsiElement element;
    private final String text;


    public RemovePsiElementSimpleFix(@NotNull PsiElement el, @NotNull String txt) {
        super(el);
        element = el;
        text = txt;
    }

    @NotNull
    @Override
    public String getText() {
        return text;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.psi.element.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.delete();
    }

    public static JetIntentionActionFactory createRemoveImportFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<PsiElement> createAction(Diagnostic diagnostic) {
                JetImportDirective directive = QuickFixUtil.getParentElementOfType(diagnostic, JetImportDirective.class);
                if (directive == null) return null;
                else {
                    JetExpression exp = directive.getImportedReference();
                    if (exp != null) {
                        return new RemovePsiElementSimpleFix(directive,
                                                             JetBundle.message("remove.useless.import", exp.getText()));
                    }
                    return new RemovePsiElementSimpleFix(directive,
                                                         JetBundle.message("remove.useless.import",""));
                }
            }
        };
    }

    public static JetIntentionActionFactory createRemoveSpreadFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<PsiElement> createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                if ((element instanceof LeafPsiElement) && ((LeafPsiElement) element).getElementType() == JetTokens.MUL) {
                    return new RemovePsiElementSimpleFix(element, JetBundle.message("remove.spread.sign"));
                }
                else return null;
            }
        };
    }

    public static JetIntentionActionFactory createRemoveVariableFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<PsiElement> createAction(Diagnostic diagnostic) {
                final JetProperty expression = QuickFixUtil.getParentElementOfType(diagnostic, JetProperty.class);
                if (expression == null) return null;
                return new RemovePsiElementSimpleFix(expression,
                                                     JetBundle.message("remove.variable.action", (expression.getName()))) {
                    @Override
                    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                        JetExpression initializer = expression.getInitializer();
                        if (initializer != null) {
                            expression.replace(initializer);
                        }
                        else {
                            expression.delete();
                        }
                    }
                };
            }
        };
    }
}