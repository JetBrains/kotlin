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

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Collections;
import java.util.List;

public class KtUserType extends KtElementImplStub<KotlinUserTypeStub> implements KtTypeElement {
    public KtUserType(@NotNull ASTNode node) {
        super(node);
    }

    public KtUserType(@NotNull KotlinUserTypeStub stub) {
        super(stub, KtStubElementTypes.USER_TYPE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitUserType(this, data);
    }

    @Nullable
    public KtTypeArgumentList getTypeArgumentList() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_ARGUMENT_LIST);
    }

    @NotNull
    public List<KtTypeProjection> getTypeArguments() {
        // TODO: empty elements in PSI
        KtTypeArgumentList typeArgumentList = getTypeArgumentList();
        return typeArgumentList == null ? Collections.emptyList() : typeArgumentList.getArguments();
    }

    @NotNull
    @Override
    public List<KtTypeReference> getTypeArgumentsAsTypes() {
        List<KtTypeReference> result = Lists.newArrayList();
        for (KtTypeProjection projection : getTypeArguments()) {
            result.add(projection.getTypeReference());
        }
        return result;
    }

    @Nullable @IfNotParsed
    public KtSimpleNameExpression getReferenceExpression() {
        KtNameReferenceExpression nameRefExpr = getStubOrPsiChild(KtStubElementTypes.REFERENCE_EXPRESSION);
        return nameRefExpr != null ? nameRefExpr : getStubOrPsiChild(KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION);
    }

    @Nullable
    public KtUserType getQualifier() {
        return getStubOrPsiChild(KtStubElementTypes.USER_TYPE);
    }

    public void deleteQualifier() {
        KtUserType qualifier = getQualifier();
        assert qualifier != null;
        PsiElement dot = findChildByType(KtTokens.DOT);
        assert dot != null;
        qualifier.delete();
        dot.delete();
    }

    @Nullable
    public String getReferencedName() {
        KtSimpleNameExpression referenceExpression = getReferenceExpression();
        return referenceExpression == null ? null : referenceExpression.getReferencedName();
    }
}
