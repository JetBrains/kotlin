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

public class LocalVariableDescriptor extends VariableDescriptorWithInitializerImpl implements VariableDescriptorWithAccessors {
    private final boolean isDelegated;
    private LocalVariableAccessorDescriptor.Getter getter;
    private LocalVariableAccessorDescriptor.Setter setter;

    public LocalVariableDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @Nullable KotlinType type,
            boolean mutable,
            boolean isDelegated,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, type, mutable, source);
        this.isDelegated = isDelegated;
    }

    @Override
    public void setOutType(KotlinType outType) {
        super.setOutType(outType);
        if (isDelegated) {
            this.getter = new LocalVariableAccessorDescriptor.Getter(this);
            if (isVar()) {
                this.setter = new LocalVariableAccessorDescriptor.Setter(this);
            }
        }
    }

    @NotNull
    @Override
    public LocalVariableDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) return this;
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitVariableDescriptor(this, data);
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibilities.LOCAL;
    }

    @Nullable
    @Override
    public LocalVariableAccessorDescriptor.Getter getGetter() {
        return getter;
    }

    @Nullable
    @Override
    public LocalVariableAccessorDescriptor.Setter getSetter() {
        return setter;
    }

    // This override is not deprecated because local variables can only come from sources,
    // and we can be sure that they won't be recompiled independently
    @Override
    public boolean isDelegated() {
        return isDelegated;
    }
}
