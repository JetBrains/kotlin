/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author alex.tkachman
 */
public interface LocalLookup {
    boolean lookupLocal(DeclarationDescriptor descriptor);

    enum LocalLookupCase {
        VAR {
            @Override
            public boolean isCase(DeclarationDescriptor d, GenerationState state) {
                return (d instanceof VariableDescriptor) && !(d instanceof PropertyDescriptor);
            }

            @Override
            public StackValue innerValue(DeclarationDescriptor d, LocalLookup localLookup, GenerationState state, MutableClosure closure) {
                VariableDescriptor vd = (VariableDescriptor) d;

                final boolean idx = localLookup != null && localLookup.lookupLocal(vd);
                if (!idx) return null;

                final Type sharedVarType = state.getInjector().getJetTypeMapper().getSharedVarType(vd);
                Type localType = state.getInjector().getJetTypeMapper().mapType(vd.getType(), MapTypeMode.VALUE);
                final Type type = sharedVarType != null ? sharedVarType : localType;

                final String fieldName = "$" + vd.getName();
                StackValue innerValue = sharedVarType != null
                                        ? StackValue.fieldForSharedVar(localType, closure.getClassName(), fieldName)
                                        : StackValue.field(type, closure.getClassName(), fieldName, false);

                closure.recordField(fieldName, type);
                closure.captureVariable(new EnclosedValueDescriptor(d, innerValue, type));

                return innerValue;
            }
        },

        LOCAL_NAMED_FUNCTION {
            @Override
            public boolean isCase(DeclarationDescriptor d, GenerationState state) {
                return CodegenUtil.isNamedFun(d, state.getBindingContext()) && (d.getContainingDeclaration() instanceof FunctionDescriptor);
            }

            @Override
            public StackValue innerValue(DeclarationDescriptor d, LocalLookup localLookup, GenerationState state, MutableClosure closure) {
                FunctionDescriptor vd = (FunctionDescriptor) d;

                final boolean idx = localLookup.lookupLocal(vd);
                if (!idx) return null;

                JetElement expression = (JetElement) BindingContextUtils.callableDescriptorToDeclaration(state.getBindingContext(), vd);
                JvmClassName cn = state.getInjector().getJetTypeMapper().getClosureAnnotator().classNameForAnonymousClass(expression);
                Type localType = cn.getAsmType();

                final String fieldName = "$" + vd.getName();
                StackValue innerValue = StackValue.field(localType, closure.getClassName(), fieldName, false);

                closure.recordField(fieldName, localType);
                closure.captureVariable(new EnclosedValueDescriptor(d, innerValue, localType));

                return innerValue;
            }
        },

        RECEIVER {
            @Override
            public boolean isCase(DeclarationDescriptor d, GenerationState state) {
                return d instanceof FunctionDescriptor;
            }

            @Override
            public StackValue innerValue(
                    DeclarationDescriptor d,
                    LocalLookup enclosingLocalLookup,
                    GenerationState state,
                    MutableClosure closure
            ) {
                if (closure.getEnclosingReceiverDescriptor() != d) return null;

                final JetType receiverType = ((CallableDescriptor) d).getReceiverParameter().getType();
                Type type = state.getInjector().getJetTypeMapper().mapType(receiverType, MapTypeMode.VALUE);
                StackValue innerValue = StackValue.field(type, closure.getClassName(), CodegenUtil.RECEIVER$0, false);
                closure.setCaptureReceiver();

                return innerValue;
            }

            @Override
            public StackValue outerValue(EnclosedValueDescriptor d, ExpressionCodegen expressionCodegen) {
                CallableDescriptor descriptor = (FunctionDescriptor) d.getDescriptor();
                return StackValue.local(descriptor.getExpectedThisObject().exists() ? 1 : 0, d.getType());
            }
        };

        public abstract boolean isCase(DeclarationDescriptor d, GenerationState state);

        public abstract StackValue innerValue(
                DeclarationDescriptor d,
                LocalLookup localLookup,
                GenerationState state,
                MutableClosure closure
        );

        public StackValue outerValue(EnclosedValueDescriptor d, ExpressionCodegen expressionCodegen) {
            final int idx = expressionCodegen.lookupLocalIndex(d.getDescriptor());
            assert idx != -1;

            return StackValue.local(idx, d.getType());
        }
    }
}
