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
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.references.ReferenceUtilKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

public class AddOpenModifierToClassDeclarationFix extends KotlinQuickFixAction<KtTypeReference> {
    private KtClass classDeclaration;

    public AddOpenModifierToClassDeclarationFix(@NotNull KtTypeReference typeReference) {
        super(typeReference);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        KtSimpleNameExpression referenceExpression = PsiTreeUtil.findChildOfType(getElement(), KtSimpleNameExpression.class);
        if (referenceExpression == null) {
            return false;
        }

        PsiReference reference = ReferenceUtilKt.getMainReference(referenceExpression);
        PsiElement target = reference.resolve();
        if (target instanceof KtSecondaryConstructor) {
            target = ((KtSecondaryConstructor) target).getContainingClassOrObject();
        }
        if (target instanceof KtClass && QuickFixUtil.canModifyElement(target)) {
            classDeclaration = (KtClass) target;
            return !(classDeclaration.isEnum() || classDeclaration.isInterface());
        }

        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("make.element.modifier", classDeclaration != null ? classDeclaration.getName() : "<unknown>", "open");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.modifier.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        classDeclaration.addModifier(KtTokens.OPEN_KEYWORD);
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                KtTypeReference typeReference = QuickFixUtil.getParentElementOfType(diagnostic, KtTypeReference.class);
                return typeReference == null ? null : new AddOpenModifierToClassDeclarationFix(typeReference);
            }
        };
    }
}
