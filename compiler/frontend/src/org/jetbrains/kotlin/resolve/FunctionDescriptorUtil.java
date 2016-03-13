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
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

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
            @NotNull LexicalScope outerScope, @NotNull FunctionDescriptor descriptor, @NotNull BindingTrace trace
    ) {
        return getFunctionInnerScope(outerScope, descriptor, new TraceBasedLocalRedeclarationChecker(trace));
    }

    @NotNull
    public static LexicalScope getFunctionInnerScope(
            @NotNull LexicalScope outerScope,
            @NotNull final FunctionDescriptor descriptor,
            @NotNull LocalRedeclarationChecker redeclarationChecker
    ) {
        ReceiverParameterDescriptor receiver = descriptor.getExtensionReceiverParameter();
        return new LexicalScopeImpl(outerScope, descriptor, true, receiver, LexicalScopeKind.FUNCTION_INNER_SCOPE, redeclarationChecker,
                                    new Function1<LexicalScopeImpl.InitializeHandler, Unit>() {
                                        @Override
                                        public Unit invoke(LexicalScopeImpl.InitializeHandler handler) {
                                            for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
                                                handler.addClassifierDescriptor(typeParameter);
                                            }
                                            for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
                                                handler.addVariableDescriptor(valueParameterDescriptor);
                                            }
                                            return Unit.INSTANCE;
                                        }
                                    });
    }

    @SuppressWarnings("unchecked")
    public static <D extends CallableDescriptor> D alphaConvertTypeParameters(D candidate) {
        return (D) candidate.substitute(MAKE_TYPE_PARAMETERS_FRESH);
    }
}
