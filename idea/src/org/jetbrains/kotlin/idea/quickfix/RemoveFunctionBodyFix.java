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

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;

public class RemoveFunctionBodyFix extends KotlinQuickFixAction<KtFunction> {

    public RemoveFunctionBodyFix(@NotNull KtFunction element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("remove.function.body");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("remove.function.body");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && getElement().hasBody();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtFunction function = (KtFunction) getElement().copy();
        assert function instanceof ASTDelegatePsiElement;
        ASTDelegatePsiElement functionElementWithAst = (ASTDelegatePsiElement) function;
        KtExpression bodyExpression = function.getBodyExpression();
        assert bodyExpression != null;
        if (function.hasBlockBody()) {
            PsiElement prevElement = bodyExpression.getPrevSibling();
            QuickFixUtil.removePossiblyWhiteSpace(functionElementWithAst, prevElement);
            functionElementWithAst.deleteChildInternal(bodyExpression.getNode());
        }
        else {
            PsiElement prevElement = bodyExpression.getPrevSibling();
            PsiElement prevPrevElement = prevElement.getPrevSibling();
            QuickFixUtil.removePossiblyWhiteSpace(functionElementWithAst, prevElement);
            removePossiblyEquationSign(functionElementWithAst, prevElement);
            removePossiblyEquationSign(functionElementWithAst, prevPrevElement);
            functionElementWithAst.deleteChildInternal(bodyExpression.getNode());
        }
        getElement().replace(function);
    }

    private static boolean removePossiblyEquationSign(@NotNull ASTDelegatePsiElement element, @Nullable PsiElement possiblyEq) {
        if (possiblyEq instanceof LeafPsiElement && ((LeafPsiElement)possiblyEq).getElementType() == KtTokens.EQ) {
            QuickFixUtil.removePossiblyWhiteSpace(element, possiblyEq.getNextSibling());
            element.deleteChildInternal(possiblyEq.getNode());
            return true;
        }
        return false;
    }

    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtFunction> createAction(Diagnostic diagnostic) {
                KtFunction function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction.class);
                if (function == null) return null;
                return new RemoveFunctionBodyFix(function);
            }
        };
    }
}
