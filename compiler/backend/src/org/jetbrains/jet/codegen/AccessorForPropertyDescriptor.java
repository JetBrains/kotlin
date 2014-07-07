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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyGetterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertySetterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;

public class AccessorForPropertyDescriptor extends PropertyDescriptorImpl {
    public AccessorForPropertyDescriptor(@NotNull PropertyDescriptor pd, @NotNull DeclarationDescriptor containingDeclaration, int index) {
        this(pd, pd.getType(), DescriptorUtils.getReceiverParameterType(pd.getReceiverParameter()), pd.getExpectedThisObject(), containingDeclaration, index);
    }

    protected AccessorForPropertyDescriptor(
            @NotNull PropertyDescriptor original,
            @NotNull JetType propertyType,
            @Nullable JetType receiverType,
            @Nullable ReceiverParameterDescriptor expectedThisObject,
            @NotNull DeclarationDescriptor containingDeclaration,
            int index
    ) {
        super(containingDeclaration, null, Annotations.EMPTY, Modality.FINAL, Visibilities.LOCAL,
              original.isVar(), Name.identifier(original.getName() + "$b$" + index),
              Kind.DECLARATION, SourceElement.NO_SOURCE);

        setType(propertyType, Collections.<TypeParameterDescriptorImpl>emptyList(), expectedThisObject, receiverType);
        initialize(new Getter(this), new Setter(this));
    }

    public static class Getter extends PropertyGetterDescriptorImpl {
        public Getter(AccessorForPropertyDescriptor property) {
            super(property, Annotations.EMPTY, Modality.FINAL, Visibilities.LOCAL,
                  false, false, Kind.DECLARATION, null, SourceElement.NO_SOURCE);
            initialize(property.getType());
        }
    }

    public static class Setter extends PropertySetterDescriptorImpl {
        public Setter(AccessorForPropertyDescriptor property) {
            super(property, Annotations.EMPTY, Modality.FINAL, Visibilities.LOCAL,
                  false, false, Kind.DECLARATION, null, SourceElement.NO_SOURCE);
            initializeDefault();
        }
    }
}
