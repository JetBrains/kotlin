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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;

public class AccessorForFunctionDescriptor extends SimpleFunctionDescriptorImpl {
    public AccessorForFunctionDescriptor(
            @NotNull FunctionDescriptor descriptor,
            @NotNull DeclarationDescriptor containingDeclaration,
            int index
    ) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(),
              Name.identifier((descriptor instanceof ConstructorDescriptor ? "$init" : descriptor.getName()) + "$b$" + index),
              Kind.DECLARATION);

        initialize(DescriptorUtils.getReceiverParameterType(descriptor.getReceiverParameter()),
                   descriptor instanceof ConstructorDescriptor ? NO_RECEIVER_PARAMETER : descriptor.getExpectedThisObject(),
                   Collections.<TypeParameterDescriptor>emptyList(),
                   copyValueParameters(descriptor),
                   descriptor.getReturnType(),
                   Modality.FINAL,
                   Visibilities.INTERNAL,
                   /*isInline = */ false);
    }

    @NotNull
    private List<ValueParameterDescriptor> copyValueParameters(@NotNull FunctionDescriptor descriptor) {
        List<ValueParameterDescriptor> valueParameters = descriptor.getValueParameters();
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(valueParameters.size());
        for (ValueParameterDescriptor valueParameter : valueParameters) {
            result.add(valueParameter.copy(this, valueParameter.getName()));
        }
        return result;
    }
}
