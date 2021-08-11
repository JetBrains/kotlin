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
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KtFunctionType extends KtElementImplStub<KotlinPlaceHolderStub<KtFunctionType>> implements KtTypeElement {

    public static final KtToken RETURN_TYPE_SEPARATOR = KtTokens.ARROW;

    public KtFunctionType(@NotNull ASTNode node) {
        super(node);
    }

    public KtFunctionType(@NotNull KotlinPlaceHolderStub<KtFunctionType> stub) {
        super(stub, KtStubElementTypes.FUNCTION_TYPE);
    }

    @NotNull
    @Override
    public List<KtTypeReference> getTypeArgumentsAsTypes() {
        ArrayList<KtTypeReference> result = Lists.newArrayList();
        List<KtTypeReference> contextReceiversTypeRefs = getContextReceiversTypeReferences();
        if (contextReceiversTypeRefs != null) {
            result.addAll(contextReceiversTypeRefs);
        }
        KtTypeReference receiverTypeRef = getReceiverTypeReference();
        if (receiverTypeRef != null) {
            result.add(receiverTypeRef);
        }
        for (KtParameter jetParameter : getParameters()) {
            result.add(jetParameter.getTypeReference());
        }
        KtTypeReference returnTypeRef = getReturnTypeReference();
        if (returnTypeRef != null) {
            result.add(returnTypeRef);
        }
        return result;
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionType(this, data);
    }

    @Nullable
    public KtParameterList getParameterList() {
        return getStubOrPsiChild(KtStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<KtParameter> getParameters() {
        KtParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Nullable
    public KtFunctionTypeReceiver getReceiver() {
        return getStubOrPsiChild(KtStubElementTypes.FUNCTION_TYPE_RECEIVER);
    }

    @Nullable
    public KtTypeReference getReceiverTypeReference() {
        KtFunctionTypeReceiver receiverDeclaration = getReceiver();
        if (receiverDeclaration == null) {
            return null;
        }
        return receiverDeclaration.getTypeReference();
    }

    public List<KtTypeReference> getContextReceiversTypeReferences() {
        KtFunctionTypeReceiver receiverDeclaration = getReceiver();
        if (receiverDeclaration == null) {
            return null;
        }
        KtTypeReference receiverTypeRef = receiverDeclaration.getTypeReference();
        KtContextReceiverList contextReceiverList = receiverTypeRef.getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST);
        if (contextReceiverList != null) {
            return contextReceiverList.typeReferences();
        } else {
            return Collections.emptyList();
        }
    }

    @Nullable
    public KtTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }
}
