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
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeProjectionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtTypeProjection extends KtModifierListOwnerStub<KotlinTypeProjectionStub> {

    public KtTypeProjection(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeProjection(@NotNull KotlinTypeProjectionStub stub) {
        super(stub, KtStubElementTypes.TYPE_PROJECTION);
    }

    @NotNull
    public KtProjectionKind getProjectionKind() {
        KotlinTypeProjectionStub stub = getStub();
        if (stub != null) {
            return stub.getProjectionKind();
        }

        PsiElement projectionToken = getProjectionToken();
        IElementType token = projectionToken != null ? projectionToken.getNode().getElementType() : null;
        for (KtProjectionKind projectionKind : KtProjectionKind.values()) {
            if (projectionKind.getToken() == token) {
                return projectionKind;
            }
        }
        throw new IllegalStateException(projectionToken.getText());
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeProjection(this, data);
    }

    @Nullable
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @Nullable
    public PsiElement getProjectionToken() {
        PsiElement star = findChildByType(KtTokens.MUL);
        if (star != null) {
            return star;
        }

        KtModifierList modifierList = getModifierList();
        if (modifierList != null) {
            PsiElement element = modifierList.getModifier(KtTokens.IN_KEYWORD);
            if (element != null) return element;

            element = modifierList.getModifier(KtTokens.OUT_KEYWORD);
            if (element != null) return element;
        }

        return null;
    }
}
