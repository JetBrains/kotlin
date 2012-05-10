/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class SimpleFunctionDescriptorImpl extends FunctionDescriptorImpl implements SimpleFunctionDescriptor {

    private boolean isInline = false;

    public SimpleFunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            Kind kind) {
        super(containingDeclaration, annotations, name, kind);
    }

    private SimpleFunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull SimpleFunctionDescriptor original,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            Kind kind) {
        super(containingDeclaration, original, annotations, name, kind);
    }

    public SimpleFunctionDescriptorImpl initialize(
            @Nullable JetType receiverType,
            @NotNull ReceiverDescriptor expectedThisObject,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable JetType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility,
            boolean isInline) {
        super.initialize(receiverType, expectedThisObject, typeParameters, unsubstitutedValueParameters, unsubstitutedReturnType, modality, visibility);
        this.isInline = isInline;
        return this;
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor getOriginal() {
        return (SimpleFunctionDescriptor) super.getOriginal();
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        if (preserveOriginal) {
            return new SimpleFunctionDescriptorImpl(
                    newOwner,
                    getOriginal(),
                    // TODO : safeSubstitute
                    getAnnotations(),
                    getName(),
                    kind);
        }
        else {
            return new SimpleFunctionDescriptorImpl(
                    newOwner,
                    // TODO : safeSubstitute
                    getAnnotations(),
                    getName(),
                    kind);
        }
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides) {
        SimpleFunctionDescriptorImpl copy = (SimpleFunctionDescriptorImpl)doSubstitute(TypeSubstitutor.EMPTY, newOwner, DescriptorUtils
            .convertModality(modality, makeNonAbstract), false, copyOverrides, kind);
        copy.isInline = isInline;
        return copy;
    }

    @Override
    public boolean isInline() {
        return isInline;
    }
}
