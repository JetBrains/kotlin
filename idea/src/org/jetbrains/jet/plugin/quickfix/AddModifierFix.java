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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 */
public class AddModifierFix extends JetIntentionAction<JetModifierListOwner> {
    private final JetKeywordToken modifier;
    private final JetToken[] modifiersThanCanBeReplaced;

    private AddModifierFix(@NotNull JetModifierListOwner element, JetKeywordToken modifier, JetToken[] modifiersThanCanBeReplaced) {
        super(element);
        this.modifier = modifier;
        this.modifiersThanCanBeReplaced = modifiersThanCanBeReplaced;
    }

    @NotNull
    /*package*/ static String getElementName(@NotNull JetModifierListOwner modifierListOwner) {
        String name = null;
        if (modifierListOwner instanceof PsiNameIdentifierOwner) {
            PsiElement nameIdentifier = ((PsiNameIdentifierOwner) modifierListOwner).getNameIdentifier();
            if (nameIdentifier != null) {
                name = nameIdentifier.getText();
            }
        }
        else if (modifierListOwner instanceof JetPropertyAccessor) {
            name = ((JetPropertyAccessor) modifierListOwner).getNamePlaceholder().getText();
        }
        if (name == null) {
            name = modifierListOwner.getText();
        }
        return "'" + name + "'";
    }

    @NotNull
    @Override
    public String getText() {
        if (modifier == JetTokens.ABSTRACT_KEYWORD || modifier == JetTokens.OPEN_KEYWORD) {
            return JetBundle.message("make.element.modifier", getElementName(element), modifier.getValue());
        }
        return JetBundle.message("add.modifier", modifier.getValue());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Add modifier";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.replace(addModifier(element, modifier, modifiersThanCanBeReplaced, project));
    }

    @NotNull
    private static JetModifierListOwner addModifier(@NotNull PsiElement element, @NotNull JetKeywordToken modifier, JetToken[] modifiersThanCanBeReplaced, @NotNull Project project) {
        JetModifierListOwner newElement = (JetModifierListOwner) (element.copy());

        JetModifierList modifierList = newElement.getModifierList();
        JetModifierList listWithModifier = JetPsiFactory.createModifier(project, modifier);
        PsiElement whiteSpace = JetPsiFactory.createWhiteSpace(project);
        if (modifierList == null) {
            PsiElement firstChild = newElement.getFirstChild();
            newElement.addBefore(listWithModifier, firstChild);
            newElement.addBefore(whiteSpace, firstChild);
        }
        else {
            boolean replaced = false;
            for (JetToken modifierThanCanBeReplaced : modifiersThanCanBeReplaced) {
                if (modifierList.hasModifier(modifierThanCanBeReplaced)) {
                    PsiElement openModifierPsi = modifierList.getModifierNode(modifierThanCanBeReplaced).getPsi();
                    assert openModifierPsi != null;
                    openModifierPsi.replace(listWithModifier.getFirstChild());
                    replaced = true;
                }
            }
            if (!replaced) {
                PsiElement lastChild = modifierList.getLastChild();
                modifierList.addAfter(listWithModifier.getFirstChild(), lastChild);
                modifierList.addAfter(whiteSpace, lastChild);
            }
        }
        return newElement;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static JetIntentionActionFactory createFactory(final JetKeywordToken modifier, final JetToken[] modifiersThatCanBeReplaced) {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(Diagnostic diagnostic) {
                JetModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, JetModifierListOwner.class);
                if (modifierListOwner == null) return null;
                return new AddModifierFix(modifierListOwner, modifier, modifiersThatCanBeReplaced);
            }
        };
    }
}
