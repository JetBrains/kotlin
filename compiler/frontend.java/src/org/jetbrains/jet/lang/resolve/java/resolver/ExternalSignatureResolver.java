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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeFieldSignatureData;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesPropagationData;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class ExternalSignatureResolver {
    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    private abstract static class MemberSignature {
        private final List<String> signatureErrors;

        protected MemberSignature(@NotNull List<String> signatureErrors) {
            this.signatureErrors = signatureErrors;
        }

        @NotNull
        public List<String> getErrors() {
            return signatureErrors;
        }
    }

    public static class AlternativeMethodSignature extends MemberSignature {
        private final JetType returnType;
        private final JetType receiverType;
        private final List<ValueParameterDescriptor> valueParameters;
        private final List<TypeParameterDescriptor> typeParameters;

        public AlternativeMethodSignature(
                @Nullable JetType returnType,
                @Nullable JetType receiverType,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull List<String> signatureErrors
        ) {
            super(signatureErrors);
            this.returnType = returnType;
            this.receiverType = receiverType;
            this.valueParameters = valueParameters;
            this.typeParameters = typeParameters;
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
    }

    public static class AlternativeFieldSignature extends MemberSignature {
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

    public static class PropagatedMethodSignature extends AlternativeMethodSignature {
        private final List<FunctionDescriptor> superMethods;

        public PropagatedMethodSignature(
                @Nullable JetType returnType,
                @Nullable JetType receiverType,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull List<String> signatureErrors,
                @NotNull List<FunctionDescriptor> superMethods
        ) {
            super(returnType, receiverType, valueParameters, typeParameters, signatureErrors);
            this.superMethods = superMethods;
        }

        @NotNull
        public List<FunctionDescriptor> getSuperMethods() {
            return superMethods;
        }
    }

    @NotNull
    public PropagatedMethodSignature resolvePropagatedSignature(
            @NotNull JavaMethod method,
            @NotNull ClassDescriptor owner,
            @NotNull JetType returnType,
            @Nullable JetType receiverType,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<TypeParameterDescriptor> typeParameters
    ) {
        SignaturesPropagationData data =
                new SignaturesPropagationData(owner, returnType, receiverType, valueParameters, typeParameters, method, trace);
        return new PropagatedMethodSignature(data.getModifiedReturnType(), data.getModifiedReceiverType(),
                                             data.getModifiedValueParameters(), data.getModifiedTypeParameters(), data.getSignatureErrors(),
                                             data.getSuperFunctions());
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NotNull
    public AlternativeMethodSignature resolveAlternativeMethodSignature(
            @NotNull JavaMethod method,
            boolean hasSuperMethods,
            @Nullable JetType returnType,
            @Nullable JetType receiverType,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<TypeParameterDescriptor> typeParameters
    ) {
        AlternativeMethodSignatureData data =
                new AlternativeMethodSignatureData(method, receiverType, valueParameters, returnType, typeParameters, hasSuperMethods);

        if (data.isAnnotated() && !data.hasErrors()) {
            return new AlternativeMethodSignature(data.getReturnType(), receiverType, data.getValueParameters(), data.getTypeParameters(),
                                                  Collections.<String>emptyList());
        }

        List<String> error = data.hasErrors() ? Collections.singletonList(data.getError()) : Collections.<String>emptyList();
        return new AlternativeMethodSignature(returnType, receiverType, valueParameters, typeParameters, error);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NotNull
    public AlternativeFieldSignature resolveAlternativeFieldSignature(
            @NotNull JavaField field,
            @NotNull JetType returnType,
            boolean isVar
    ) {
        AlternativeFieldSignatureData data = new AlternativeFieldSignatureData(field, returnType, isVar);

        if (data.isAnnotated() && !data.hasErrors()) {
            return new AlternativeFieldSignature(data.getReturnType(), null);
        }

        String error = data.hasErrors() ? data.getError() : null;
        return new AlternativeFieldSignature(returnType, error);
    }

    public void reportSignatureErrors(@NotNull CallableMemberDescriptor descriptor, @NotNull List<String> signatureErrors) {
        trace.record(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor, signatureErrors);
    }
}
