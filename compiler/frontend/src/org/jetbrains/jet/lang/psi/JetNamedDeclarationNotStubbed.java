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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

// TODO: Remove when all named declarations get stubs
@Deprecated
abstract class JetNamedDeclarationNotStubbed extends JetDeclarationImpl implements JetNamedDeclaration {
    public JetNamedDeclarationNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
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

    @Override
    public Name getNameAsName() {
        String name = getName();
        return name != null ? Name.identifier(name) : null;
    }

    @Override
    @NotNull
    public Name getNameAsSafeName() {
        return JetPsiUtil.safeName(getName());
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(JetTokens.IDENTIFIER);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return getNameIdentifier().replace(JetPsiFactory(this).createNameIdentifier(name));
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
    }
}
