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

package org.jetbrains.jet.codegen.context;

import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.binding.MutableClosure;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.types.JetType;

import static org.jetbrains.jet.codegen.AsmUtil.CAPTURED_RECEIVER_FIELD;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.classNameForAnonymousClass;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.isLocalNamedFun;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.callableDescriptorToDeclaration;

public interface LocalLookup {
    boolean lookupLocal(DeclarationDescriptor descriptor);

    enum LocalLookupCase {
        VAR {
            @Override
            public boolean isCase(DeclarationDescriptor d, GenerationState state) {
                return (d instanceof VariableDescriptor) && !(d instanceof PropertyDescriptor);
            }

            @Override
            public StackValue innerValue(
                    DeclarationDescriptor d,
                    LocalLookup localLookup,
                    GenerationState state,
                    MutableClosure closure, JvmClassName className
            ) {
                VariableDescriptor vd = (VariableDescriptor) d;

                boolean idx = localLookup != null && localLookup.lookupLocal(vd);
                if (!idx) return null;

                Type sharedVarType = state.getTypeMapper().getSharedVarType(vd);
                Type localType = state.getTypeMapper().mapType(vd);
                Type type = sharedVarType != null ? sharedVarType : localType;

                String fieldName = "$" + vd.getName();
                StackValue innerValue = sharedVarType != null
                                        ? StackValue.fieldForSharedVar(localType, className, fieldName)
                                        : StackValue.field(type, className, fieldName, false);

                closure.recordField(fieldName, type);
                closure.captureVariable(new EnclosedValueDescriptor(d, innerValue, type));

                return innerValue;
            }
        },

        LOCAL_NAMED_FUNCTION {
            @Override
            public boolean isCase(DeclarationDescriptor d, GenerationState state) {
                return isLocalNamedFun(state.getBindingContext(), d);
            }

            @Override
            public StackValue innerValue(
                    DeclarationDescriptor d,
                    LocalLookup localLookup,
                    GenerationState state,
                    MutableClosure closure,
                    JvmClassName className
            ) {
                FunctionDescriptor vd = (FunctionDescriptor) d;

                boolean idx = localLookup.lookupLocal(vd);
                if (!idx) return null;

                JetElement expression = (JetElement) callableDescriptorToDeclaration(state.getBindingContext(), vd);
                JvmClassName cn = classNameForAnonymousClass(state.getBindingContext(), expression);
                Type localType = cn.getAsmType();

                String fieldName = "$" + vd.getName();
                StackValue innerValue = StackValue.field(localType, className, fieldName, false);

                closure.recordField(fieldName, localType);
                closure.captureVariable(new EnclosedValueDescriptor(d, innerValue, localType));

                return innerValue;
            }
        },

        RECEIVER {
            @Override
            public boolean isCase(DeclarationDescriptor d, GenerationState state) {
                return d instanceof CallableDescriptor;
            }

            @Override
            public StackValue innerValue(
                    DeclarationDescriptor d,
                    LocalLookup enclosingLocalLookup,
                    GenerationState state,
                    MutableClosure closure, JvmClassName className
            ) {
                if (closure.getEnclosingReceiverDescriptor() != d) return null;

                JetType receiverType = ((CallableDescriptor) d).getReceiverParameter().getType();
                Type type = state.getTypeMapper().mapType(receiverType);
                StackValue innerValue = StackValue.field(type, className, CAPTURED_RECEIVER_FIELD, false);
                closure.setCaptureReceiver();

                return innerValue;
            }

            @Override
            public StackValue outerValue(EnclosedValueDescriptor d, ExpressionCodegen expressionCodegen) {
                CallableDescriptor descriptor = (CallableDescriptor) d.getDescriptor();
                return StackValue.local(descriptor.getExpectedThisObject() != null ? 1 : 0, d.getType());
            }
        };

        public abstract boolean isCase(DeclarationDescriptor d, GenerationState state);

        public abstract StackValue innerValue(
                DeclarationDescriptor d,
                LocalLookup localLookup,
                GenerationState state,
                MutableClosure closure,
                JvmClassName className
        );

        public StackValue outerValue(EnclosedValueDescriptor d, ExpressionCodegen expressionCodegen) {
            int idx = expressionCodegen.lookupLocalIndex(d.getDescriptor());
            assert idx != -1;

            return StackValue.local(idx, d.getType());
        }
    }
}
