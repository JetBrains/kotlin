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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.Variance.INVARIANT;

public class SingleAbstractMethodUtils {

    @NotNull
    private static List<CallableMemberDescriptor> getAbstractMembers(@NotNull JetType type) {
        List<CallableMemberDescriptor> abstractMembers = Lists.newArrayList();
        for (DeclarationDescriptor member : type.getMemberScope().getAllDescriptors()) {
            if (member instanceof CallableMemberDescriptor &&
                ((CallableMemberDescriptor) member).getModality() == Modality.ABSTRACT &&
                ((CallableMemberDescriptor) member).getKind() != CallableMemberDescriptor.Kind.SYNTHESIZED) {
                abstractMembers.add((CallableMemberDescriptor) member);
            }
        }
        return abstractMembers;
    }

    // TODO maybe do this in substitutor?
    private static JetType fixProjections(@NotNull JetType functionType) {
        //removes redundant projection kinds and detects conflicts

        List<TypeProjection> arguments = Lists.newArrayList();
        for (TypeParameterDescriptor typeParameter : functionType.getConstructor().getParameters()) {
            Variance variance = typeParameter.getVariance();
            TypeProjection argument = functionType.getArguments().get(typeParameter.getIndex());
            Variance kind = argument.getProjectionKind();
            if (kind != INVARIANT && variance != INVARIANT) {
                if (kind == variance) {
                    arguments.add(new TypeProjection(argument.getType()));
                }
                else {
                    return null;
                }
            }
            else {
                 arguments.add(argument);
            }
        }
        ClassifierDescriptor classifier = functionType.getConstructor().getDeclarationDescriptor();
        assert classifier instanceof ClassDescriptor : "Not class: " + classifier;
        return new JetTypeImpl(
                functionType.getAnnotations(),
                functionType.getConstructor(),
                functionType.isNullable(),
                arguments,
                ((ClassDescriptor) classifier).getMemberScope(arguments)
        );
    }

    @Nullable
    private static JetType getFunctionTypeForSamType(@NotNull JetType samType) {
        // e.g. samType == Comparator<String>?

        ClassifierDescriptor classifier = samType.getConstructor().getDeclarationDescriptor();
        if (classifier instanceof ClassDescriptorFromJvmBytecode) {
            // Function2<T, T, Int>
            JetType functionTypeDefault = ((ClassDescriptorFromJvmBytecode) classifier).getFunctionTypeForSamInterface();

            if (functionTypeDefault != null) {
                // Function2<String, String, Int>?
                JetType substitute = TypeSubstitutor.create(samType).substitute(functionTypeDefault, Variance.INVARIANT);

                return substitute == null ? null : fixProjections(TypeUtils.makeNullableAsSpecified(substitute, samType.isNullable()));
            }
        }
        return null;
    }

    @NotNull
    public static JetType getFunctionTypeForAbstractMethod(@NotNull FunctionDescriptor function) {
        JetType returnType = function.getReturnType();
        assert returnType != null : "function is not initialized: " + function;
        List<JetType> parameterTypes = Lists.newArrayList();
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            parameterTypes.add(parameter.getType());
        }
        return KotlinBuiltIns.getInstance().getFunctionType(
                Collections.<AnnotationDescriptor>emptyList(), null, parameterTypes, returnType);
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

        TypeParameters typeParameters = recreateAndInitializeTypeParameters(samInterface.getTypeConstructor().getParameters(), result);

        JetType parameterTypeUnsubstituted = getFunctionTypeForSamType(samInterface.getDefaultType());
        assert parameterTypeUnsubstituted != null : "couldn't get function type for SAM type " + samInterface.getDefaultType();
        JetType parameterType = typeParameters.substitutor.substitute(parameterTypeUnsubstituted, Variance.IN_VARIANCE);
        assert parameterType != null : "couldn't substitute type: " + parameterType + ", substitutor = " + typeParameters.substitutor;
        ValueParameterDescriptor parameter = new ValueParameterDescriptorImpl(
                result, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("function"), parameterType, false, null);

