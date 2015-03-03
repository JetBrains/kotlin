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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinStubWithFqName;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.lexer.JetTokens;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

abstract class JetNamedDeclarationStub<T extends KotlinStubWithFqName> extends JetDeclarationStub<T> implements JetNamedDeclaration {
    public JetNamedDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public JetNamedDeclarationStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        T stub = getStub();
        if (stub != null) {
            return stub.getName();
        }

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

    @NotNull
    @Override
    public SearchScope getUseScope() {
        JetElement enclosingBlock = JetPsiUtil.getEnclosingElementForLocalDeclaration(this, false);
        if (enclosingBlock != null) return new LocalSearchScope(enclosingBlock);

        if (hasModifier(JetTokens.PRIVATE_KEYWORD)) {
            JetElement containingClass = PsiTreeUtil.getParentOfType(this, JetClassOrObject.class);
            if (containingClass instanceof JetObjectDeclaration && ((JetObjectDeclaration) containingClass).isDefault()) {
                JetElement defaultObjectClass = PsiTreeUtil.getParentOfType(containingClass, JetClassOrObject.class);
                if (defaultObjectClass != null) {
                    containingClass = defaultObjectClass;
                }
            }
            if (containingClass != null) {
                return new LocalSearchScope(containingClass);
            }
        }

        return super.getUseScope();
    }

    @Nullable
    @Override
    public FqName getFqName() {
        // TODO: stubs do not agree with PSI here in case where there's no name:
        // stubs return a normalized name, and PSI returns null
        T stub = getStub();
        if (stub != null) {
            return stub.getFqName();
        }
        return JetNamedDeclarationUtil.getFQName(this);
    }
}
