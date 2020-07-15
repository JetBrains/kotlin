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
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.Variance;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractReceiverParameterDescriptor extends DeclarationDescriptorImpl implements ReceiverParameterDescriptor {

    public AbstractReceiverParameterDescriptor(@NotNull Annotations annotations) {
        super(annotations, SpecialNames.THIS);
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) return this;

        KotlinType substitutedType;
        if (getContainingDeclaration() instanceof ClassDescriptor) {
            // Due to some reasons we check that receiver value type is a subtype of dispatch parameter
            // (although we get members exactly from it's scope)
            // So to make receiver with projections be a subtype of parameter's type with captured type arguments,
            // we approximate latter to it's upper bound.
            // See approximateDispatchReceiver.kt test for clarification
            substitutedType = substitutor.substitute(getType(), Variance.OUT_VARIANCE);
        }
        else {
            substitutedType = substitutor.substitute(getType(), Variance.INVARIANT);
        }

        if (substitutedType == null) return null;
        if (substitutedType == getType()) return this;

        return new ReceiverParameterDescriptorImpl(getContainingDeclaration(), new TransientReceiver(substitutedType), getAnnotations());
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitReceiverParameterDescriptor(this, data);
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getContextReceiverParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getExtensionReceiverParameter() {
        return null;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getDispatchReceiverParameter() {
        return null;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public KotlinType getReturnType() {
        return getType();
    }

    @NotNull
    @Override
    public KotlinType getType() {
        return getValue().getType();
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasStableParameterNames() {
        return false;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        return false;
    }

    @NotNull
    @Override
    public Collection<? extends CallableDescriptor> getOverriddenDescriptors() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public DescriptorVisibility getVisibility() {
        return DescriptorVisibilities.LOCAL;
    }

    @NotNull
    @Override
    public ParameterDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public SourceElement getSource() {
        return SourceElement.NO_SOURCE;
    }

    @Nullable
    @Override
    public <V> V getUserData(UserDataKey<V> key) {
        return null;
    }
}