        JetType returnType = typeParameters.substitutor.substitute(samInterface.getDefaultType(), Variance.OUT_VARIANCE);
        assert returnType != null : "couldn't substitute type: " + returnType + ", substitutor = " + typeParameters.substitutor;

        result.initialize(
                null,
                null,
                typeParameters.descriptors,
                Arrays.asList(parameter),
                returnType,
                Modality.FINAL,
                samInterface.getVisibility(),
                false
        );

        return result;
    }

    public static boolean isSamType(@NotNull JetType type) {
        return getFunctionTypeForSamType(type) != null;
    }

    public static boolean isSamAdapterNecessary(@NotNull SimpleFunctionDescriptor fun) {
        for (ValueParameterDescriptor param : fun.getValueParameters()) {
            if (isSamType(param.getType())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static SimpleFunctionDescriptor createSamAdapterFunction(@NotNull SimpleFunctionDescriptor original) {
        SimpleFunctionDescriptorImpl result = new SimpleFunctionDescriptorImpl(
                original.getContainingDeclaration(),
                original.getAnnotations(),
                original.getName(),
                CallableMemberDescriptor.Kind.SYNTHESIZED
        );

        TypeParameters typeParameters = recreateAndInitializeTypeParameters(original.getTypeParameters(), result);

        JetType returnTypeUnsubstituted = original.getReturnType();
        assert returnTypeUnsubstituted != null : original;
        JetType returnType = typeParameters.substitutor.substitute(returnTypeUnsubstituted, Variance.OUT_VARIANCE);
        assert returnType != null : "couldn't substitute type: " + returnType + ", substitutor = " + typeParameters.substitutor;

        List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();
        for (ValueParameterDescriptor originalParam : original.getValueParameters()) {
            JetType originalType = originalParam.getType();
            JetType functionType = getFunctionTypeForSamType(originalType);
            JetType newTypeUnsubstituted = functionType != null ? functionType : originalType;
            JetType newType = typeParameters.substitutor.substitute(newTypeUnsubstituted, Variance.IN_VARIANCE);
            assert newType != null : "couldn't substitute type: " + newTypeUnsubstituted + ", substitutor = " + typeParameters.substitutor;

            ValueParameterDescriptor newParam = new ValueParameterDescriptorImpl(
                    result, originalParam.getIndex(), originalParam.getAnnotations(), originalParam.getName(), newType, false, null);
            valueParameters.add(newParam);
        }

        result.initialize(
                null,
                original.getExpectedThisObject(),
                typeParameters.descriptors,
                valueParameters,
                returnType,
                original.getModality(),
                original.getVisibility(),
                false
        );

        return result;
    }

    @NotNull
    private static TypeParameters recreateAndInitializeTypeParameters(
            @NotNull List<TypeParameterDescriptor> originalParameters,
            @Nullable DeclarationDescriptor newOwner
    ) {
        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> traitToFunTypeParameters =
                SignaturesUtil.recreateTypeParametersAndReturnMapping(originalParameters, newOwner);
        TypeSubstitutor typeParametersSubstitutor = SignaturesUtil.createSubstitutorForTypeParameters(traitToFunTypeParameters);
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

        List<TypeParameterDescriptor> typeParameters = Lists.<TypeParameterDescriptor>newArrayList(traitToFunTypeParameters.values());
        return new TypeParameters(typeParameters, typeParametersSubstitutor);
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

    private static class TypeParameters {
        public final List<TypeParameterDescriptor> descriptors;
        public final TypeSubstitutor substitutor;

        private TypeParameters(List<TypeParameterDescriptor> descriptors, TypeSubstitutor substitutor) {
            this.descriptors = descriptors;
            this.substitutor = substitutor;
        }
    }
}
