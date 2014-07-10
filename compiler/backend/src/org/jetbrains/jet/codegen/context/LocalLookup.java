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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JvmCodegenUtil;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.binding.MutableClosure;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;

import static org.jetbrains.jet.codegen.AsmUtil.CAPTURED_RECEIVER_FIELD;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;

public interface LocalLookup {
    boolean lookupLocal(DeclarationDescriptor descriptor);

    enum LocalLookupCase {
        VAR {
            @Override
            public boolean isCase(DeclarationDescriptor d) {
                return d instanceof VariableDescriptor && !(d instanceof PropertyDescriptor);
            }

            @Override
            public StackValue innerValue(
                    DeclarationDescriptor d,
                    LocalLookup localLookup,
                    GenerationState state,
                    MutableClosure closure,
                    Type classType
            ) {
                VariableDescriptor vd = (VariableDescriptor) d;

                boolean idx = localLookup != null && localLookup.lookupLocal(vd);
                if (!idx) return null;

                Type sharedVarType = state.getTypeMapper().getSharedVarType(vd);
                Type localType = state.getTypeMapper().mapType(vd);
                Type type = sharedVarType != null ? sharedVarType : localType;

                String fieldName = "$" + vd.getName();
                StackValue innerValue = sharedVarType != null
                                        ? StackValue.fieldForSharedVar(localType, classType, fieldName)
                                        : StackValue.field(type, classType, fieldName, false);

                closure.recordField(fieldName, type);
                closure.captureVariable(new EnclosedValueDescriptor(fieldName, d, innerValue, type));

                return innerValue;
            }
        },

        LOCAL_NAMED_FUNCTION {
            @Override
            public boolean isCase(DeclarationDescriptor d) {
                return isLocalNamedFun(d);
            }

            @Override
            public StackValue innerValue(
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

                MutableClosure localFunClosure = bindingContext.get(CLOSURE, bindingContext.get(CLASS_FOR_FUNCTION, vd));
                if (localFunClosure != null && JvmCodegenUtil.isConst(localFunClosure)) {
                    // This is an optimization: we can obtain an instance of a const closure simply by GETSTATIC ...$instance
                    // (instead of passing this instance to the constructor and storing as a field)
                    return StackValue.field(localType, localType, JvmAbi.INSTANCE_FIELD, true);
                }

                String fieldName = "$" + vd.getName();
                StackValue innerValue = StackValue.field(localType, classType, fieldName, false);

                closure.recordField(fieldName, localType);
                closure.captureVariable(new EnclosedValueDescriptor(fieldName, d, innerValue, localType));

                return innerValue;
            }
        },

        RECEIVER {
            @Override
            public boolean isCase(DeclarationDescriptor d) {
                return d instanceof CallableDescriptor;
            }

            @Override
            public StackValue innerValue(
                    DeclarationDescriptor d,
                    LocalLookup enclosingLocalLookup,
                    GenerationState state,
                    MutableClosure closure,
                    Type classType
            ) {
                if (closure.getEnclosingReceiverDescriptor() != d) return null;

                JetType receiverType = ((CallableDescriptor) d).getReceiverParameter().getType();
                Type type = state.getTypeMapper().mapType(receiverType);
                StackValue innerValue = StackValue.field(type, classType, CAPTURED_RECEIVER_FIELD, false);
                closure.setCaptureReceiver();

                return innerValue;
            }

            @NotNull
            @Override
            public StackValue outerValue(@NotNull EnclosedValueDescriptor d, @NotNull ExpressionCodegen codegen) {
                CallableDescriptor descriptor = (CallableDescriptor) d.getDescriptor();
                return StackValue.local(descriptor.getExpectedThisObject() != null ? 1 : 0, d.getType());
            }
        };

        public abstract boolean isCase(DeclarationDescriptor d);

        public abstract StackValue innerValue(
                DeclarationDescriptor d,
                LocalLookup localLookup,
                GenerationState state,
                MutableClosure closure,
                Type classType
        );

        @NotNull
        public StackValue outerValue(@NotNull EnclosedValueDescriptor d, @NotNull ExpressionCodegen codegen) {
            int idx = codegen.lookupLocalIndex(d.getDescriptor());
            assert idx != -1;

            return StackValue.local(idx, d.getType());
        }
    }
}
