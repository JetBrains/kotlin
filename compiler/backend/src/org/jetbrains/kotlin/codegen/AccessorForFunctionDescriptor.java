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
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.LinkedHashMap;

public class AccessorForFunctionDescriptor extends AbstractAccessorForFunctionDescriptor implements AccessorForCallableDescriptor<FunctionDescriptor> {
    private final FunctionDescriptor calleeDescriptor;
    private final ClassDescriptor superCallTarget;
    private final String nameSuffix;

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
        this.nameSuffix = nameSuffix;

        initialize(DescriptorUtils.getReceiverParameterType(descriptor.getExtensionReceiverParameter()),
                   descriptor instanceof ConstructorDescriptor || CodegenUtilKt.isJvmStaticInObjectOrClass(descriptor)
                        ? null
                        : descriptor.getDispatchReceiverParameter(),
                   copyTypeParameters(descriptor),
                   copyValueParameters(descriptor),
                   descriptor.getReturnType(),
                   Modality.FINAL,
                   Visibilities.LOCAL);

        setSuspend(descriptor.isSuspend());
        if (descriptor.getUserData(CoroutineCodegenUtilKt.INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) != null) {
            userDataMap = new LinkedHashMap<>();
            userDataMap.put(
                    CoroutineCodegenUtilKt.INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION,
                    descriptor.getUserData(CoroutineCodegenUtilKt.INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION)
            );
        }
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        return new AccessorForFunctionDescriptor(calleeDescriptor, newOwner, superCallTarget, nameSuffix);
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
