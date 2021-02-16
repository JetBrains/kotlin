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

package org.jetbrains.kotlin.resolve;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.ArrayList;
import java.util.List;

public class FunctionDescriptorUtil {
    private static final TypeSubstitutor MAKE_TYPE_PARAMETERS_FRESH = TypeSubstitutor.create(new TypeSubstitution() {
        @Override
        public TypeProjection get(@NotNull KotlinType key) {
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
            @NotNull List<KotlinType> typeArguments
    ) {
        if (functionDescriptor.getTypeParameters().isEmpty()) return TypeSubstitution.EMPTY;

        return new IndexedParametersSubstitution(functionDescriptor.getTypeParameters(), TypeUtilsKt.defaultProjections(typeArguments));
    }

    @NotNull
    public static LexicalScope getFunctionInnerScope(
            @NotNull LexicalScope outerScope, @NotNull FunctionDescriptor descriptor,
            @NotNull BindingTrace trace, @NotNull OverloadChecker overloadChecker
    ) {
        return getFunctionInnerScope(outerScope, descriptor, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker));
    }

    @NotNull
    public static LexicalScope getFunctionInnerScope(
            @NotNull LexicalScope outerScope,
            @NotNull FunctionDescriptor descriptor,
            @NotNull LocalRedeclarationChecker redeclarationChecker
    ) {
        List<ReceiverParameterDescriptor> implicitReceivers = new ArrayList<>();
        ReceiverParameterDescriptor extensionReceiverParameter = descriptor.getExtensionReceiverParameter();
        if (descriptor.getExtensionReceiverParameter() != null) {
            implicitReceivers.add(extensionReceiverParameter);
        }
        List<ReceiverParameterDescriptor> contextReceiverParameters = descriptor.getContextReceiverParameters();
        if (!contextReceiverParameters.isEmpty()) {
            implicitReceivers.addAll(contextReceiverParameters);
        }
        return new LexicalScopeImpl(
                outerScope, descriptor, true, implicitReceivers,
                LexicalScopeKind.FUNCTION_INNER_SCOPE, redeclarationChecker,
                handler -> {
                    for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
                        handler.addClassifierDescriptor(typeParameter);
                    }
                    for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
                        if (valueParameterDescriptor instanceof ValueParameterDescriptorImpl.WithDestructuringDeclaration) {
                            List<VariableDescriptor> entries =
                                    ((ValueParameterDescriptorImpl.WithDestructuringDeclaration) valueParameterDescriptor)
                                            .getDestructuringVariables();
                            for (VariableDescriptor entry : entries) {
                                handler.addVariableDescriptor(entry);
                            }
                        }
                        else {
                            handler.addVariableDescriptor(valueParameterDescriptor);
                        }
                    }
                    return Unit.INSTANCE;
                }
        );
    }

    @SuppressWarnings("unchecked")
    public static <D extends CallableDescriptor> D alphaConvertTypeParameters(D candidate) {
        return (D) candidate.substitute(MAKE_TYPE_PARAMETERS_FRESH);
    }
}
