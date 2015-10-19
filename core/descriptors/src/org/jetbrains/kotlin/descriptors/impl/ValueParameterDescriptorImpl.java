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

import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.types.KtType;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.Collection;

public class ValueParameterDescriptorImpl extends VariableDescriptorImpl implements ValueParameterDescriptor {
    private final boolean declaresDefaultValue;
    private final boolean isCrossinline;
    private final boolean isNoinline;
    private final KtType varargElementType;
    private final int index;
    private final ValueParameterDescriptor original;

    public ValueParameterDescriptorImpl(
            @NotNull CallableDescriptor containingDeclaration,
            @Nullable ValueParameterDescriptor original,
            int index,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull KtType outType,
            boolean declaresDefaultValue,
            boolean isCrossinline,
            boolean isNoinline,
            @Nullable KtType varargElementType,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, outType, source);
        this.original = original == null ? this : original;
        this.index = index;
        this.declaresDefaultValue = declaresDefaultValue;
        this.isCrossinline = isCrossinline;
        this.isNoinline = isNoinline;
        this.varargElementType = varargElementType;
    }

    @NotNull
    @Override
    public CallableDescriptor getContainingDeclaration() {
        return (CallableDescriptor) super.getContainingDeclaration();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean declaresDefaultValue() {
        return declaresDefaultValue && ((CallableMemberDescriptor) getContainingDeclaration()).getKind().isReal();
    }

    @Override
    public boolean isCrossinline() {
        return isCrossinline;
    }

    @Override
    public boolean isNoinline() {
        return isNoinline;
    }

    @Nullable
    @Override
    public KtType getVarargElementType() {
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
        if (substitutor.isEmpty()) return this;
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

    @Nullable
    @Override
    public ConstantValue<?> getCompileTimeInitializer() {
        return null;
    }

    @NotNull
    @Override
    public ValueParameterDescriptor copy(@NotNull CallableDescriptor newOwner, @NotNull Name newName) {
        return new ValueParameterDescriptorImpl(
                newOwner, null, index, getAnnotations(), newName, getType(), declaresDefaultValue(), isCrossinline(), isNoinline(), varargElementType,
                SourceElement.NO_SOURCE
        );
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibilities.LOCAL;
    }

    @NotNull
    @Override
    public Collection<? extends ValueParameterDescriptor> getOverriddenDescriptors() {
        return CollectionsKt.map(
                getContainingDeclaration().getOverriddenDescriptors(),
                new Function1<CallableDescriptor, ValueParameterDescriptor>() {
                    @Override
                    public ValueParameterDescriptor invoke(CallableDescriptor descriptor) {
                        return descriptor.getValueParameters().get(getIndex());
                    }
                });
    }
}
