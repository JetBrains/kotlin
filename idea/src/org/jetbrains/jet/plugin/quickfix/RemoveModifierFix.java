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

import com.google.common.collect.Lists;
import com.intellij.codeInsight.intention.IntentionAction;
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
import org.jetbrains.jet.lang.psi.JetTypeProjection;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RemoveModifierFix extends JetIntentionAction<JetModifierListOwner> {
    private final JetKeywordToken modifier;
    private final boolean isRedundant;
    private static final Collection<JetKeywordToken> VISIBILITY_MODIFIERS =
            Lists.newArrayList(JetTokens.PRIVATE_KEYWORD, JetTokens.PROTECTED_KEYWORD,
                               JetTokens.PUBLIC_KEYWORD, JetTokens.INTERNAL_KEYWORD);

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

    private static JetModifierListOwner getModifierListOwner(Diagnostic diagnostic) {
        return QuickFixUtil.getParentElementOfType(diagnostic, JetModifierListOwner.class);
    }

    private static JetKeywordToken getModifier(Diagnostic diagnostic) {
        PsiElement psiElement = diagnostic.getPsiElement();
        IElementType elementType = psiElement.getNode().getElementType();
        if (!(elementType instanceof JetKeywordToken)) return null;
        return (JetKeywordToken) elementType;
    }

    public static JetIntentionActionFactory createRemoveModifierFactory(final boolean isRedundant) {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(Diagnostic diagnostic) {
                JetModifierListOwner modifierListOwner = getModifierListOwner(diagnostic);
                JetKeywordToken modifier = getModifier(diagnostic);
                if (modifierListOwner == null || modifier == null) return null;
                return new RemoveModifierFix(modifierListOwner, modifier, isRedundant);
            }
        };
    }

    public static JetIntentionActionFactory createRemoveProjectionFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(Diagnostic diagnostic) {
                JetTypeProjection projection = QuickFixUtil.getParentElementOfType(diagnostic, JetTypeProjection.class);
                if (projection == null) return null;
                ASTNode projectionAstNode = projection.getProjectionNode();
                if (projectionAstNode == null) return null;
                IElementType elementType = projectionAstNode.getElementType();
                if (!(elementType instanceof JetKeywordToken)) return null;
                JetKeywordToken variance = (JetKeywordToken) elementType;
                return new RemoveModifierFix(projection, variance, true);
            }
        };
    }

    /**
     * Checks whether these two modifiers can be used together.
     */
    private static boolean areCompatible(JetKeywordToken thisModifier, JetKeywordToken otherModifier) {
        if (thisModifier == otherModifier) return false;
        if (VISIBILITY_MODIFIERS.contains(thisModifier) && VISIBILITY_MODIFIERS.contains(otherModifier)) return false;
        if ((thisModifier == JetTokens.OPEN_KEYWORD || thisModifier == JetTokens.ABSTRACT_KEYWORD) &&
            otherModifier == JetTokens.FINAL_KEYWORD) return false;
        if ((otherModifier == JetTokens.OPEN_KEYWORD || otherModifier == JetTokens.ABSTRACT_KEYWORD) &&
            thisModifier == JetTokens.FINAL_KEYWORD) return false;
        return true;
    }

    private static JetIntentionActionFactory createRemoveIncompatibleModifierFactory(final JetKeywordToken modifierToRemove) {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetModifierListOwner modifierListOwner = getModifierListOwner(diagnostic);
                JetKeywordToken modifier = getModifier(diagnostic);
                if (modifierListOwner == null || modifier == null
                    || areCompatible(modifier, modifierToRemove)
                    || !modifierListOwner.hasModifier(modifierToRemove)) {
                    return null;
                }
                return new RemoveModifierFix(modifierListOwner, modifierToRemove, false);
            }
        };
    }
    public static List<JetIntentionActionFactory> getIncompatibleModifiersFactories() {
        List<JetIntentionActionFactory> factories = new ArrayList<JetIntentionActionFactory>();
        for(JetKeywordToken modifier : VISIBILITY_MODIFIERS) {
            factories.add(createRemoveIncompatibleModifierFactory(modifier));
        }
        factories.add(createRemoveIncompatibleModifierFactory(JetTokens.OPEN_KEYWORD));
        factories.add(createRemoveIncompatibleModifierFactory(JetTokens.ABSTRACT_KEYWORD));
        factories.add(createRemoveIncompatibleModifierFactory(JetTokens.FINAL_KEYWORD));
        return factories;
    }
}
