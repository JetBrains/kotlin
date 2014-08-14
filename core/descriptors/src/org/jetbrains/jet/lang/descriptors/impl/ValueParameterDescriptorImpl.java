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
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ValueParameterDescriptorImpl extends VariableDescriptorImpl implements ValueParameterDescriptor {
    private Boolean hasDefaultValue;
    private final boolean declaresDefaultValue;
    private final JetType varargElementType;
    private final int index;
    private final ValueParameterDescriptor original;
    private final Set<ValueParameterDescriptor> overriddenDescriptors = new LinkedHashSet<ValueParameterDescriptor>(); // Linked is essential
    private boolean overriddenDescriptorsLocked = false;
    private final Set<? extends ValueParameterDescriptor> readOnlyOverriddenDescriptors = Collections.unmodifiableSet(overriddenDescriptors);

    public ValueParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable ValueParameterDescriptor original,
            int index,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull JetType outType,
            boolean declaresDefaultValue,
            @Nullable JetType varargElementType,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, outType, source);
        this.original = original == null ? this : original;
        this.index = index;
        this.declaresDefaultValue = declaresDefaultValue;
        this.varargElementType = varargElementType;
    }

    public void setType(@NotNull JetType type) {
        setOutType(type);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean hasDefaultValue() {
        computeDefaultValuePresence();
        return hasDefaultValue;
    }

    @Override
    public boolean declaresDefaultValue() {
        return declaresDefaultValue && ((CallableMemberDescriptor) getContainingDeclaration()).getKind().isReal();
    }

    private void computeDefaultValuePresence() {
        if (hasDefaultValue != null) return;
        overriddenDescriptorsLocked = true;
        if (declaresDefaultValue) {
            hasDefaultValue = true;
        }
        else {
            for (ValueParameterDescriptor descriptor : overriddenDescriptors) {
                if (descriptor.hasDefaultValue()) {
                    hasDefaultValue = true;
                    return;
                }
            }
            hasDefaultValue = false;
        }
    }

    @Nullable
    @Override
    public JetType getVarargElementType() {
        return varargElementType;
    }

    @NotNull
    @Override
    public ValueParameterDescriptor getOriginal() {
        return original == this ? this : original.getOriginal();
    }

    @NotNull
    @Override
    public ValueParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitValueParameterDescriptor(this, data);
    }

    @Override
    public boolean isVar() {
        return false;
    }

    @NotNull
    @Override
    public ValueParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner, @NotNull Name newName) {
        return new ValueParameterDescriptorImpl(newOwner, null, index, getAnnotations(), newName, getType(), declaresDefaultValue(), varargElementType, SourceElement.NO_SOURCE);
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibilities.LOCAL;
    }

    @NotNull
    @Override
    public Set<? extends ValueParameterDescriptor> getOverriddenDescriptors() {
        return readOnlyOverriddenDescriptors;
    }

    @Override
    public void addOverriddenDescriptor(@NotNull ValueParameterDescriptor overridden) {
        assert !overriddenDescriptorsLocked : "Adding more overridden descriptors is not allowed at this point: " +
                                              "the presence of the default value has already been calculated";
        overriddenDescriptors.add(overridden);
    }
}
