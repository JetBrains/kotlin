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
import org.jetbrains.kotlin.resolve.DescriptorUtils;

public class AccessorForFunctionDescriptor extends AbstractAccessorForFunctionDescriptor implements AccessorForCallableDescriptor<FunctionDescriptor> {
    private final FunctionDescriptor calleeDescriptor;
    private final ClassDescriptor superCallTarget;

    public AccessorForFunctionDescriptor(
            @NotNull FunctionDescriptor descriptor,
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable ClassDescriptor superCallTarget,
            @NotNull String nameSuffix
    ) {
        super(containingDeclaration,
              Name.identifier("access$" + nameSuffix));
        this.calleeDescriptor = descriptor;
        this.superCallTarget = superCallTarget;

        initialize(DescriptorUtils.getReceiverParameterType(descriptor.getExtensionReceiverParameter()),
                   descriptor instanceof ConstructorDescriptor || CodegenUtilKt.isJvmStaticInObjectOrClass(descriptor)
                        ? null
                        : descriptor.getDispatchReceiverParameter(),
                   copyTypeParameters(descriptor),
                   copyValueParameters(descriptor),
                   descriptor.getReturnType(),
                   Modality.FINAL,
                   Visibilities.LOCAL);
    }

    @NotNull
    @Override
    public FunctionDescriptor getCalleeDescriptor() {
        return calleeDescriptor;
    }

    @Override
    public ClassDescriptor getSuperCallTarget() {
        return superCallTarget;
    }
}
