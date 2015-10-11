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

package org.jetbrains.kotlin.resolve;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionDescriptorUtil {
    private static final TypeSubstitutor MAKE_TYPE_PARAMETERS_FRESH = TypeSubstitutor.create(new TypeSubstitution() {

        @Override
        public TypeProjection get(@NotNull JetType key) {
            return null;
        }

        @Override
        public String toString() {
            return "FunctionDescriptorUtil.MAKE_TYPE_PARAMETERS_FRESH";
        }
    });

    private FunctionDescriptorUtil() {
    }

    public static TypeSubstitution createSubstitution(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull List<JetType> typeArguments
    ) {
        if (functionDescriptor.getTypeParameters().isEmpty()) return TypeSubstitution.getEMPTY();

        return new IndexedParametersSubstitution(functionDescriptor.getTypeParameters(), TypeUtilPackage.defaultProjections(typeArguments));
    }

    @NotNull
    public static LexicalScope getFunctionInnerScope(@NotNull LexicalScope outerScope, @NotNull FunctionDescriptor descriptor, @NotNull BindingTrace trace) {
        TraceBasedRedeclarationHandler redeclarationHandler = new TraceBasedRedeclarationHandler(trace);
        return getFunctionInnerScope(outerScope, descriptor, redeclarationHandler);
    }

    @NotNull
    public static LexicalScope getFunctionInnerScope(
            @NotNull LexicalScope outerScope,
            @NotNull final FunctionDescriptor descriptor,
            @NotNull RedeclarationHandler redeclarationHandler
    ) {
        ReceiverParameterDescriptor receiver = descriptor.getExtensionReceiverParameter();
        return new LexicalScopeImpl(outerScope, descriptor, true, receiver, "Function inner scope", redeclarationHandler,
                                    new Function1<LexicalScopeImpl.InitializeHandler, Unit>() {
                                        @Override
                                        public Unit invoke(LexicalScopeImpl.InitializeHandler handler) {
                                            for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
                                                handler.addClassifierDescriptor(typeParameter);
                                            }
                                            for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
                                                handler.addVariableDescriptor(valueParameterDescriptor);
                                            }
                                            return Unit.INSTANCE$;
                                        }
                                    });
    }

    public static void initializeFromFunctionType(
            @NotNull FunctionDescriptorImpl functionDescriptor,
            @NotNull JetType functionType,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull Modality modality,
            @NotNull Visibility visibility
    ) {

        assert KotlinBuiltIns.isFunctionOrExtensionFunctionType(functionType);
        functionDescriptor.initialize(KotlinBuiltIns.getReceiverType(functionType),
                                      dispatchReceiverParameter,
                                      Collections.<TypeParameterDescriptorImpl>emptyList(),
                                      KotlinBuiltIns.getValueParameters(functionDescriptor, functionType),
                                      KotlinBuiltIns.getReturnTypeFromFunctionType(functionType),
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
        ReceiverParameterDescriptor receiver = function.getExtensionReceiverParameter();
        descriptor.initialize(
                receiver == null ? null : receiver.getType(),
                function.getDispatchReceiverParameter(),
                function.getTypeParameters(),
                parameters,
                function.getReturnType(),
                function.getModality(),
                function.getVisibility());
        descriptor.setOperator(function.isOperator());
        descriptor.setInfix(function.isInfix());
        return descriptor;
    }
}
