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
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtModifierListOwner;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtPropertyAccessor;

import static org.jetbrains.kotlin.lexer.KtTokens.ABSTRACT_KEYWORD;

public class AddModifierFix extends KotlinQuickFixAction<KtModifierListOwner> {
    @NotNull private final KtModifierKeywordToken modifier;

    public AddModifierFix(@NotNull KtModifierListOwner element, @NotNull KtModifierKeywordToken modifier) {
        super(element);
        this.modifier = modifier;
    }

    @NotNull
    /*package*/ static String getElementName(@NotNull KtModifierListOwner modifierListOwner) {
        String name = null;
        if (modifierListOwner instanceof PsiNameIdentifierOwner) {
            PsiElement nameIdentifier = ((PsiNameIdentifierOwner) modifierListOwner).getNameIdentifier();
            if (nameIdentifier != null) {
                name = nameIdentifier.getText();
            }
        }
        else if (modifierListOwner instanceof KtPropertyAccessor) {
            name = ((KtPropertyAccessor) modifierListOwner).getNamePlaceholder().getText();
        }
        if (name == null) {
            name = modifierListOwner.getText();
        }
        return "'" + name + "'";
    }

    @NotNull
    @Override
    public String getText() {
        if (modifier == ABSTRACT_KEYWORD || modifier == KtTokens.OPEN_KEYWORD) {
            return JetBundle.message("make.element.modifier", getElementName(getElement()), modifier.getValue());
        }
        return JetBundle.message("add.modifier", modifier.getValue());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.modifier.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        getElement().addModifier(modifier);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static <T extends KtModifierListOwner> JetSingleIntentionActionFactory createFactory(final KtModifierKeywordToken modifier, final Class<T> modifierOwnerClass) {
        return new JetSingleIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                KtModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, modifierOwnerClass);
                if (modifierListOwner == null) return null;

                if (modifier == KtTokens.ABSTRACT_KEYWORD &&
                    modifierListOwner instanceof KtObjectDeclaration &&
                    ((KtObjectDeclaration) modifierListOwner).isObjectLiteral()) return null;

                return new AddModifierFix(modifierListOwner, modifier);
            }
        };
    }

    public static JetSingleIntentionActionFactory createFactory(KtModifierKeywordToken modifier) {
        return createFactory(modifier, KtModifierListOwner.class);
    }
}
