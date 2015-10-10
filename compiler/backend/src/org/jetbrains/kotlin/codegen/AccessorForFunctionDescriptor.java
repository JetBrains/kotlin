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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetSuperExpression;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationsPackage;

public class AccessorForFunctionDescriptor extends AbstractAccessorForFunctionDescriptor implements AccessorForCallableDescriptor<FunctionDescriptor> {
    private final FunctionDescriptor calleeDescriptor;
    private final JetSuperExpression superCallExpression;

    public AccessorForFunctionDescriptor(
            @NotNull FunctionDescriptor descriptor,
            @NotNull DeclarationDescriptor containingDeclaration,
            int index,
            @Nullable JetSuperExpression superCallExpression
    ) {
        super(containingDeclaration,
              Name.identifier("access$" + (descriptor instanceof ConstructorDescriptor ? "init" : descriptor.getName()) + "$" + index));
        this.calleeDescriptor = descriptor;
        this.superCallExpression = superCallExpression;

        initialize(DescriptorUtils.getReceiverParameterType(descriptor.getExtensionReceiverParameter()),
                   descriptor instanceof ConstructorDescriptor || AnnotationsPackage.isPlatformStaticInObjectOrClass(descriptor)
                        ? null
                        : descriptor.getDispatchReceiverParameter(),
                   copyTypeParameters(descriptor),
                   copyValueParameters(descriptor),
                   descriptor.getReturnType(),
                   Modality.FINAL,
                   Visibilities.LOCAL,
                   descriptor.isOperator(),
                   descriptor.isInfix());
    }

    @NotNull
    @Override
    public FunctionDescriptor getCalleeDescriptor() {
        return calleeDescriptor;
    }

    @Override
    public JetSuperExpression getSuperCallExpression() {
        return superCallExpression;
    }
}
