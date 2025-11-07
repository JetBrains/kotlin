/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionTypeStub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KtFunctionType extends KtElementImplStub<KotlinFunctionTypeStub> implements KtTypeElement {

    public static final KtToken RETURN_TYPE_SEPARATOR = KtTokens.ARROW;

    public KtFunctionType(@NotNull ASTNode node) {
        super(node);
    }

    public KtFunctionType(@NotNull KotlinFunctionTypeStub stub) {
        super(stub, KtStubBasedElementTypes.FUNCTION_TYPE);
    }

    @NotNull
    @Override
    public List<KtTypeReference> getTypeArgumentsAsTypes() {
        List<KtTypeReference> result = Lists.newArrayList();
        List<KtTypeReference> contextReceiversTypeRefs = getContextReceiversTypeReferences();
        if (contextReceiversTypeRefs != null) {
            result.addAll(contextReceiversTypeRefs);
        }
        KtTypeReference receiverTypeRef = getReceiverTypeReference();
        if (receiverTypeRef != null) {
            result.add(receiverTypeRef);
        }
        for (KtParameter ktParameter : getParameters()) {
            result.add(ktParameter.getTypeReference());
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
    @SuppressWarnings("deprecation") // KT-78356
    public KtParameterList getParameterList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<KtParameter> getParameters() {
        KtParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtFunctionTypeReceiver getReceiver() {
        return getStubOrPsiChild(KtStubBasedElementTypes.FUNCTION_TYPE_RECEIVER);
    }

    @Nullable
    public KtTypeReference getReceiverTypeReference() {
        KtFunctionTypeReceiver receiverDeclaration = getReceiver();
        if (receiverDeclaration == null) {
            return null;
        }
        return receiverDeclaration.getTypeReference();
    }

    /**
     * Returns the context receiver list for this function type, if present.
     *
     * @return the context receiver list, or {@code null} if this function type has no context receivers
     * @deprecated Use {@link #getContextParameterList()} instead. This method is obsolete and exists for compatibility reasons only.
     */
    @Deprecated
    @Nullable
    public KtContextReceiverList getContextReceiverList() {
        return (KtContextReceiverList) getContextParameterList();
    }

    /**
     * Returns the context parameter list for this function type, if present.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Function type with context parameters
     * val executor: context(Logger, Database) (String) -> Unit = { }
     * }</pre>
     *
     * @return the context parameter list, or {@code null} if this function type has no context parameters
     *
     * @see KtContextParameterList
     */
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtContextParameterList getContextParameterList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST);
    }

    public List<KtTypeReference> getContextReceiversTypeReferences() {
        KtContextParameterList contextReceiverList = getContextParameterList();
        if (contextReceiverList != null) {
            return contextReceiverList.typeReferences();
        } else {
            return Collections.emptyList();
        }
    }

    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE);
    }

    /**
     * @return the total number of parameters for a function type, including
     * context parameters, the function type receiver, and value parameters.
     */
    public int getTotalParameterCount() {
        int count = 0;
        KtContextParameterList contextReceiverList = getContextParameterList();
        if (contextReceiverList != null) {
            count += contextReceiverList.contextReceivers().size();
        }

        KtFunctionTypeReceiver receiverDeclaration = getReceiver();
        if (receiverDeclaration != null) {
            count++;
        }

        List<KtParameter> list = getParameters();
        count += list.size();

        return count;
    }
}
