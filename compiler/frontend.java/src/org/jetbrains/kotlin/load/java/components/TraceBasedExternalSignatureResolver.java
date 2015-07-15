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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.load.java.JavaBindingContext;
import org.jetbrains.kotlin.load.java.structure.JavaConstructor;
import org.jetbrains.kotlin.load.java.structure.JavaField;
import org.jetbrains.kotlin.load.java.structure.JavaMember;
import org.jetbrains.kotlin.load.java.structure.JavaMethod;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.jvm.JvmPackage;
import org.jetbrains.kotlin.resolve.jvm.kotlinSignature.AlternativeFieldSignatureData;
import org.jetbrains.kotlin.resolve.jvm.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.kotlin.resolve.jvm.kotlinSignature.SignaturesPropagationData;
import org.jetbrains.kotlin.types.JetType;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class TraceBasedExternalSignatureResolver implements ExternalSignatureResolver {
    private BindingTrace trace;
    private ExternalAnnotationResolver externalAnnotationResolver;
    private Project project;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setExternalAnnotationResolver(ExternalAnnotationResolver externalAnnotationResolver) {
        this.externalAnnotationResolver = externalAnnotationResolver;
    }

    @Inject
    public void setProject(Project project) {
        this.project = project;
    }

    @Override
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
                new SignaturesPropagationData(owner, returnType, receiverType, valueParameters, typeParameters, method);
        return new PropagatedMethodSignature(data.getModifiedReturnType(), data.getModifiedReceiverType(),
                                             data.getModifiedValueParameters(), data.getModifiedTypeParameters(), data.getSignatureErrors(),
                                             data.getModifiedHasStableParameterNames(), data.getSuperFunctions());
    }

    @Override
    @NotNull
    public AlternativeMethodSignature resolveAlternativeMethodSignature(
            @NotNull JavaMember methodOrConstructor,
            boolean hasSuperMethods,
            @Nullable JetType returnType,
            @Nullable JetType receiverType,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            boolean hasStableParameterNames
    ) {
        assert methodOrConstructor instanceof JavaMethod || methodOrConstructor instanceof JavaConstructor :
                "Not a method or a constructor: " + methodOrConstructor.getName();

        AlternativeMethodSignatureData data =
                new AlternativeMethodSignatureData(externalAnnotationResolver, methodOrConstructor, receiverType, project, valueParameters,
                                                   returnType, typeParameters, hasSuperMethods);

        if (data.isAnnotated() && !data.hasErrors()) {
            if (JvmPackage.getPLATFORM_TYPES()) {
                // We only take parameter names from the @KotlinSignature
                return new AlternativeMethodSignature(returnType, receiverType,
                                                      AlternativeMethodSignatureData.updateNames(valueParameters, data.getValueParameters()),
                                                      typeParameters, Collections.<String>emptyList(), true);
            }
            return new AlternativeMethodSignature(data.getReturnType(), receiverType, data.getValueParameters(), data.getTypeParameters(),
                                                  Collections.<String>emptyList(), true);
        }

        List<String> error = data.hasErrors() ? Collections.singletonList(data.getError()) : Collections.<String>emptyList();
        return new AlternativeMethodSignature(returnType, receiverType, valueParameters, typeParameters, error, hasStableParameterNames);
    }

    @Override
    @NotNull
    public AlternativeFieldSignature resolveAlternativeFieldSignature(
            @NotNull JavaField field,
            @NotNull JetType returnType,
            boolean isVar
    ) {
        AlternativeFieldSignatureData data =
                new AlternativeFieldSignatureData(externalAnnotationResolver, field, returnType, project, isVar);

        if (data.isAnnotated() && !data.hasErrors()) {
            if (JvmPackage.getPLATFORM_TYPES()) {
                return new AlternativeFieldSignature(returnType, null);
            }
            return new AlternativeFieldSignature(data.getReturnType(), null);
        }

        String error = data.hasErrors() ? data.getError() : null;
        return new AlternativeFieldSignature(returnType, error);
    }

    @Override
    public void reportSignatureErrors(@NotNull CallableMemberDescriptor descriptor, @NotNull List<String> signatureErrors) {
        trace.record(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor.getOriginal(), signatureErrors);
    }
}