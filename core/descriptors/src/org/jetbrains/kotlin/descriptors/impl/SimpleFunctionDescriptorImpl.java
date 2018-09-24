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

package org.jetbrains.kotlin.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimpleFunctionDescriptorImpl extends FunctionDescriptorImpl implements SimpleFunctionDescriptor {
    protected SimpleFunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, original, annotations, name, kind, source);
    }

    @NotNull
    public static SimpleFunctionDescriptorImpl create(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        return new SimpleFunctionDescriptorImpl(containingDeclaration, null, annotations, name, kind, source);
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptorImpl initialize(
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable KotlinType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility
    ) {
        return initialize(extensionReceiverParameter, dispatchReceiverParameter, typeParameters, unsubstitutedValueParameters,
                          unsubstitutedReturnType, modality, visibility, null);
    }

    @NotNull
    public SimpleFunctionDescriptorImpl initialize(
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable KotlinType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility,
            @Nullable Map<? extends UserDataKey<?>, ?> userData
    ) {
        super.initialize(extensionReceiverParameter, dispatchReceiverParameter, typeParameters, unsubstitutedValueParameters,
                         unsubstitutedReturnType, modality, visibility);

        if (userData != null && !userData.isEmpty()) {
            userDataMap = new LinkedHashMap<UserDataKey<?>, Object>(userData);
        }

        return this;
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor getOriginal() {
        return (SimpleFunctionDescriptor) super.getOriginal();
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
        return new SimpleFunctionDescriptorImpl(
                newOwner,
                (SimpleFunctionDescriptor) original,
                annotations,
                newName != null ? newName : getName(),
                kind,
                source
        );
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            Visibility visibility,
            Kind kind,
            boolean copyOverrides
    ) {
        return (SimpleFunctionDescriptor) super.copy(newOwner, modality, visibility, kind, copyOverrides);
    }

    @NotNull
    @Override
    public CopyBuilder<? extends SimpleFunctionDescriptor> newCopyBuilder() {
        //noinspection unchecked
        return (CopyBuilder<? extends SimpleFunctionDescriptor>) super.newCopyBuilder();
    }
}
