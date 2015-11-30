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
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.List;

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
            @Nullable KotlinType receiverParameterType,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable KotlinType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility
    ) {
        super.initialize(receiverParameterType, dispatchReceiverParameter, typeParameters, unsubstitutedValueParameters,
                         unsubstitutedReturnType, modality, visibility);
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
            boolean preserveSource
    ) {
        return new SimpleFunctionDescriptorImpl(
                newOwner,
                (SimpleFunctionDescriptor) original,
                // TODO : safeSubstitute
                getAnnotations(),
                newName != null ? newName : getName(),
                kind,
                getSourceToUseForCopy(preserveSource, original)
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
        return (SimpleFunctionDescriptorImpl) doSubstitute(
                TypeSubstitutor.EMPTY, newOwner, modality, visibility,
                isOperator(), isInfix(), isExternal(), isInline(), isTailrec(),
                hasStableParameterNames(), hasSynthesizedParameterNames(),
                null, copyOverrides, kind
        );
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor createRenamedCopy(@NotNull Name name) {
        //noinspection ConstantConditions
        return (SimpleFunctionDescriptorImpl) doSubstitute(
                TypeSubstitutor.EMPTY, getContainingDeclaration(), getModality(), getVisibility(),
                isOperator(), isInfix(), isExternal(), isInline(), isTailrec(), hasStableParameterNames(), hasSynthesizedParameterNames(),
                null, /* copyOverrides = */ true, getKind(), getValueParameters(), getExtensionReceiverParameterType(), getReturnType(), name,
                /* preserveSource = */ true, /* signatureChange = */ true);
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor createCopyWithNewValueParameters(@NotNull List<ValueParameterDescriptor> valueParameters) {
        //noinspection ConstantConditions
        return (SimpleFunctionDescriptorImpl) doSubstitute(
                TypeSubstitutor.EMPTY, getContainingDeclaration(), getModality(), getVisibility(),
                isOperator(), isInfix(), isExternal(), isInline(), isTailrec(), hasStableParameterNames(), hasSynthesizedParameterNames(),
                null, /* copyOverrides = */ true, getKind(), valueParameters, getExtensionReceiverParameterType(), getReturnType(), null,
                /* preserveSource = */ true, /* signatureChange = */ true);
    }
}
