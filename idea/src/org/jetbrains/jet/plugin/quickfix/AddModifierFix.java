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
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class AddModifierFix extends JetIntentionAction<JetModifierListOwner> {
    private final JetKeywordToken modifier;
    private final JetToken[] modifiersThanCanBeReplaced;

    private AddModifierFix(@NotNull JetModifierListOwner element, @NotNull JetKeywordToken modifier, @Nullable JetToken[] modifiersThanCanBeReplaced) {
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
        if (modifier == ABSTRACT_KEYWORD || modifier == JetTokens.OPEN_KEYWORD) {
            return JetBundle.message("make.element.modifier", getElementName(element), modifier.getValue());
        }
        return JetBundle.message("add.modifier", modifier.getValue());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.modifier.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.replace(addModifier(element, modifier, modifiersThanCanBeReplaced, project, false));
    }

    @NotNull
    /*package*/ static JetModifierListOwner addModifier(@NotNull PsiElement element, @NotNull JetKeywordToken modifier, @Nullable JetToken[] modifiersThatCanBeReplaced, @NotNull Project project, boolean toBeginning) {
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
            if (modifiersThatCanBeReplaced != null) {
                PsiElement toBeReplaced = null;
                PsiElement toReplace = null;
                for (JetToken modifierThatCanBeReplaced : modifiersThatCanBeReplaced) {
                    if (modifierList.hasModifier(modifierThatCanBeReplaced)) {
                        PsiElement modifierElement = modifierList.getModifierNode(modifierThatCanBeReplaced).getPsi();
                        assert modifierElement != null;
                        if (!replaced) {
                            toBeReplaced = modifierElement;
                            toReplace = listWithModifier.getFirstChild();
                            //modifierElement.replace(listWithModifier.getFirstChild());
                            replaced = true;
                        }
                        else {
                            modifierList.deleteChildInternal(modifierElement.getNode());
                        }
                    }
                }
                if (toBeReplaced != null && toReplace != null) {
                    toBeReplaced.replace(toReplace);
                }
            }
            if (!replaced) {
                if (toBeginning) {
                    PsiElement firstChild = modifierList.getFirstChild();
                    modifierList.addBefore(listWithModifier.getFirstChild(), firstChild);
                    modifierList.addBefore(whiteSpace, firstChild);
                }
                else {
                    PsiElement lastChild = modifierList.getLastChild();
                    modifierList.addAfter(listWithModifier.getFirstChild(), lastChild);
                    modifierList.addAfter(whiteSpace, lastChild);
                }
            }
        }
        return newElement;
    }

    @NotNull
    /*package*/ static JetModifierListOwner addModifierWithDefaultReplacement(@NotNull PsiElement element, @NotNull JetKeywordToken modifier, @NotNull Project project, boolean toBeginning) {
        return addModifier(element, modifier, MODIFIERS_THAT_CAN_BE_REPLACED.get(modifier), project, toBeginning);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static <T extends JetModifierListOwner> JetIntentionActionFactory createFactory(final JetKeywordToken modifier, final Class<T> modifierOwnerClass) {
        return new JetIntentionActionFactory() {
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, modifierOwnerClass);
                if (modifierListOwner == null) return null;
                return new AddModifierFix(modifierListOwner, modifier, MODIFIERS_THAT_CAN_BE_REPLACED.get(modifier));
            }
        };
    }

    public static JetIntentionActionFactory createFactory(final JetKeywordToken modifier) {
        return createFactory(modifier, JetModifierListOwner.class);
    }

    private static Map<JetToken, JetToken[]> MODIFIERS_THAT_CAN_BE_REPLACED = new HashMap<JetToken, JetToken[]>();
    static {
        MODIFIERS_THAT_CAN_BE_REPLACED.put(ABSTRACT_KEYWORD, new JetToken[]{OPEN_KEYWORD, FINAL_KEYWORD});
        MODIFIERS_THAT_CAN_BE_REPLACED.put(OVERRIDE_KEYWORD, new JetToken[]{OPEN_KEYWORD});
        MODIFIERS_THAT_CAN_BE_REPLACED.put(OPEN_KEYWORD, new JetToken[]{FINAL_KEYWORD});
    }
}
