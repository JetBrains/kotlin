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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class PropertyGetterDescriptor extends PropertyAccessorDescriptor {
    private JetType returnType;

    public PropertyGetterDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality, @NotNull Visibility visibility, boolean hasBody, boolean isDefault, Kind kind)
    {
        super(modality, visibility, correspondingProperty, annotations, "get-" + correspondingProperty.getName(), hasBody, isDefault, kind);
    }
    
    public void initialize(JetType returnType) {
        this.returnType = returnType == null ? getCorrespondingProperty().getType() : returnType;
    }

    @NotNull
    @Override
    public Set<? extends PropertyAccessorDescriptor> getOverriddenDescriptors() {
        return super.getOverriddenDescriptors(true);
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return Collections.emptyList();
    }

    @Override
    public JetType getReturnType() {
        return returnType;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyGetterDescriptor(this, data);
    }
}
