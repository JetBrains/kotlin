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

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 */
public class RemoveModifierFix extends JetIntentionAction<JetModifierListOwner> {
    private final JetKeywordToken modifier;
    private final boolean isRedundant;

    public RemoveModifierFix(@NotNull JetModifierListOwner element, @NotNull JetKeywordToken modifier, boolean isRedundant) {
        super(element);
        this.modifier = modifier;
        this.isRedundant = isRedundant;
    }

    private static String makeText(@Nullable JetModifierListOwner element, JetKeywordToken modifier, boolean isRedundant) {
        if (isRedundant) {
            return JetBundle.message("remove.redundant.modifier", modifier.getValue());
        }
        if (element != null && (modifier == JetTokens.ABSTRACT_KEYWORD || modifier == JetTokens.OPEN_KEYWORD)) {
            return JetBundle.message("make.element.not.modifier", AddModifierFix.getElementName(element), modifier.getValue());
        }
        return JetBundle.message("remove.modifier", modifier.getValue());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.modifier.family");
    }

    @NotNull
    private static <T extends JetModifierListOwner> T removeModifier(T element, JetToken modifier) {
        JetModifierList modifierList = element.getModifierList();
        assert modifierList != null;
        removeModifierFromList(modifierList, modifier);
        if (modifierList.getFirstChild() == null) {
            PsiElement whiteSpace = modifierList.getNextSibling();
            assert element instanceof ASTDelegatePsiElement;
            ((ASTDelegatePsiElement) element).deleteChildInternal(modifierList.getNode());
            QuickFixUtil.removePossiblyWhiteSpace((ASTDelegatePsiElement) element, whiteSpace);
        }
        return element;
    }

    @NotNull
    private static JetModifierList removeModifierFromList(@NotNull JetModifierList modifierList, JetToken modifier) {
        assert modifierList.hasModifier(modifier);
        ASTNode modifierNode = modifierList.getModifierNode(modifier);
        PsiElement whiteSpace = modifierNode.getPsi().getNextSibling();
        boolean wsRemoved = QuickFixUtil.removePossiblyWhiteSpace(modifierList, whiteSpace);
        modifierList.deleteChildInternal(modifierNode);
        if (!wsRemoved) {
            QuickFixUtil.removePossiblyWhiteSpace(modifierList, modifierList.getLastChild());
        }
        return modifierList;
    }

    @NotNull
    @Override
    public String getText() {
        return makeText(element, modifier, isRedundant);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetModifierListOwner newElement = (JetModifierListOwner) element.copy();
        element.replace(removeModifier(newElement, modifier));
    }


    public static JetIntentionActionFactory createRemoveModifierFromListOwnerFactory(final JetKeywordToken modifier) {
        return createRemoveModifierFromListOwnerFactory(modifier, false);
    }

    public static JetIntentionActionFactory createRemoveModifierFromListOwnerFactory(final JetKeywordToken modifier, final boolean isRedundant) {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(Diagnostic diagnostic) {
                JetModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, JetModifierListOwner.class);
                if (modifierListOwner == null) return null;
                return new RemoveModifierFix(modifierListOwner, modifier, isRedundant);
            }
        };
    }

    public static JetIntentionActionFactory createRemoveModifierFactory() {
        return createRemoveModifierFactory(false);
    }

    public static JetIntentionActionFactory createRemoveModifierFactory(final boolean isRedundant) {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(Diagnostic diagnostic) {
                JetModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, JetModifierListOwner.class);
                if (modifierListOwner == null) return null;
                PsiElement psiElement = diagnostic.getPsiElement();
                IElementType elementType = psiElement.getNode().getElementType();
                if (!(elementType instanceof JetKeywordToken)) return null;
                JetKeywordToken modifier = (JetKeywordToken) elementType;
                return new RemoveModifierFix(modifierListOwner, modifier, isRedundant);
            }
        };
    }
}
