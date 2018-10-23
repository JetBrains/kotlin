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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class PropertySetterDescriptorImpl extends PropertyAccessorDescriptorImpl implements PropertySetterDescriptor {
    private ValueParameterDescriptor parameter;

    @NotNull
    private final PropertySetterDescriptor original;

    public PropertySetterDescriptorImpl(
            @NotNull PropertyDescriptor correspondingProperty,
            @NotNull Annotations annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isDefault,
            boolean isExternal,
            boolean isInline,
            @NotNull Kind kind,
            @Nullable PropertySetterDescriptor original,
            @NotNull SourceElement source
    ) {
        super(modality, visibility, correspondingProperty, annotations, Name.special("<set-" + correspondingProperty.getName() + ">"),
              isDefault, isExternal, isInline, kind, source);
        this.original = original != null ? original : this;
    }

    public void initialize(@NotNull ValueParameterDescriptor parameter) {
        assert this.parameter == null;
        this.parameter = parameter;
    }

    public void initializeDefault() {
        initialize(createSetterParameter(this, getCorrespondingProperty().getType(), Annotations.Companion.getEMPTY()));
    }

    public static ValueParameterDescriptorImpl createSetterParameter(
            @NotNull PropertySetterDescriptor setterDescriptor,
            @NotNull KotlinType type,
            @NotNull Annotations annotations
    ) {
        return new ValueParameterDescriptorImpl(
                setterDescriptor, null, 0, annotations, Name.special("<set-?>"), type,
                /* declaresDefaultValue = */ false,
                /* isCrossinline = */ false,
                /* isNoinline = */ false,
                null, SourceElement.NO_SOURCE
        );
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Collection<? extends PropertySetterDescriptor> getOverriddenDescriptors() {
        return (Collection) super.getOverriddenDescriptors(false);
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        if (parameter == null) {
            throw new IllegalStateException();
        }
        return Collections.singletonList(parameter);
    }

    @NotNull
    @Override
    public KotlinType getReturnType() {
        return getBuiltIns(this).getUnitType();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertySetterDescriptor(this, data);
    }

    @NotNull
    @Override
    public PropertySetterDescriptor getOriginal() {
        return this.original;
    }

}
