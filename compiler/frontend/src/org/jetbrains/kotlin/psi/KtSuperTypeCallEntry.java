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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Collections;
import java.util.List;

public class KtSuperTypeCallEntry extends KtSuperTypeListEntry implements KtCallElement {
    public KtSuperTypeCallEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtSuperTypeCallEntry(@NotNull KotlinPlaceHolderStub<? extends KtSuperTypeListEntry> stub) {
        super(stub, KtStubElementTypes.SUPER_TYPE_CALL_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeCallEntry(this, data);
    }

    @NotNull
    @Override
    public KtConstructorCalleeExpression getCalleeExpression() {
        return getRequiredStubOrPsiChild(KtStubElementTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    @Nullable
    public KtValueArgumentList getValueArgumentList() {
        return (KtValueArgumentList) findChildByType(KtNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        KtValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<KtValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<KtLambdaArgument> getLambdaArguments() {
        return Collections.emptyList();
    }

    @Override
    public KtTypeReference getTypeReference() {
        return getCalleeExpression().getTypeReference();
    }

    @NotNull
    @Override
    public List<KtTypeProjection> getTypeArguments() {
        KtTypeArgumentList typeArgumentList = getTypeArgumentList();
        if (typeArgumentList == null) {
            return Collections.emptyList();
        }
        return typeArgumentList.getArguments();
    }

    @Override
    public KtTypeArgumentList getTypeArgumentList() {
        KtUserType userType = getTypeAsUserType();
        return userType != null ? userType.getTypeArgumentList() : null;
    }

}
