
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
import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.Collection;


public class RemoveUnusedParameterFix extends JetIntentionAction<JetParameter>{
    private final JetParameter parameter;

    public RemoveUnusedParameterFix(@NotNull JetParameter parameter) {
        super(parameter);
        this.parameter = parameter;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.unused.parameter", parameter.getName());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.parameter.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file);
    }

    private boolean removeComma(@NotNull ASTDelegatePsiElement element, @Nullable PsiElement
            possiblyComma) {
        if (possiblyComma instanceof LeafPsiElement && ((LeafPsiElement)possiblyComma).getElementType()
                                                       == JetTokens.COMMA) {
            QuickFixUtil.removePossiblyWhiteSpace(element, possiblyComma.getNextSibling());
            element.deleteChildInternal(possiblyComma.getNode());
            return true;
        }
        return false;
    }

    private void removeWithComma(PsiElement element) {
        assert element instanceof ASTDelegatePsiElement;
        ASTDelegatePsiElement ASTElement = (ASTDelegatePsiElement) element;
        if (element.getNextSibling() != null)
            QuickFixUtil.removePossiblyWhiteSpace(ASTElement, element.getNextSibling());
        if (element.getNextSibling() == null || !removeComma(ASTElement, element.getNextSibling())) {
            if (element.getPrevSibling() != null) {
                QuickFixUtil.removePossiblyWhiteSpace(ASTElement, element.getPrevSibling());
                if (element.getPrevSibling() != null) {
                    removeComma(ASTElement, element.getPrevSibling());
                }
            }
        }
        element.delete();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final JetFunction function = (JetFunction) parameter.getParent().getParent();
        int nr = -1;
        for(JetParameter param: function.getValueParameterList().getParameters()) {
            ++nr;
            if (param == parameter) break;
        }

        Collection<PsiReference> refs = ReferencesSearch.search(function).findAll();
        for (PsiReference ref: refs) {
            PsiElement e = ref.getElement();
            JetCallExpression callExp = PsiTreeUtil.getParentOfType(e, JetCallExpression.class);
            if (callExp != null && callExp.isValid()) {
                removeWithComma(callExp.getValueArgumentList().getArguments().get(nr));
            }
        }
        removeWithComma(parameter);
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetParameter parameter = QuickFixUtil.getParentElementOfType(diagnostic, JetParameter.class);
                return parameter == null ? null : new RemoveUnusedParameterFix(parameter);
            }
        };
    }
}
