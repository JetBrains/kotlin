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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

public class AddOpenModifierToClassDeclarationFix extends JetIntentionAction<JetTypeReference> {
    private JetClass classDeclaration;

    public AddOpenModifierToClassDeclarationFix(@NotNull JetTypeReference typeReference) {
        super(typeReference);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        JetSimpleNameExpression referenceExpression = PsiTreeUtil.findChildOfType(element, JetSimpleNameExpression.class);
        if (referenceExpression == null) {
            return false;
        }

        PsiReference reference = referenceExpression.getReference();
        if (reference != null) {
            PsiElement target = reference.resolve();
            if (target instanceof JetClass && target.isWritable()) {
                classDeclaration = (JetClass) target;
                return !(classDeclaration.isEnum() || classDeclaration.isTrait());
            }
        }

        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("make.element.modifier", classDeclaration.getName(), "open");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.modifier.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        classDeclaration.replace(AddModifierFix.addModifierWithDefaultReplacement(classDeclaration, JetTokens.OPEN_KEYWORD, project, false));
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetTypeReference typeReference = QuickFixUtil.getParentElementOfType(diagnostic, JetTypeReference.class);
                return typeReference == null ? null : new AddOpenModifierToClassDeclarationFix(typeReference);
            }
        };
    }
}
