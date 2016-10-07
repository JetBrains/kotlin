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
     * This method should be used with q Wgreat care, because if descriptor is substituted one, calling 'getOverriddenDescriptors'
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

    boolean isExternal();

    boolean isHiddenForResolutionEverywhereBesideSupercalls();

    boolean isSuspend();

    interface UserDataKey<V> {}

    // TODO: pull up userdata related members to DeclarationDescriptor and use more efficient implementation (e.g. THashMap)
    @Nullable
    <V> V getUserData(UserDataKey<V> key);

    @NotNull
    CopyBuilder<? extends FunctionDescriptor> newCopyBuilder();

    interface CopyBuilder<D extends FunctionDescriptor> {
        @NotNull
        CopyBuilder<D> setOwner(@NotNull DeclarationDescriptor owner);

        @NotNull
        CopyBuilder<D> setModality(@NotNull Modality modality);

        @NotNull
        CopyBuilder<D> setVisibility(@NotNull Visibility visibility);

        @NotNull
        CopyBuilder<D> setKind(@NotNull Kind kind);

        @NotNull
        CopyBuilder<D> setCopyOverrides(boolean copyOverrides);

        @NotNull
        CopyBuilder<D> setName(@NotNull Name name);

        @NotNull
        CopyBuilder<D> setValueParameters(@NotNull List<ValueParameterDescriptor> parameters);

        @NotNull
        CopyBuilder<D> setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters);

        @NotNull
        CopyBuilder<D> setReturnType(@NotNull KotlinType type);

        @NotNull
        CopyBuilder<D> setExtensionReceiverType(@Nullable KotlinType type);

        @NotNull
        CopyBuilder<D> setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter);

        @NotNull
        CopyBuilder<D> setOriginal(@Nullable FunctionDescriptor original);

        @NotNull
        CopyBuilder<D> setSignatureChange();

        @NotNull
        CopyBuilder<D> setPreserveSourceElement();

        @NotNull
        CopyBuilder<D> setSource(@NotNull SourceElement source);

        @NotNull
        CopyBuilder<D> setDropOriginalInContainingParts();

        @NotNull
        CopyBuilder<D> setHiddenToOvercomeSignatureClash();

        @NotNull
        CopyBuilder<D> setHiddenForResolutionEverywhereBesideSupercalls();

        @NotNull
        CopyBuilder<D> setAdditionalAnnotations(@NotNull Annotations additionalAnnotations);

        @NotNull
        CopyBuilder<D> setSubstitution(@NotNull TypeSubstitution substitution);

        @NotNull
        <V> CopyBuilder<D> putUserData(@NotNull UserDataKey<V> userDataKey, V value);

        @Nullable
        D build();
    }
}
