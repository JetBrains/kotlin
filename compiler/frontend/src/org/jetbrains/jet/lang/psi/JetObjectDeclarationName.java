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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class JetObjectDeclarationName extends JetExpressionImpl {
    public JetObjectDeclarationName(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public String getName() {
        PsiElement identifier = getNameIdentifier();
        if (identifier != null) {
            String text = identifier.getText();
            return text != null ? JetPsiUtil.unquoteIdentifier(text) : null;
        }
        else {
            return null;
        }
    }

    public PsiElement getNameIdentifier() {
        return findChildByType(JetTokens.IDENTIFIER);
    }

    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return getNameIdentifier().replace(JetPsiFactory(this).createNameIdentifier(name));
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitObjectDeclarationName(this, data);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        JetObjectDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(this, JetObjectDeclaration.class);
        return objectDeclaration != null ? objectDeclaration.getUseScope() : super.getUseScope();
    }
}
