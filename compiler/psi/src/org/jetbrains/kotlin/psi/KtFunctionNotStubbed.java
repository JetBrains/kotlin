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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
public abstract class KtFunctionNotStubbed extends KtTypeParameterListOwnerNotStubbed implements KtFunction {

    public KtFunctionNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public KtParameterList getValueParameterList() {
        return (KtParameterList) findChildByType(KtNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<KtParameter> getValueParameters() {
        KtParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Override
    @Nullable
    public KtExpression getBodyExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return false;
    }

    @Override
    @Nullable
    public KtTypeReference getReceiverTypeReference() {
        return null;
    }

    @NotNull
    @Override
    public List<KtContextReceiver> getContextReceivers() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public KtTypeReference getTypeReference() {
        return null;
    }

    @Nullable
    @Override
    public KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef) {
        if (typeRef == null) return null;
        throw new IllegalStateException("Lambda expressions can't have type reference");
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return null;
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof KtFile || parent instanceof KtClassBody);
    }
}
