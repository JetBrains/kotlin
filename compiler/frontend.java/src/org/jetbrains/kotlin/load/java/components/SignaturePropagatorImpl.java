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

package org.jetbrains.kotlin.load.java.components;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.load.java.structure.JavaMethod;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.jvm.JvmBindingContextSlices;
import org.jetbrains.kotlin.resolve.jvm.kotlinSignature.SignaturesPropagationData;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.List;

public class SignaturePropagatorImpl implements SignaturePropagator {
    private final BindingTrace trace;

    public SignaturePropagatorImpl(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Override
    @NotNull
    public PropagatedSignature resolvePropagatedSignature(
            @NotNull JavaMethod method,
            @NotNull ClassDescriptor owner,
            @NotNull KotlinType returnType,
            @Nullable KotlinType receiverType,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<TypeParameterDescriptor> typeParameters
    ) {
        SignaturesPropagationData data =
                new SignaturesPropagationData(owner, returnType, receiverType, valueParameters, typeParameters, method);
        return new PropagatedSignature(
                returnType, data.getModifiedReceiverType(), data.getModifiedValueParameters(),
                typeParameters, data.getSignatureErrors(), data.getModifiedHasStableParameterNames()
        );
    }

    @Override
    public void reportSignatureErrors(@NotNull CallableMemberDescriptor descriptor, @NotNull List<String> signatureErrors) {
        trace.record(JvmBindingContextSlices.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor.getOriginal(), signatureErrors);
    }
}
