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
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtBackingField extends KtDeclarationStub<KotlinBackingFieldStub>
        implements KtModifierListOwner, KtDeclarationWithInitializer {
    public KtBackingField(@NotNull ASTNode node) {
        super(node);
    }

    public KtBackingField(@NotNull KotlinBackingFieldStub stub) {
        super(stub, KtStubElementTypes.BACKING_FIELD);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBackingField(this, data);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Nullable
    public KtTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    public PsiElement getNamePlaceholder() {
        PsiElement it = getFieldKeyword();
        if (it != null) {
            return it;
        }
        return getNode().getPsi();
    }

    @Nullable
    @Override
    public KtExpression getInitializer() {
        KotlinBackingFieldStub stub = getStub();
        if (stub != null && !stub.hasInitializer()) {
            return null;
        }
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), KtExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        KotlinBackingFieldStub stub = getStub();
        if (stub != null) {
            return stub.hasInitializer();
        }
        return getInitializer() != null;
    }

    @Override
    public int getTextOffset() {
        return getNamePlaceholder().getTextRange().getStartOffset();
    }

    public PsiElement getFieldKeyword() {
        return findChildByType(KtTokens.FIELD_KEYWORD);
    }
}
