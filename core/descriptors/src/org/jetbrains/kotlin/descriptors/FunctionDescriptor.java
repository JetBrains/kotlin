/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitution;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;

public interface FunctionDescriptor extends CallableMemberDescriptor {
    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @NotNull
    @Override
    FunctionDescriptor getOriginal();

    @Nullable
    @Override
    FunctionDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    /**
     * This method should be used with a great care, because if descriptor is substituted one, calling 'getOverriddenDescriptors'
     * may force lazy computation, that's unnecessary in most cases.
     * So, if 'getOriginal().getOverriddenDescriptors()' is enough for you, please use it instead.
     * @return
     */
    @Override
    @NotNull
    Collection<? extends FunctionDescriptor> getOverriddenDescriptors();

    /**
     * @return descriptor that represents initial signature, e.g in case of result SimpleFunctionDescriptor.createRenamedCopy it returns
     * descriptor before rename
     */
    @Nullable
    FunctionDescriptor getInitialSignatureDescriptor();

    /**
     * @return true if descriptor signature clashed with some other signature and it's supposed to be legal
     * See java.nio.CharBuffer
     */
    boolean isHiddenToOvercomeSignatureClash();

    @NotNull
    @Override
    FunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, Visibility visibility, Kind kind, boolean copyOverrides);

    boolean isOperator();

    boolean isInfix();

    boolean isInline();

    boolean isTailrec();

    boolean isHiddenForResolutionEverywhereBesideSupercalls();

    boolean isSuspend();

    interface UserDataKey<V> {}

    // TODO: pull up userdata related members to DeclarationDescriptor and use more efficient implementation (e.g. THashMap)
    @Nullable
    <V> V getUserData(UserDataKey<V> key);

    @NotNull
    @Override
    CopyBuilder<? extends FunctionDescriptor> newCopyBuilder();

    interface CopyBuilder<D extends FunctionDescriptor> extends CallableMemberDescriptor.CopyBuilder<D> {
        @NotNull
        @Override
        CopyBuilder<D> setOwner(@NotNull DeclarationDescriptor owner);

        @NotNull
        @Override
        CopyBuilder<D> setModality(@NotNull Modality modality);

        @NotNull
        @Override
        CopyBuilder<D> setVisibility(@NotNull Visibility visibility);

        @NotNull
        @Override
        CopyBuilder<D> setKind(@NotNull Kind kind);

        @NotNull
        @Override
        CopyBuilder<D> setCopyOverrides(boolean copyOverrides);

        @Override
        @NotNull
        CopyBuilder<D> setName(@NotNull Name name);

        @NotNull
        CopyBuilder<D> setValueParameters(@NotNull List<ValueParameterDescriptor> parameters);

        @NotNull
        @Override
        CopyBuilder<D> setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters);

        @NotNull
        CopyBuilder<D> setReturnType(@NotNull KotlinType type);

        @NotNull
        CopyBuilder<D> setExtensionReceiverParameter(@Nullable ReceiverParameterDescriptor extensionReceiverParameter);

        @NotNull
        @Override
        CopyBuilder<D> setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter);

        @NotNull
        @Override
        CopyBuilder<D> setOriginal(@Nullable CallableMemberDescriptor original);

        @NotNull
        CopyBuilder<D> setSignatureChange();

        @NotNull
        CopyBuilder<D> setPreserveSourceElement();

        @NotNull
        CopyBuilder<D> setDropOriginalInContainingParts();

        @NotNull
        CopyBuilder<D> setHiddenToOvercomeSignatureClash();

        @NotNull
        CopyBuilder<D> setHiddenForResolutionEverywhereBesideSupercalls();

        @NotNull
        CopyBuilder<D> setAdditionalAnnotations(@NotNull Annotations additionalAnnotations);

        @NotNull
        @Override
        CopyBuilder<D> setSubstitution(@NotNull TypeSubstitution substitution);

        @NotNull
        <V> CopyBuilder<D> putUserData(@NotNull UserDataKey<V> userDataKey, V value);

        @Nullable
        @Override
        D build();
    }
}
