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

package org.jetbrains.kotlin.resolve.jvm.kotlinSignature;

import com.google.common.collect.Lists;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor;
import org.jetbrains.kotlin.load.java.structure.JavaMethod;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.KotlinToJvmSignatureMapper;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.KotlinToJvmSignatureMapperKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

public class SignaturesPropagationData {

    private static final KotlinToJvmSignatureMapper SIGNATURE_MAPPER = ServiceLoader.load(
            KotlinToJvmSignatureMapper.class,
            KotlinToJvmSignatureMapper.class.getClassLoader()
    ).iterator().next();

    private final ValueParameters modifiedValueParameters;
    private final List<String> signatureErrors = new ArrayList<>(0);
    private final List<FunctionDescriptor> superFunctions;

    public SignaturesPropagationData(
            @NotNull ClassDescriptor containingClass,
            @NotNull KotlinType autoReturnType, // type built by JavaTypeTransformer from Java signature and @NotNull annotations
            @Nullable KotlinType receiverType,
            @NotNull List<ValueParameterDescriptor> autoValueParameters, // descriptors built by parameters resolver
            @NotNull List<TypeParameterDescriptor> autoTypeParameters, // descriptors built by signature resolver
            @NotNull JavaMethod method
    ) {
        assert receiverType == null : "Parameters before propagation have receiver type," +
                                      " but propagation should be disabled for functions compiled from Kotlin in class: " +
                                      DescriptorUtils.getFqName(containingClass);

        JavaMethodDescriptor autoMethodDescriptor =
                createAutoMethodDescriptor(containingClass, method, autoReturnType, autoValueParameters, autoTypeParameters);

        superFunctions = getSuperFunctionsForMethod(method, autoMethodDescriptor, containingClass);
        modifiedValueParameters = superFunctions.isEmpty()
                                  ? new ValueParameters(null, autoValueParameters, false)
                                  : modifyValueParametersAccordingToSuperMethods(autoValueParameters);
    }

    @NotNull
    private static JavaMethodDescriptor createAutoMethodDescriptor(
            @NotNull ClassDescriptor containingClass,
            @NotNull JavaMethod method, KotlinType autoReturnType,
            @NotNull List<ValueParameterDescriptor> autoValueParameters,
            @NotNull List<TypeParameterDescriptor> autoTypeParameters
    ) {
        JavaMethodDescriptor autoMethodDescriptor = JavaMethodDescriptor.createJavaMethod(
                containingClass,
                Annotations.Companion.getEMPTY(),
                method.getName(),
                //TODO: what to do?
                SourceElement.NO_SOURCE,
                false
        );
        autoMethodDescriptor.initialize(
                null, containingClass.getThisAsReceiverParameter(), CollectionsKt.emptyList(),
                autoTypeParameters,
                autoValueParameters,
                autoReturnType,
                Modality.OPEN,
                DescriptorVisibilities.PUBLIC
        );
        return autoMethodDescriptor;
    }

    public KotlinType getModifiedReceiverType() {
        return modifiedValueParameters.receiverType;
    }

    public List<ValueParameterDescriptor> getModifiedValueParameters() {
        return modifiedValueParameters.descriptors;
    }

    public boolean getModifiedHasStableParameterNames() {
        return modifiedValueParameters.hasStableParameterNames;
    }

    public List<String> getSignatureErrors() {
        return signatureErrors;
    }

    void reportError(String error) {
        signatureErrors.add(error);
    }

    private ValueParameters modifyValueParametersAccordingToSuperMethods(@NotNull List<ValueParameterDescriptor> parameters) {
        KotlinType resultReceiverType = null;
        List<ValueParameterDescriptor> resultParameters = new ArrayList<>(parameters.size());

        boolean shouldBeExtension = checkIfShouldBeExtension();

        for (ValueParameterDescriptor originalParam : parameters) {
            int originalIndex = originalParam.getIndex();
            List<TypeAndName> typesFromSuperMethods = CollectionsKt.map(
                    superFunctions,
                    superFunction -> {
                        ReceiverParameterDescriptor receiver = superFunction.getExtensionReceiverParameter();
                        int index = receiver != null ? originalIndex - 1 : originalIndex;
                        if (index == -1) {
                            assert receiver != null : "can't happen: index is -1, while function is not extension";
                            return new TypeAndName(receiver.getType(), originalParam.getName());
                        }
                        ValueParameterDescriptor parameter = superFunction.getValueParameters().get(index);
                        return new TypeAndName(parameter.getType(), parameter.getName());
                    }
            );

            VarargCheckResult varargCheckResult = checkVarargInSuperFunctions(originalParam);

            KotlinType altType = varargCheckResult.parameterType;

            if (shouldBeExtension && originalIndex == 0) {
                resultReceiverType = altType;
            }
            else {
                Name stableName = null;
                for (int i = 0; i < superFunctions.size(); i++) {
                    if (superFunctions.get(i).hasStableParameterNames()) {
                        // When there's more than one stable name in super functions, we pick the first one. This behaviour is similar to
                        // the compiler front-end, except that it reports a warning in such cases
                        // TODO: report a warning somewhere if there's more than one stable name in super functions
                        stableName = typesFromSuperMethods.get(i).name;
                        break;
                    }
                }

                resultParameters.add(new ValueParameterDescriptorImpl(
                        originalParam.getContainingDeclaration(),
                        null,
                        shouldBeExtension ? originalIndex - 1 : originalIndex,
                        originalParam.getAnnotations(),
                        stableName != null ? stableName : originalParam.getName(),
                        altType,
                        originalParam.declaresDefaultValue(),
                        originalParam.isCrossinline(),
                        originalParam.isNoinline(),
                        varargCheckResult.isVararg ? DescriptorUtilsKt.getBuiltIns(originalParam).getArrayElementType(altType) : null,
                        SourceElement.NO_SOURCE
                ));
            }
        }

        boolean hasStableParameterNames = CollectionsKt.any(superFunctions, CallableDescriptor::hasStableParameterNames);

        return new ValueParameters(resultReceiverType, resultParameters, hasStableParameterNames);
    }

