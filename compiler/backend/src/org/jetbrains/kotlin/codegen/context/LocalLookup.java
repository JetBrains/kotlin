/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.context;

import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.AsmUtil;
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

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isLocalFunction;

public interface LocalLookup {
    boolean isLocal(DeclarationDescriptor descriptor);

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

                boolean idx = localLookup != null && localLookup.isLocal(vd);
                if (!idx) return null;

                KotlinType delegateType =
                        vd instanceof VariableDescriptorWithAccessors
                        ? JvmCodegenUtil.getPropertyDelegateType((VariableDescriptorWithAccessors) vd, state.getBindingContext())
                        : null;
                Type sharedVarType = state.getTypeMapper().getSharedVarType(vd);
                KotlinType localKotlinType = delegateType != null ? delegateType : vd.getType();
                Type localType = state.getTypeMapper().mapType(localKotlinType);
                Type type = sharedVarType != null ? sharedVarType : localType;
                KotlinType kotlinType = sharedVarType != null ? null : localKotlinType;

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
                    innerValue = StackValue.field(type, kotlinType, classType, fieldName, false, thiz, vd);
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

                boolean idx = localLookup != null && localLookup.isLocal(vd);
                if (!idx) return null;

                BindingContext bindingContext = state.getBindingContext();
                Type localType = asmTypeForAnonymousClass(bindingContext, vd);

                ClassDescriptor callableClass = bindingContext.get(CLASS_FOR_CALLABLE, vd);
                assert callableClass != null : "No CLASS_FOR_CALLABLE:" + vd;

                MutableClosure localFunClosure = bindingContext.get(CLOSURE, callableClass);
                if (localFunClosure != null && JvmCodegenUtil.isConst(localFunClosure)) {
                    // This is an optimization: we can obtain an instance of a const closure simply by GETSTATIC ...$instance
                    // (instead of passing this instance to the constructor and storing as a field)
                    return StackValue.field(localType, null, localType, JvmAbi.INSTANCE_FIELD, true, StackValue.LOCAL_0, vd);
                }

                String internalName = localType.getInternalName();
                String simpleName = StringsKt.substringAfterLast(internalName, "/", internalName);
                int localClassIndexStart = simpleName.lastIndexOf('$');
                String localFunSuffix = localClassIndexStart >= 0 ? simpleName.substring(localClassIndexStart) : "";

                String fieldName = "$" + vd.getName() + localFunSuffix;
                StackValue.StackValueWithSimpleReceiver innerValue = StackValue.field(
                        localType, null, classType, fieldName, false, StackValue.LOCAL_0, vd
                );

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
                ReceiverParameterDescriptor enclosingReceiverDescriptor = closure.getEnclosingReceiverDescriptor();
                if (enclosingReceiverDescriptor != d) {
                    return null;
                }

                assert(enclosingReceiverDescriptor != null);

                KotlinType receiverType = enclosingReceiverDescriptor.getType();
                Type type = state.getTypeMapper().mapType(receiverType);
                String fieldName = closure.getCapturedReceiverFieldName(state.getBindingContext(), state.getLanguageVersionSettings());
                StackValue.StackValueWithSimpleReceiver innerValue = StackValue.field(
                        type, receiverType, classType, fieldName, false, StackValue.LOCAL_0, d
                );
                closure.setNeedsCaptureReceiverFromOuterContext();

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
