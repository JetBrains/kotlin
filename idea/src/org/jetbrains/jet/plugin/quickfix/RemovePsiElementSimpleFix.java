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
import org.jetbrains.jet.lang.psi.JetTypeArgumentList;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

public class RemovePsiElementSimpleFix extends JetIntentionAction<PsiElement> {

    public enum PsiElementType {
        IMPORT, SPREAD, TYPE_ARGS, VARIABLE
    }

    private final PsiElement element;
    private final PsiElementType type;

    public RemovePsiElementSimpleFix(@NotNull PsiElement el, @NotNull PsiElementType t) {
        super(el);
        element = el;
        type = t;
    }

    @NotNull
    @Override
    public String getText() {
        switch (type) {
            case IMPORT:
                JetExpression exp = ((JetImportDirective) element).getImportedReference();
                if (exp != null) {
                    return JetBundle.message("remove.useless.import",
                                         exp.getText());
                }
                else return JetBundle.message("remove.useless.import", "");
            case SPREAD:
                return JetBundle.message("remove.spread.sign");

            case TYPE_ARGS:
                return JetBundle.message("remove.type.arguments");

            default:
                return JetBundle.message("remove.variable.action", ((JetProperty) element).getName());
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.psi.element.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (type == PsiElementType.VARIABLE) {
            JetExpression initializer = ((JetProperty) element).getInitializer();
            if (initializer != null) {
                element.replace(initializer);
                return;
            }
        }
        element.delete();
    }

    public static JetIntentionActionFactory createFactory(final PsiElementType type) {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<PsiElement> createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                if ((element instanceof LeafPsiElement && ((LeafPsiElement) element).getElementType() == JetTokens.MUL
                        && type == PsiElementType.SPREAD) ||
                    (element instanceof JetTypeArgumentList && type == PsiElementType.TYPE_ARGS) ||
                    (element instanceof JetProperty && type == PsiElementType.VARIABLE)) {
                    return new RemovePsiElementSimpleFix(element, type);
                }
                else if (type == PsiElementType.IMPORT) {
                    JetImportDirective directive = QuickFixUtil.getParentElementOfType(diagnostic, JetImportDirective.class);
                    if (directive != null) return new RemovePsiElementSimpleFix(directive, type);
                }
                return null;
            }
        };
    }
}