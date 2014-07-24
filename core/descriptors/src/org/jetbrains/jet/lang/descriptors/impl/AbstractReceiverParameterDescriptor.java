/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractReceiverParameterDescriptor extends DeclarationDescriptorImpl implements ReceiverParameterDescriptor {
    private static final Name RECEIVER_PARAMETER_NAME = Name.special("<this>");

    public AbstractReceiverParameterDescriptor() {
        super(Annotations.EMPTY, RECEIVER_PARAMETER_NAME);
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) return this;
        JetType substitutedType = substitutor.substitute(getType(), Variance.INVARIANT);
        if (substitutedType == null) return null;

        return new ReceiverParameterDescriptorImpl(getContainingDeclaration(), substitutedType, new TransientReceiver(substitutedType));
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitReceiverParameterDescriptor(this, data);
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getReceiverParameter() {
        return null;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getExpectedThisObject() {
        return null;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public JetType getReturnType() {
        return getType();
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
    public Set<? extends CallableDescriptor> getOverriddenDescriptors() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibilities.LOCAL;
    }

    @NotNull
    @Override
    public CallableDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public SourceElement getSource() {
        return SourceElement.NO_SOURCE;
    }
}
