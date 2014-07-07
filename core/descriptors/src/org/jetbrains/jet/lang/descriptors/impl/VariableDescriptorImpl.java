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
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.storage.NullableLazyValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class VariableDescriptorImpl extends DeclarationDescriptorNonRootImpl implements VariableDescriptor {
    private JetType outType;
    private NullableLazyValue<CompileTimeConstant<?>> compileTimeInitializer;

    public VariableDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @Nullable JetType outType,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, source);

        this.outType = outType;
    }

    @NotNull
    @Override
    public JetType getType() {
        return outType;
    }

    public void setOutType(JetType outType) {
        assert this.outType == null;
        this.outType = outType;
    }

    @Nullable
    @Override
    public CompileTimeConstant<?> getCompileTimeInitializer() {
        if (compileTimeInitializer != null) {
            return compileTimeInitializer.invoke();
        }
        return null;
    }

    public void setCompileTimeInitializer(@NotNull NullableLazyValue<CompileTimeConstant<?>> compileTimeInitializer) {
        assert !isVar() : "Compile-time value for property initializer should be recorded only for final variables " + getName();
        this.compileTimeInitializer = compileTimeInitializer;
    }

    @Override
    @NotNull
    public VariableDescriptor getOriginal() {
        return (VariableDescriptor) super.getOriginal();
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
    public List<TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public ReceiverParameterDescriptor getReceiverParameter() {
        return ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
    }

    @Override
    public ReceiverParameterDescriptor getExpectedThisObject() {
        return ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
    }

    @NotNull
    @Override
    public JetType getReturnType() {
        return getType();
    }
}
