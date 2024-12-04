/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.sam;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor;
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor;
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor;
import org.jetbrains.kotlin.resolve.sam.*;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.Variance;

import java.util.ArrayList;
import java.util.List;

public class JavaSingleAbstractMethodUtils {
    private JavaSingleAbstractMethodUtils() {
    }

    public static boolean isSamClassDescriptor(@NotNull ClassDescriptor descriptor) {
        if (descriptor.isFun()) return true;
        if (descriptor instanceof LazyJavaClassDescriptor && descriptor.getDefaultFunctionTypeForSamInterface() != null) return true;

        return false;
    }

    public static boolean isSamType(@NotNull KotlinType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).isFun()) return true;

        return SamConversionResolverImplKt.getFunctionTypeForSamType(
                type, JavaBasedSamConversionResolver.INSTANCE, JavaBasedSamConversionOracle.INSTANCE
        ) != null;
    }

    public static boolean isSamAdapterNecessary(@NotNull FunctionDescriptor fun) {
        for (ValueParameterDescriptor param : fun.getValueParameters()) {
            if (isSamType(param.getType())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static SamAdapterDescriptor<JavaMethodDescriptor> createSamAdapterFunction(
            @NotNull JavaMethodDescriptor original,
            @NotNull SamConversionResolver samResolver,
            @NotNull SamConversionOracle samConversionOracle,
            boolean allowNonSpreadArraysForVarargAfterSam
    ) {
        SamAdapterFunctionDescriptor result = new SamAdapterFunctionDescriptor(original);
        return initSamAdapter(original, result, new FunctionInitializer() {
            @Override
            public void initialize(
                    @NotNull List<TypeParameterDescriptor> typeParameters,
                    @NotNull List<ValueParameterDescriptor> valueParameters,
                    @NotNull KotlinType returnType
            ) {
                result.initialize(
                        null, original.getDispatchReceiverParameter(), CollectionsKt.emptyList(),
                        typeParameters,
                        valueParameters,
                        returnType,
                        Modality.FINAL,
                        original.getVisibility()
                );
            }
        }, samResolver, samConversionOracle, allowNonSpreadArraysForVarargAfterSam);
    }

    @NotNull
    public static SamAdapterDescriptor<JavaClassConstructorDescriptor> createSamAdapterConstructor(
            @NotNull JavaClassConstructorDescriptor original,
            @NotNull SamConversionResolver samResolver,
            @NotNull SamConversionOracle samConversionOracle,
            boolean allowNonSpreadArraysForVarargAfterSam
    ) {
        SamAdapterClassConstructorDescriptor result = new SamAdapterClassConstructorDescriptor(original);
        return initSamAdapter(original, result, new FunctionInitializer() {
            @Override
            public void initialize(
                    @NotNull List<TypeParameterDescriptor> typeParameters,
                    @NotNull List<ValueParameterDescriptor> valueParameters,
                    @NotNull KotlinType returnType
            ) {
                result.initialize(valueParameters, original.getVisibility());
                result.setReturnType(returnType);
            }
        }, samResolver, samConversionOracle, allowNonSpreadArraysForVarargAfterSam);
    }

    @NotNull
    private static <F extends FunctionDescriptor> SamAdapterDescriptor<F> initSamAdapter(
            @NotNull F original,
            @NotNull SamAdapterDescriptor<F> adapter,
            @NotNull FunctionInitializer initializer,
            @NotNull SamConversionResolver samResolver,
            @NotNull SamConversionOracle samConversionOracle,
            boolean allowNonSpreadArraysForVarargAfterSam
    ) {
        SamConstructorTypeParameters typeParameters;
        if (adapter instanceof SamAdapterClassConstructorDescriptor) {
            typeParameters = new SamConstructorTypeParameters(original.getTypeParameters(), TypeSubstitutor.EMPTY);
        } else {
            typeParameters = SamConstructorUtilsKt.recreateAndInitializeTypeParameters(original.getTypeParameters(), adapter);
        }

        KotlinType returnTypeUnsubstituted = original.getReturnType();
        assert returnTypeUnsubstituted != null : "Creating SAM adapter for not initialized original: " + original;

        TypeSubstitutor substitutor = typeParameters.getSubstitutor();
        KotlinType returnType = substitutor.substitute(returnTypeUnsubstituted, Variance.INVARIANT);
        assert returnType != null : "couldn't substitute type: " + returnTypeUnsubstituted +
                                        ", substitutor = " + substitutor;


        List<ValueParameterDescriptor> valueParameters =
                createValueParametersForSamAdapter(original, adapter, substitutor, samResolver, samConversionOracle, allowNonSpreadArraysForVarargAfterSam);

        initializer.initialize(typeParameters.getDescriptors(), valueParameters, returnType);

        return adapter;
    }

    public static List<ValueParameterDescriptor> createValueParametersForSamAdapter(
            @NotNull FunctionDescriptor original,
            @NotNull FunctionDescriptor samAdapter,
            @NotNull TypeSubstitutor substitutor,
            @NotNull SamConversionResolver samResolver,
            @NotNull SamConversionOracle samConversionOracle,
            boolean allowNonSpreadArraysForVarargAfterSam
    ) {
        List<ValueParameterDescriptor> originalValueParameters = original.getValueParameters();
        List<ValueParameterDescriptor> valueParameters = new ArrayList<>(originalValueParameters.size());
        for (ValueParameterDescriptor originalParam : originalValueParameters) {
            KotlinType originalType = originalParam.getType();
            KotlinType functionType = SamConversionResolverImplKt.getFunctionTypeForSamType(originalType, samResolver, samConversionOracle);
            KotlinType newTypeUnsubstituted = functionType != null ? functionType : originalType;
            KotlinType newType = substitutor.substitute(newTypeUnsubstituted, Variance.IN_VARIANCE);
            assert newType != null : "couldn't substitute type: " + newTypeUnsubstituted + ", substitutor = " + substitutor;

            /*
             * Before 1.5 we allowed passing non-spread arrays into vararg parameter, after sam conversion like:
             *      public static String foo1(Runnable r, String... strs) { }
             *      ...
             *      Test.foo1({}, arrayOf())
             * For that, we don't pass `varargElementType` from the original parameter descriptor.
             */
            KotlinType varargElementType = allowNonSpreadArraysForVarargAfterSam ? null : originalParam.getVarargElementType();

            ValueParameterDescriptor newParam = new ValueParameterDescriptorImpl(
                    samAdapter, null, originalParam.getIndex(), originalParam.getAnnotations(),
                    originalParam.getName(), newType,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    varargElementType, SourceElement.NO_SOURCE
            );
            valueParameters.add(newParam);
        }
        return valueParameters;
    }

    private static abstract class FunctionInitializer {
        public abstract void initialize(
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @NotNull KotlinType returnType
        );
    }
}
