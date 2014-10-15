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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction.addTypeAnnotation;
import static org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction.getTypeForDeclaration;

@SuppressWarnings("IntentionDescriptionNotFoundInspection")
public class SpecifyTypeExplicitlyFix extends PsiElementBaseIntentionAction {
    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("specify.type.explicitly.action.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        //noinspection unchecked
        JetCallableDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetProperty.class, JetNamedFunction.class);
        JetType type = getTypeForDeclaration(declaration);
        addTypeAnnotation(project, editor, declaration, type);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        //noinspection unchecked
        JetCallableDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetProperty.class, JetNamedFunction.class);
        if (declaration instanceof JetProperty) {
            setText(JetBundle.message("specify.type.explicitly.add.action.name"));
        }
        else if (declaration instanceof JetNamedFunction) {
            setText(JetBundle.message("specify.type.explicitly.add.return.type.action.name"));
        }
        else {
            return false;
        }

        return !getTypeForDeclaration(declaration).isError();
    }
}
