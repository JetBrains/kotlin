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

import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;

public class AccessorForPropertyDescriptor extends PropertyDescriptorImpl {
    public AccessorForPropertyDescriptor(PropertyDescriptor pd, DeclarationDescriptor containingDeclaration, int index) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Visibilities.PUBLIC,
              pd.isVar(), Name.identifier(pd.getName() + "$b$" + index),
              Kind.DECLARATION);

        JetType receiverType = DescriptorUtils.getReceiverParameterType(pd.getReceiverParameter());
        setType(pd.getType(), Collections.<TypeParameterDescriptorImpl>emptyList(), pd.getExpectedThisObject(), receiverType);
        initialize(new Getter(this), new Setter(this));
    }

    public static class Getter extends PropertyGetterDescriptorImpl {
        public Getter(AccessorForPropertyDescriptor property) {
            super(property, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Visibilities.PUBLIC,
                  false,
                  false, Kind.DECLARATION);
            initialize(property.getType());
        }
    }

    public static class Setter extends PropertySetterDescriptorImpl {
        public Setter(AccessorForPropertyDescriptor property) {
            super(property, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Visibilities.PUBLIC,
                  false,
                  false, Kind.DECLARATION);
            initializeDefault();
        }
    }
}
