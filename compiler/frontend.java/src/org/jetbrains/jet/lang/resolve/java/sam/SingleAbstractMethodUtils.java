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

package org.jetbrains.jet.lang.resolve.java.sam;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SingleAbstractMethodUtils {
    @NotNull
    private static List<CallableMemberDescriptor> getAbstractMembers(@NotNull JetType type) {
        List<CallableMemberDescriptor> abstractMembers = Lists.newArrayList();
        for (DeclarationDescriptor member : type.getMemberScope().getAllDescriptors()) {
            if (member instanceof CallableMemberDescriptor && ((CallableMemberDescriptor) member).getModality() == Modality.ABSTRACT) {
                abstractMembers.add((CallableMemberDescriptor) member);
            }
        }
        return abstractMembers;
    }

    @NotNull
    private static JetType getFunctionTypeForFunction(@NotNull FunctionDescriptor function) {
        JetType returnType = function.getReturnType();
        assert returnType != null : "function is not initialized: " + function;
        List<JetType> parameterTypes = Lists.newArrayList();
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            parameterTypes.add(parameter.getType());
        }
        return KotlinBuiltIns.getInstance().getFunctionType(Collections.<AnnotationDescriptor>emptyList(), null, parameterTypes, returnType);
    }

    private static boolean isSamInterface(@NotNull ClassDescriptor klass) {
        if (klass.getKind() != ClassKind.TRAIT) {
            return false;
        }

        List<CallableMemberDescriptor> abstractMembers = getAbstractMembers(klass.getDefaultType());
        if (abstractMembers.size() == 1) {
            CallableMemberDescriptor member = abstractMembers.get(0);
            if (member instanceof SimpleFunctionDescriptor) {
                return member.getTypeParameters().isEmpty();
            }
        }
        return false;
    }

    @NotNull
    public static SimpleFunctionDescriptor createSamConstructorFunction(
            @NotNull ClassOrNamespaceDescriptor owner,
            @NotNull ClassDescriptor samInterface
    ) {
        assert isSamInterface(samInterface) : samInterface;

        SimpleFunctionDescriptorImpl result = new SimpleFunctionDescriptorImpl(
                owner,
                samInterface.getAnnotations(),
                samInterface.getName(),
                CallableMemberDescriptor.Kind.SYNTHESIZED
        );

        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> traitToFunTypeParameters =
                SignaturesUtil.recreateTypeParametersAndReturnMapping(samInterface.getTypeConstructor().getParameters(), result);
        TypeSubstitutor typeParametersSubstitutor = SignaturesUtil.createSubstitutorForTypeParameters(traitToFunTypeParameters);

        JetType parameterTypeUnsubstituted = getFunctionTypeForFunction(getAbstractMethodOfSamType(samInterface.getDefaultType()));
        JetType parameterType = typeParametersSubstitutor.substitute(parameterTypeUnsubstituted, Variance.IN_VARIANCE);
        assert parameterType != null : "couldn't substitute type: " + parameterType + ", substitutor = " + typeParametersSubstitutor;
        ValueParameterDescriptor parameter = new ValueParameterDescriptorImpl(
                result, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("function"), parameterType, false, null);

        JetType returnType = typeParametersSubstitutor.substitute(samInterface.getDefaultType(), Variance.OUT_VARIANCE);
        assert returnType != null : "couldn't substitute type: " + returnType + ", substitutor = " + typeParametersSubstitutor;

        for (Map.Entry<TypeParameterDescriptor, TypeParameterDescriptorImpl> mapEntry : traitToFunTypeParameters.entrySet()) {
            TypeParameterDescriptor traitTypeParameter = mapEntry.getKey();
            TypeParameterDescriptorImpl funTypeParameter = mapEntry.getValue();

            for (JetType upperBound : traitTypeParameter.getUpperBounds()) {
                JetType upperBoundSubstituted = typeParametersSubstitutor.substitute(upperBound, Variance.INVARIANT);
                assert upperBoundSubstituted != null : "couldn't substitute type: " + upperBound + ", substitutor = " + typeParametersSubstitutor;
                funTypeParameter.addUpperBound(upperBoundSubstituted);
            }

            funTypeParameter.setInitialized();
        }

        result.initialize(
                null,
                null,
                Lists.newArrayList(traitToFunTypeParameters.values()),
                Arrays.asList(parameter),
                returnType,
                Modality.FINAL,
                samInterface.getVisibility(),
                false
        );

        return result;
    }

    @NotNull
    public static SimpleFunctionDescriptor getAbstractMethodOfSamType(@NotNull JetType type) {
        return (SimpleFunctionDescriptor) getAbstractMembers(type).get(0);
    }

    @NotNull
    public static SimpleFunctionDescriptor getAbstractMethodOfSamInterface(@NotNull ClassDescriptor samInterface) {
        return getAbstractMethodOfSamType(samInterface.getDefaultType());
    }

    private SingleAbstractMethodUtils() {
    }
}
