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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public class FunctionDescriptorUtil {
    private static final TypeSubstitutor MAKE_TYPE_PARAMETERS_FRESH = TypeSubstitutor.create(new TypeSubstitution() {

        @Override
        public TypeProjection get(TypeConstructor key) {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public String toString() {
            return "FunctionDescriptorUtil.MAKE_TYPE_PARAMETERS_FRESH";
        }
    });

    public static Map<TypeConstructor, TypeProjection> createSubstitutionContext(@NotNull FunctionDescriptor functionDescriptor, List<JetType> typeArguments) {
        if (functionDescriptor.getTypeParameters().isEmpty()) return Collections.emptyMap();

        Map<TypeConstructor, TypeProjection> result = new HashMap<TypeConstructor, TypeProjection>();

        int typeArgumentsSize = typeArguments.size();
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        assert typeArgumentsSize == typeParameters.size();
        for (int i = 0; i < typeArgumentsSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            result.put(typeParameterDescriptor.getTypeConstructor(), new TypeProjection(typeArgument));
        }
        return result;
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(FunctionDescriptor substitutedDescriptor, @NotNull FunctionDescriptor functionDescriptor, @NotNull TypeSubstitutor substitutor) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getValueParameters();
        for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
            // TODO : Lazy?
            JetType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType(), Variance.IN_VARIANCE);
            JetType varargElementType = unsubstitutedValueParameter.getVarargElementType();
            JetType substituteVarargElementType = varargElementType == null ? null : substitutor.substitute(varargElementType, Variance.IN_VARIANCE);
            if (substitutedType == null) return null;
            result.add(new ValueParameterDescriptorImpl(
                    substitutedDescriptor,
                    unsubstitutedValueParameter,
                    unsubstitutedValueParameter.getAnnotations(),
                    unsubstitutedValueParameter.isVar(),
                    substitutedType,
                    substituteVarargElementType
            ));
        }
        return result;
    }

    @Nullable
    public static JetType getSubstitutedReturnType(@NotNull FunctionDescriptor functionDescriptor, TypeSubstitutor substitutor) {
        return substitutor.substitute(functionDescriptor.getReturnType(), Variance.OUT_VARIANCE);
    }

    @Nullable
    public static FunctionDescriptor substituteFunctionDescriptor(@NotNull List<JetType> typeArguments, @NotNull FunctionDescriptor functionDescriptor) {
        Map<TypeConstructor, TypeProjection> substitutionContext = createSubstitutionContext(functionDescriptor, typeArguments);
        return functionDescriptor.substitute(TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    public static JetScope getFunctionInnerScope(@NotNull JetScope outerScope, @NotNull FunctionDescriptor descriptor, @NotNull BindingTrace trace) {
        WritableScope parameterScope = new WritableScopeImpl(outerScope, descriptor, new TraceBasedRedeclarationHandler(trace), "Function inner scope");
        ReceiverParameterDescriptor receiver = descriptor.getReceiverParameter();
        if (receiver != null) {
            parameterScope.setImplicitReceiver(receiver);
        }
        for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
            parameterScope.addTypeParameterDescriptor(typeParameter);
        }
        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            parameterScope.addVariableDescriptor(valueParameterDescriptor);
        }
        parameterScope.addLabeledDeclaration(descriptor);
        parameterScope.changeLockLevel(WritableScope.LockLevel.READING);
        return parameterScope;
    }

    public static void initializeFromFunctionType(
            @NotNull FunctionDescriptorImpl functionDescriptor,
            @NotNull JetType functionType,
            @Nullable ReceiverParameterDescriptor expectedThisObject,
            @NotNull Modality modality,
            @NotNull Visibility visibility
    ) {

        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(functionType);
        functionDescriptor.initialize(KotlinBuiltIns.getInstance().getReceiverType(functionType),
                                      expectedThisObject,
                                      Collections.<TypeParameterDescriptorImpl>emptyList(),
                                      KotlinBuiltIns.getInstance().getValueParameters(functionDescriptor, functionType),
                                      KotlinBuiltIns.getInstance().getReturnTypeFromFunctionType(functionType),
                                      modality,
                                      visibility);
    }

    public static <D extends CallableDescriptor> D alphaConvertTypeParameters(D candidate) {
        return (D) candidate.substitute(MAKE_TYPE_PARAMETERS_FRESH);
    }

    public static FunctionDescriptor getInvokeFunction(@NotNull JetType functionType) {
        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(functionType);

        ClassifierDescriptor classDescriptorForFunction = functionType.getConstructor().getDeclarationDescriptor();
        assert classDescriptorForFunction instanceof ClassDescriptor;
        Collection<FunctionDescriptor> invokeFunctions = ((ClassDescriptor) classDescriptorForFunction).getMemberScope(functionType.getArguments()).getFunctions(Name.identifier("invoke"));
        assert invokeFunctions.size() == 1;
        return invokeFunctions.iterator().next();
    }
}
