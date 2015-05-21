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
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;

public class JvmRuntimeTypes {
    private final ClassDescriptor lambda;
    private final ClassDescriptor functionReference;

    public JvmRuntimeTypes() {
        ModuleDescriptorImpl module = new ModuleDescriptorImpl(
                Name.special("<jvm functions impl>"),
                LockBasedStorageManager.NO_LOCKS,
                TopDownAnalyzerFacadeForJVM.JVM_MODULE_PARAMETERS
        );
        PackageFragmentDescriptor kotlinJvmInternal = new MutablePackageFragmentDescriptor(module, new FqName("kotlin.jvm.internal"));

        this.lambda = createClass(kotlinJvmInternal, "Lambda");
        this.functionReference = createClass(kotlinJvmInternal, "FunctionReference");
    }

    @NotNull
    private static ClassDescriptor createClass(@NotNull PackageFragmentDescriptor packageFragment, @NotNull String name) {
        MutableClassDescriptor descriptor = new MutableClassDescriptor(
                packageFragment, packageFragment.getMemberScope(), ClassKind.CLASS, false, Name.identifier(name), SourceElement.NO_SOURCE
        );

        descriptor.setModality(Modality.FINAL);
        descriptor.setVisibility(Visibilities.PUBLIC);
        descriptor.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        descriptor.createTypeConstructor();

        return descriptor;
    }

    @NotNull
    public Collection<JetType> getSupertypesForClosure(@NotNull FunctionDescriptor descriptor) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();

        //noinspection ConstantConditions
        JetType functionType = getBuiltIns(descriptor).getFunctionType(
                Annotations.EMPTY,
                receiverParameter == null ? null : receiverParameter.getType(),
                ExpressionTypingUtils.getValueParametersTypes(descriptor.getValueParameters()),
                descriptor.getReturnType()
        );

        return Arrays.asList(lambda.getDefaultType(), functionType);
    }

    @NotNull
    public Collection<JetType> getSupertypesForFunctionReference(@NotNull FunctionDescriptor descriptor) {
        ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();
        ReceiverParameterDescriptor dispatchReceiver = descriptor.getDispatchReceiverParameter();

        JetType receiverType =
                extensionReceiver != null ? extensionReceiver.getType() : dispatchReceiver != null ? dispatchReceiver.getType() : null;

        //noinspection ConstantConditions
        JetType functionType = getBuiltIns(descriptor).getFunctionType(
                Annotations.EMPTY,
                receiverType,
                ExpressionTypingUtils.getValueParametersTypes(descriptor.getValueParameters()),
                descriptor.getReturnType()
        );

        return Arrays.asList(functionReference.getDefaultType(), functionType);
    }
}
