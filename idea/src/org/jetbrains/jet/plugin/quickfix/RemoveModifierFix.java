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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

/**
* @author svtk
*/
public class RemoveModifierFix {
    private final JetKeywordToken modifier;
    private final boolean isRedundant;

    public RemoveModifierFix(JetKeywordToken modifier, boolean isRedundant) {
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
    
    private static String getFamilyName() {
        return JetBundle.message("remove.modifier.family");
    }

    @NotNull
    private static <T extends JetModifierListOwner> T removeModifier(T element, JetToken modifier) {
        JetModifierList modifierList = element.getModifierList();
        assert modifierList != null;
        removeModifierFromList(modifierList, modifier);
        if (modifierList.getFirstChild() == null) {
            PsiElement whiteSpace = modifierList.getNextSibling();
            assert element instanceof JetElement;
            ((JetElement) element).deleteChildInternal(modifierList.getNode());
            QuickFixUtil.removePossiblyWhiteSpace((JetElement) element, whiteSpace);
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
    
    private class RemoveModifierFromListOwner extends JetIntentionAction<JetModifierListOwner> {
        public RemoveModifierFromListOwner(@NotNull JetModifierListOwner element) {
            super(element);
        }

        @NotNull
        @Override
        public String getText() {
            return makeText(element, modifier, isRedundant);
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return RemoveModifierFix.getFamilyName();
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            JetModifierListOwner newElement = (JetModifierListOwner) element.copy();
            element.replace(removeModifier(newElement, modifier));
        }
    }

    private class RemoveModifierFromList extends JetIntentionAction<JetModifierList> {
        public RemoveModifierFromList(@NotNull JetModifierList element) {
            super(element);
        }

        @NotNull
        @Override
        public String getText() {
            return makeText(null, modifier, isRedundant);
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return RemoveModifierFix.getFamilyName();
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            JetModifierList newElement = (JetModifierList) element.copy();
            element.replace(RemoveModifierFix.removeModifierFromList(newElement, modifier));
        }
    }


    public static JetIntentionActionFactory<JetModifierListOwner> createRemoveModifierFromListOwnerFactory(final JetKeywordToken modifier, final boolean isRedundant) {
        return new JetIntentionActionFactory<JetModifierListOwner>() {
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierListOwner;
                return new RemoveModifierFix(modifier, isRedundant).new RemoveModifierFromListOwner((JetModifierListOwner) diagnostic.getPsiElement());
            }
        };
    }
    
    private static RemoveModifierFix createRemoveModifierFixFromDiagnostic(DiagnosticWithPsiElement diagnostic, boolean isRedundant) {
        DiagnosticWithParameters<PsiElement> diagnosticWithParameters = JetIntentionAction.assertAndCastToDiagnosticWithParameters(diagnostic, DiagnosticParameters.MODIFIER);
        JetKeywordToken modifier = diagnosticWithParameters.getParameter(DiagnosticParameters.MODIFIER);
        return new RemoveModifierFix(modifier, isRedundant);
    }

    public static JetIntentionActionFactory<JetModifierListOwner> createRemoveModifierFromListOwnerFactory(final boolean isRedundant) {
        return new JetIntentionActionFactory<JetModifierListOwner>() {
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierListOwner;
                return createRemoveModifierFixFromDiagnostic(diagnostic, isRedundant).new RemoveModifierFromListOwner((JetModifierListOwner) diagnostic.getPsiElement());
            }
        };
    }

    public static JetIntentionActionFactory<JetModifierList> createRemoveModifierFromListFactory(final boolean isRedundant) {
        return new JetIntentionActionFactory<JetModifierList>() {
            @Override
            public JetIntentionAction<JetModifierList> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierList;
                return createRemoveModifierFixFromDiagnostic(diagnostic, isRedundant).new RemoveModifierFromList((JetModifierList) diagnostic.getPsiElement());
            }
        };
    }

    public static JetIntentionActionFactory<JetModifierList> createRemoveModifierFromListFactory(final JetKeywordToken modifier, final boolean isRedundant) {
        return new JetIntentionActionFactory<JetModifierList>() {
            @Override
            public JetIntentionAction<JetModifierList> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierList;
                return new RemoveModifierFix(modifier, isRedundant).new RemoveModifierFromList((JetModifierList) diagnostic.getPsiElement());
            }
        };
    }

    public static JetIntentionActionFactory<JetModifierListOwner> createRemoveModifierFromListOwnerFactory(final JetKeywordToken modifier) {
        return createRemoveModifierFromListOwnerFactory(modifier, false);
    }

    public static JetIntentionActionFactory<JetModifierList> createRemoveModifierFromListFactory() {
        return createRemoveModifierFromListFactory(false);
    }

    public static JetIntentionActionFactory<JetModifierList> createRemoveModifierFromListFactory(final JetKeywordToken modifier) {
        return createRemoveModifierFromListFactory(modifier, false);
    }
}
