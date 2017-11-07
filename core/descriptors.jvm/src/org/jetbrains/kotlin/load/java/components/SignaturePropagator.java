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
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collections;
import java.util.List;

public interface SignaturePropagator {
    SignaturePropagator DO_NOTHING = new SignaturePropagator() {
        @NotNull
        @Override
        public PropagatedSignature resolvePropagatedSignature(
                @NotNull JavaMethod method,
                @NotNull ClassDescriptor owner,
                @NotNull KotlinType returnType,
                @Nullable KotlinType receiverType,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @NotNull List<TypeParameterDescriptor> typeParameters
        ) {
            return new PropagatedSignature(
                    returnType, receiverType, valueParameters, typeParameters, Collections.<String>emptyList(), false
            );
        }

        @Override
        public void reportSignatureErrors(@NotNull CallableMemberDescriptor descriptor, @NotNull List<String> signatureErrors) {
            throw new UnsupportedOperationException("Should not be called");
        }
    };

    class PropagatedSignature {
        private final KotlinType returnType;
        private final KotlinType receiverType;
        private final List<ValueParameterDescriptor> valueParameters;
        private final List<TypeParameterDescriptor> typeParameters;
        private final List<String> signatureErrors;
        private final boolean hasStableParameterNames;

        public PropagatedSignature(
                @NotNull KotlinType returnType,
                @Nullable KotlinType receiverType,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull List<String> signatureErrors,
                boolean hasStableParameterNames
        ) {
            this.returnType = returnType;
            this.receiverType = receiverType;
            this.valueParameters = valueParameters;
            this.typeParameters = typeParameters;
            this.signatureErrors = signatureErrors;
            this.hasStableParameterNames = hasStableParameterNames;
        }

        @NotNull
        public KotlinType getReturnType() {
            return returnType;
        }

        @Nullable
        public KotlinType getReceiverType() {
            return receiverType;
        }

        @NotNull
        public List<ValueParameterDescriptor> getValueParameters() {
            return valueParameters;
        }

        @NotNull
        public List<TypeParameterDescriptor> getTypeParameters() {
            return typeParameters;
        }

        public boolean hasStableParameterNames() {
            return hasStableParameterNames;
        }

        @NotNull
        public List<String> getErrors() {
            return signatureErrors;
        }
    }

    @NotNull
    PropagatedSignature resolvePropagatedSignature(
            @NotNull JavaMethod method,
            @NotNull ClassDescriptor owner,
            @NotNull KotlinType returnType,
            @Nullable KotlinType receiverType,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<TypeParameterDescriptor> typeParameters
    );

    void reportSignatureErrors(@NotNull CallableMemberDescriptor descriptor, @NotNull List<String> signatureErrors);
}
