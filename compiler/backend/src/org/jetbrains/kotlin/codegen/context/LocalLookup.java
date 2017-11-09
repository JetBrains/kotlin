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

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.JvmCodegenUtil;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;

import static org.jetbrains.kotlin.codegen.AsmUtil.CAPTURED_RECEIVER_FIELD;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isLocalFunction;

public interface LocalLookup {
    boolean lookupLocal(DeclarationDescriptor descriptor);

    enum LocalLookupCase {
        VAR {
            @Override
            public boolean isCase(DeclarationDescriptor d) {
                return d instanceof VariableDescriptor && !(d instanceof PropertyDescriptor);
            }

            @Override
            public StackValue.StackValueWithSimpleReceiver innerValue(
                    DeclarationDescriptor d,
                    LocalLookup localLookup,
                    GenerationState state,
                    MutableClosure closure,
                    Type classType
            ) {
                VariableDescriptor vd = (VariableDescriptor) d;

                boolean idx = localLookup != null && localLookup.lookupLocal(vd);
                if (!idx) return null;

                KotlinType delegateType =
                        vd instanceof VariableDescriptorWithAccessors
                        ? JvmCodegenUtil.getPropertyDelegateType((VariableDescriptorWithAccessors) vd, state.getBindingContext())
                        : null;
                Type sharedVarType = state.getTypeMapper().getSharedVarType(vd);
                Type localType = state.getTypeMapper().mapType(delegateType != null ? delegateType : vd.getType());
                Type type = sharedVarType != null ? sharedVarType : localType;

                String fieldName = "$" + vd.getName();
                StackValue.Local thiz = StackValue.LOCAL_0;

                StackValue.StackValueWithSimpleReceiver innerValue;
                EnclosedValueDescriptor enclosedValueDescriptor;
                if (sharedVarType != null) {
                    StackValue.Field wrapperValue = StackValue.receiverWithRefWrapper(localType, classType, fieldName, thiz, vd);
                    innerValue = StackValue.fieldForSharedVar(localType, classType, fieldName, wrapperValue, vd);
                    enclosedValueDescriptor = new EnclosedValueDescriptor(fieldName, d, innerValue, wrapperValue, type);
                }
                else {
                    innerValue = StackValue.field(type, classType, fieldName, false, thiz, vd);
                    enclosedValueDescriptor = new EnclosedValueDescriptor(fieldName, d, innerValue, type);
                }

                closure.captureVariable(enclosedValueDescriptor);

                return innerValue;
            }
        },

        LOCAL_NAMED_FUNCTION {
            @Override
            public boolean isCase(DeclarationDescriptor d) {
                return isLocalFunction(d);
            }

            @Override
            public StackValue.StackValueWithSimpleReceiver innerValue(
                    DeclarationDescriptor d,
                    LocalLookup localLookup,
                    GenerationState state,
                    MutableClosure closure,
                    Type classType
            ) {
                FunctionDescriptor vd = (FunctionDescriptor) d;

                boolean idx = localLookup != null && localLookup.lookupLocal(vd);
                if (!idx) return null;

                BindingContext bindingContext = state.getBindingContext();
                Type localType = asmTypeForAnonymousClass(bindingContext, vd);

                ClassDescriptor callableClass = bindingContext.get(CLASS_FOR_CALLABLE, vd);
                assert callableClass != null : "No CLASS_FOR_CALLABLE:" + vd;

                MutableClosure localFunClosure = bindingContext.get(CLOSURE, callableClass);
                if (localFunClosure != null && JvmCodegenUtil.isConst(localFunClosure)) {
                    // This is an optimization: we can obtain an instance of a const closure simply by GETSTATIC ...$instance
                    // (instead of passing this instance to the constructor and storing as a field)
                    return StackValue.field(localType, localType, JvmAbi.INSTANCE_FIELD, true, StackValue.LOCAL_0, vd);
                }

                String localFunClassName = callableClass.getName().asString();
                int localClassIndexStart = localFunClassName.lastIndexOf('$');
                String localFunSuffix = localClassIndexStart >= 0 ? localFunClassName.substring(localClassIndexStart) : "";

                String fieldName = "$" + vd.getName() + localFunSuffix;
                StackValue.StackValueWithSimpleReceiver innerValue = StackValue.field(localType, classType, fieldName, false,
                                                                                      StackValue.LOCAL_0, vd);

                closure.captureVariable(new EnclosedValueDescriptor(fieldName, d, innerValue, localType));

                return innerValue;
            }
        },

        RECEIVER {
            @Override
            public boolean isCase(DeclarationDescriptor d) {
                return d instanceof ReceiverParameterDescriptor;
            }

            @Override
            public StackValue.StackValueWithSimpleReceiver innerValue(
                    DeclarationDescriptor d,
                    LocalLookup enclosingLocalLookup,
                    GenerationState state,
                    MutableClosure closure,
                    Type classType
            ) {
                if (closure.getEnclosingReceiverDescriptor() != d) {
                    return null;
                }

                KotlinType receiverType = closure.getEnclosingReceiverDescriptor().getType();
                Type type = state.getTypeMapper().mapType(receiverType);
                StackValue.StackValueWithSimpleReceiver innerValue = StackValue.field(type, classType, CAPTURED_RECEIVER_FIELD, false,
                                                                                      StackValue.LOCAL_0, d);
                closure.setCaptureReceiver();

                return innerValue;
            }

            @NotNull
            @Override
            public StackValue outerValue(@NotNull EnclosedValueDescriptor d, @NotNull ExpressionCodegen codegen) {
                CallableDescriptor descriptor = (CallableDescriptor) d.getDescriptor();
                return StackValue.local(descriptor.getDispatchReceiverParameter() != null ? 1 : 0, d.getType());
            }
        };

        public abstract boolean isCase(DeclarationDescriptor d);

        public abstract StackValue.StackValueWithSimpleReceiver innerValue(
                DeclarationDescriptor d,
                LocalLookup localLookup,
                GenerationState state,
                MutableClosure closure,
                Type classType
        );

        @NotNull
        public StackValue outerValue(@NotNull EnclosedValueDescriptor d, @NotNull ExpressionCodegen codegen) {
            DeclarationDescriptor declarationDescriptor = d.getDescriptor();
            int idx = codegen.lookupLocalIndex(declarationDescriptor);
            if (idx >= 0) {
                return StackValue.local(idx, d.getType());
            }
            else {
                assert declarationDescriptor != null : "No declaration descriptor for " + d;
                StackValue capturedValue = codegen.findCapturedValue(declarationDescriptor);
                assert capturedValue != null : "Unresolved captured value for " + d;
                return capturedValue;
            }
        }
    }
}
