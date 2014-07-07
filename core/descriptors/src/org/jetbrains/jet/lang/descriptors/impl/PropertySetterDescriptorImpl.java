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
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PropertySetterDescriptorImpl extends PropertyAccessorDescriptorImpl implements PropertySetterDescriptor {

    private ValueParameterDescriptor parameter;
    @NotNull
    private final PropertySetterDescriptor original;

    public PropertySetterDescriptorImpl(
            @NotNull PropertyDescriptor correspondingProperty,
            @NotNull Annotations annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean hasBody,
            boolean isDefault,
            @NotNull Kind kind,
            @Nullable PropertySetterDescriptor original,
            @NotNull SourceElement source
    ) {
        super(modality, visibility, correspondingProperty, annotations, Name.special("<set-" + correspondingProperty.getName() + ">"),
              hasBody, isDefault, kind, source);
        this.original = original != null ? original : this;
    }

    public void initialize(@NotNull ValueParameterDescriptor parameter) {
        assert this.parameter == null;
        this.parameter = parameter;
    }

    public void initializeDefault() {
        assert parameter == null;
        parameter = createSetterParameter(this, getCorrespondingProperty().getReturnType());
    }

    public static ValueParameterDescriptorImpl createSetterParameter(
            @NotNull PropertySetterDescriptor setterDescriptor,
            @NotNull JetType type
    ) {
        return new ValueParameterDescriptorImpl(
                setterDescriptor, null, 0, Annotations.EMPTY, Name.special("<set-?>"), type, false, null, SourceElement.NO_SOURCE
        );
    }

    @NotNull
    @Override
    public Set<? extends PropertyAccessorDescriptor> getOverriddenDescriptors() {
        return super.getOverriddenDescriptors(false);
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
    public JetType getReturnType() {
        return KotlinBuiltIns.getInstance().getUnitType();
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
