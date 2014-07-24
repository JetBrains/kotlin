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

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
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

    private FunctionDescriptorUtil() {
    }

    public static Map<TypeConstructor, TypeProjection> createSubstitutionContext(@NotNull FunctionDescriptor functionDescriptor, List<JetType> typeArguments) {
        if (functionDescriptor.getTypeParameters().isEmpty()) return Collections.emptyMap();

        Map<TypeConstructor, TypeProjection> result = new HashMap<TypeConstructor, TypeProjection>();

        int typeArgumentsSize = typeArguments.size();
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        assert typeArgumentsSize == typeParameters.size();
        for (int i = 0; i < typeArgumentsSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            result.put(typeParameterDescriptor.getTypeConstructor(), new TypeProjectionImpl(typeArgument));
        }
        return result;
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

    /**
     * Returns function's copy with new parameter list. Note that parameters may belong to other methods or have incorrect "index" property
     * -- it will be fixed by this function.
     */
    @NotNull
    public static FunctionDescriptor replaceFunctionParameters(
            @NotNull FunctionDescriptor function,
            @NotNull List<ValueParameterDescriptor> newParameters
    ) {
        FunctionDescriptorImpl descriptor = SimpleFunctionDescriptorImpl.create(
                function.getContainingDeclaration(),
                function.getAnnotations(),
                function.getName(),
                function.getKind(),
                SourceElement.NO_SOURCE
        );
        List<ValueParameterDescriptor> parameters = new ArrayList<ValueParameterDescriptor>(newParameters.size());
        int idx = 0;
        for (ValueParameterDescriptor parameter : newParameters) {
            JetType returnType = parameter.getReturnType();
            assert returnType != null;

            parameters.add(
                    new ValueParameterDescriptorImpl(
                            descriptor,
                            null,
                            idx,
                            parameter.getAnnotations(),
                            parameter.getName(),
                            returnType,
                            parameter.declaresDefaultValue(),
                            parameter.getVarargElementType(),
                            SourceElement.NO_SOURCE
                    )
            );
            idx++;
        }
        ReceiverParameterDescriptor receiver = function.getReceiverParameter();
        descriptor.initialize(
                receiver == null ? null : receiver.getType(),
                function.getExpectedThisObject(),
                function.getTypeParameters(),
                parameters,
                function.getReturnType(),
                function.getModality(),
                function.getVisibility());
        return descriptor;
    }
}
