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
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.types.Variance;

public class KtTypeParameter extends KtNamedDeclarationStub<KotlinTypeParameterStub> {

    public KtTypeParameter(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeParameter(@NotNull KotlinTypeParameterStub stub) {
        super(stub, KtStubElementTypes.TYPE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameter(this, data);
    }

    @NotNull
    public Variance getVariance() {
        KotlinTypeParameterStub stub = getStub();
        if (stub != null) {
            if (stub.isOutVariance()) return Variance.OUT_VARIANCE;
            if (stub.isInVariance()) return Variance.IN_VARIANCE;
            return Variance.INVARIANT;
        }

        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Variance.INVARIANT;

        if (modifierList.hasModifier(KtTokens.OUT_KEYWORD)) return Variance.OUT_VARIANCE;
        if (modifierList.hasModifier(KtTokens.IN_KEYWORD)) return Variance.IN_VARIANCE;
        return Variance.INVARIANT;
    }

    @Nullable
    public KtTypeReference setExtendsBound(@Nullable KtTypeReference typeReference) {
        KtTypeReference currentExtendsBound = getExtendsBound();
        if (currentExtendsBound != null) {
            if (typeReference == null) {
                PsiElement colon = findChildByType(KtTokens.COLON);
                if (colon != null) colon.delete();
                currentExtendsBound.delete();
                return null;
            }
            return (KtTypeReference) currentExtendsBound.replace(typeReference);
        }

        if (typeReference != null) {
            PsiElement colon = addAfter(new KtPsiFactory(getProject()).createColon(), getNameIdentifier());
            return (KtTypeReference) addAfter(typeReference, colon);
        }

        return null;
    }

    @Nullable
    public KtTypeReference getExtendsBound() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtTypeParameterListOwner owner = PsiTreeUtil.getParentOfType(this, KtTypeParameterListOwner.class);
        return new LocalSearchScope(owner != null ? owner : this);
    }
}
