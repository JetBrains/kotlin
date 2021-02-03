/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitution;

import java.util.Collection;
import java.util.List;

public class ErrorSimpleFunctionDescriptorImpl extends SimpleFunctionDescriptorImpl {
    // used for diagnostic only
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
    private final ErrorUtils.ErrorScope ownerScope;

    public ErrorSimpleFunctionDescriptorImpl(@NotNull ClassDescriptor containingDeclaration, @NotNull ErrorUtils.ErrorScope ownerScope) {
        super(containingDeclaration, null, Annotations.Companion.getEMPTY(), Name.special("<ERROR FUNCTION>"), Kind.DECLARATION, SourceElement.NO_SOURCE);
        this.ownerScope = ownerScope;
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        return this;
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, DescriptorVisibility visibility, Kind kind, boolean copyOverrides) {
        return this;
    }

    @NotNull
    @Override
    public CopyBuilder<? extends SimpleFunctionDescriptor> newCopyBuilder() {
        return new CopyBuilder<SimpleFunctionDescriptor>() {
            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setOwner(@NotNull DeclarationDescriptor owner) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setModality(@NotNull Modality modality) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setVisibility(@NotNull DescriptorVisibility visibility) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setKind(@NotNull Kind kind) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setCopyOverrides(boolean copyOverrides) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setName(@NotNull Name name) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setValueParameters(@NotNull List<ValueParameterDescriptor> parameters) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setSubstitution(@NotNull TypeSubstitution substitution) {
                return this;
            }

            @NotNull
            @Override
            public <V> CopyBuilder<SimpleFunctionDescriptor> putUserData(
                    @NotNull UserDataKey<V> userDataKey,
                    V value
            ) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setReturnType(@NotNull KotlinType type) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setContextReceiverParameters(@NotNull List<ReceiverParameterDescriptor> contextReceiverParameters) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setExtensionReceiverParameter(@Nullable ReceiverParameterDescriptor extensionReceiverParameter) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setOriginal(@Nullable CallableMemberDescriptor original) {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setSignatureChange() {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setPreserveSourceElement() {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setDropOriginalInContainingParts() {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setHiddenToOvercomeSignatureClash() {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setHiddenForResolutionEverywhereBesideSupercalls() {
                return this;
            }

            @NotNull
            @Override
            public CopyBuilder<SimpleFunctionDescriptor> setAdditionalAnnotations(@NotNull Annotations additionalAnnotations) {
                return this;
            }

            @Nullable
            @Override
            public SimpleFunctionDescriptor build() {
                return ErrorSimpleFunctionDescriptorImpl.this;
            }
        };
    }

    @Override
    public boolean isSuspend() {
        return false;
    }

    @Override
    public <V> V getUserData(UserDataKey<V> key) {
        return null;
    }

    @Override
    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {
        // nop
    }
}
