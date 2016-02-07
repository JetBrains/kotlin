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

package org.jetbrains.kotlin.descriptors;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface PropertyAccessorDescriptor extends VariableAccessorDescriptor {
    boolean isDefault();

    @NotNull
    @Override
    PropertyAccessorDescriptor getOriginal();

    @Override
    @NotNull
    Collection<? extends PropertyAccessorDescriptor> getOverriddenDescriptors();

    @NotNull
    PropertyDescriptor getCorrespondingProperty();

    @NotNull
    @Override
    PropertyAccessorDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            Visibility visibility,
            Kind kind,
            boolean copyOverrides
    );
}