    private static List<FunctionDescriptor> getSuperFunctionsForMethod(
            @NotNull JavaMethod method,
            @NotNull JavaMethodDescriptor autoMethodDescriptor,
            @NotNull ClassDescriptor containingClass
    ) {
        List<FunctionDescriptor> superFunctions = Lists.newArrayList();

        // TODO: Add propagation for other kotlin descriptors (KT-3621)
        Name name = method.getName();
        Method autoSignature = null;
        boolean autoMethodContainsVararg = SignaturePropagationUtilKt.containsVarargs(autoMethodDescriptor);
        for (KotlinType supertype : containingClass.getTypeConstructor().getSupertypes()) {
            Collection<? extends SimpleFunctionDescriptor> superFunctionCandidates =
                    supertype.getMemberScope().getContributedFunctions(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS);

            if (!autoMethodContainsVararg && !SignaturePropagationUtilKt.containsAnyNotTrivialSignature(superFunctionCandidates)) continue;

            if (autoSignature == null) {
                autoSignature = SIGNATURE_MAPPER.mapToJvmMethodSignature(autoMethodDescriptor);
            }

            for (FunctionDescriptor candidate : superFunctionCandidates) {
                // Skip suspend super functions, because we doesn't process them correctly by now
                // Moreover, we fail with exception sometimes
                // TODO: remove this continue when KT-15747 is fixed
                if (candidate.isSuspend()) continue;
                Method candidateSignature = SIGNATURE_MAPPER.mapToJvmMethodSignature(candidate);
                if (KotlinToJvmSignatureMapperKt.erasedSignaturesEqualIgnoringReturnTypes(autoSignature, candidateSignature)) {
                    superFunctions.add(candidate);
                }
            }
        }

        // sorting for diagnostic stability
        superFunctions.sort(Comparator.comparing(fun -> getFqName(fun.getContainingDeclaration()).asString()));

        return superFunctions;
    }

    private boolean checkIfShouldBeExtension() {
        boolean someSupersExtension = false;
        boolean someSupersNotExtension = false;

        for (FunctionDescriptor superFunction : superFunctions) {
            if (superFunction.getExtensionReceiverParameter() != null)  {
                someSupersExtension = true;
            }
            else {
                someSupersNotExtension = true;
            }
        }

        if (someSupersExtension) {
            if (someSupersNotExtension) {
                reportError("Incompatible super methods: some are extension functions, some are not");
            }
            else {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private VarargCheckResult checkVarargInSuperFunctions(@NotNull ValueParameterDescriptor originalParam) {
        boolean someSupersVararg = false;
        boolean someSupersNotVararg = false;
        for (FunctionDescriptor superFunction : superFunctions) {
            int originalIndex = originalParam.getIndex();
            int index = superFunction.getExtensionReceiverParameter() != null ? originalIndex - 1 : originalIndex;
            if (index != -1 && superFunction.getValueParameters().get(index).getVarargElementType() != null) {
                someSupersVararg = true;
            }
            else {
                someSupersNotVararg = true;
            }
        }

        KotlinType originalVarargElementType = originalParam.getVarargElementType();
        KotlinType originalType = originalParam.getType();

        if (someSupersVararg && someSupersNotVararg) {
            reportError("Incompatible super methods: some have vararg parameter, some have not");
            return new VarargCheckResult(originalType, originalVarargElementType != null);
        }

        if (someSupersVararg && originalVarargElementType == null) {
            // convert to vararg

            assert isArrayType(originalType);
            return new VarargCheckResult(TypeUtils.makeNotNullable(originalType), true);
        }
        else if (someSupersNotVararg && originalVarargElementType != null) {
            // convert to non-vararg

            assert isArrayType(originalType);
            return new VarargCheckResult(TypeUtils.makeNullable(originalType), false);
        }
        return new VarargCheckResult(originalType, originalVarargElementType != null);
    }

    private static boolean isArrayType(@NotNull KotlinType type) {
        return KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type);
    }

    private static class VarargCheckResult {
        public final KotlinType parameterType;
        public final boolean isVararg;

        public VarargCheckResult(KotlinType parameterType, boolean isVararg) {
            this.parameterType = parameterType;
            this.isVararg = isVararg;
        }
    }

    private static class TypeAndName {
        public final KotlinType type;
        public final Name name;

        public TypeAndName(KotlinType type, Name name) {
            this.type = type;
            this.name = name;
        }
    }

    private static class ValueParameters {
        private final KotlinType receiverType;
        private final List<ValueParameterDescriptor> descriptors;
        private final boolean hasStableParameterNames;

        public ValueParameters(
                @Nullable KotlinType receiverType,
                @NotNull List<ValueParameterDescriptor> descriptors,
                boolean hasStableParameterNames
        ) {
            this.receiverType = receiverType;
            this.descriptors = descriptors;
            this.hasStableParameterNames = hasStableParameterNames;
        }
    }
}
