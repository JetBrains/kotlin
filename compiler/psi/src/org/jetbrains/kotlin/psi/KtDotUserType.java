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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinDotUserTypeStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Collections;
import java.util.List;

public class KtDotUserType extends KtElementImplStub<KotlinDotUserTypeStub> implements KtTypeElement {
    public KtDotUserType(@NotNull ASTNode node) {
        super(node);
    }

    public KtDotUserType(@NotNull KotlinDotUserTypeStub stub) {
        super(stub, KtStubElementTypes.USER_TYPE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDotUserType(this, data);
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
    public KtDotNameReferenceExpression getReferenceExpression() {
        return getStubOrPsiChild(KtStubElementTypes.DOT_REFERENCE_EXPRESSION);
    }

    @Nullable
    public String getReferencedName() {
        KtDotNameReferenceExpression referenceExpression = getReferenceExpression();
        return referenceExpression == null ? null : referenceExpression.getReferencedName();
    }
}
