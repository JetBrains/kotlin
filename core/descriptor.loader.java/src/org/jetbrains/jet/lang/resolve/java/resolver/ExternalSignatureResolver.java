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

package org.jetbrains.jet.lang.resolve.java.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMember;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collections;
import java.util.List;

public interface ExternalSignatureResolver {
    abstract class MemberSignature {
        private final List<String> signatureErrors;

        protected MemberSignature(@NotNull List<String> signatureErrors) {
            this.signatureErrors = signatureErrors;
        }

        @NotNull
        public List<String> getErrors() {
            return signatureErrors;
        }
    }

    class AlternativeMethodSignature extends MemberSignature {
        private final JetType returnType;
        private final JetType receiverType;
        private final List<ValueParameterDescriptor> valueParameters;
        private final List<TypeParameterDescriptor> typeParameters;
        private final boolean hasStableParameterNames;

        public AlternativeMethodSignature(
                @Nullable JetType returnType,
                @Nullable JetType receiverType,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull List<String> signatureErrors,
                boolean hasStableParameterNames
        ) {
            super(signatureErrors);
            this.returnType = returnType;
            this.receiverType = receiverType;
            this.valueParameters = valueParameters;
            this.typeParameters = typeParameters;
            this.hasStableParameterNames = hasStableParameterNames;
        }

        @Nullable
        public JetType getReturnType() {
            return returnType;
        }

        @Nullable
        public JetType getReceiverType() {
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
    }

    class AlternativeFieldSignature extends MemberSignature {
        private final JetType returnType;

        public AlternativeFieldSignature(@NotNull JetType returnType, @Nullable String signatureError) {
            super(signatureError == null ? Collections.<String>emptyList() : Collections.singletonList(signatureError));
            this.returnType = returnType;
        }

        @NotNull
        public JetType getReturnType() {
            return returnType;
        }
    }

    class PropagatedMethodSignature extends AlternativeMethodSignature {
        private final List<FunctionDescriptor> superMethods;

        public PropagatedMethodSignature(
                @Nullable JetType returnType,
                @Nullable JetType receiverType,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull List<String> signatureErrors,
                boolean hasStableParameterNames,
                @NotNull List<FunctionDescriptor> superMethods
        ) {
            super(returnType, receiverType, valueParameters, typeParameters, signatureErrors, hasStableParameterNames);
            this.superMethods = superMethods;
        }

        @NotNull
        public List<FunctionDescriptor> getSuperMethods() {
            return superMethods;
        }
    }

    @NotNull
    PropagatedMethodSignature resolvePropagatedSignature(
            @NotNull JavaMethod method,
            @NotNull ClassDescriptor owner,
            @NotNull JetType returnType,
            @Nullable JetType receiverType,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<TypeParameterDescriptor> typeParameters
    );

    @NotNull
    AlternativeMethodSignature resolveAlternativeMethodSignature(
            @NotNull JavaMember methodOrConstructor,
            boolean hasSuperMethods,
            @Nullable JetType returnType,
            @Nullable JetType receiverType,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            boolean hasStableParameterNames
    );

    @NotNull
    AlternativeFieldSignature resolveAlternativeFieldSignature(
            @NotNull JavaField field,
            @NotNull JetType returnType,
            boolean isVar
    );

    void reportSignatureErrors(@NotNull CallableMemberDescriptor descriptor, @NotNull List<String> signatureErrors);
}
