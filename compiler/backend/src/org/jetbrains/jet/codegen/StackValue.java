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

package org.jetbrains.jet.codegen;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethod;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public abstract class StackValue {
    public final Type type;

    public StackValue(Type type) {
        this.type = type;
    }

    public static void valueOf(InstructionAdapter instructionAdapter, final Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return;
        }
        if (type == Type.VOID_TYPE) {
            instructionAdapter.aconst(null);
        }
        else {
            Type boxed = JetTypeMapper.boxType(type);
            instructionAdapter.invokestatic(boxed.getInternalName(), "valueOf", "(" + type.getDescriptor() + ")" + boxed.getDescriptor());
        }
    }

    public abstract void put(Type type, InstructionAdapter v);

    public void store(InstructionAdapter v) {
        throw new UnsupportedOperationException("cannot store to value " + this);
    }

    public void dupReceiver(InstructionAdapter v) {
    }

    public int receiverSize() {
        return 0;
    }

    public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
        if (this.type == Type.BOOLEAN_TYPE) {
            put(Type.BOOLEAN_TYPE, v);
            if (jumpIfFalse) {
                v.ifeq(label);
            }
            else {
                v.ifne(label);
            }
        }
        else {
            throw new UnsupportedOperationException("can't generate a cond jump for a non-boolean value");
        }
    }

    public static StackValue local(int index, Type type) {
        return new Local(index, type);
    }

    public static StackValue shared(int index, Type type) {
        return new Shared(index, type);
    }

    public static StackValue onStack(Type type) {
        return type == Type.VOID_TYPE ? none() : new OnStack(type);
    }

    public static StackValue constant(Object value, Type type) {
        return new Constant(value, type);
    }

    public static StackValue cmp(IElementType opToken, Type type) {
        return type.getSort() == Type.OBJECT ? new ObjectCompare(opToken, type) : new NumberCompare(opToken, type);
    }

    public static StackValue not(StackValue stackValue) {
        return new Invert(stackValue);
    }

    public static StackValue arrayElement(Type type, boolean unbox) {
        return new ArrayElement(type, unbox);
    }

    public static StackValue collectionElement(Type type, ResolvedCall<FunctionDescriptor> getter, ResolvedCall<FunctionDescriptor> setter, ExpressionCodegen codegen, GenerationState state) {
        return new CollectionElement(type, getter, setter, codegen, state);
    }

    public static StackValue field(Type type, String owner, String name, boolean isStatic) {
        return new Field(type, owner, name, isStatic);
    }

    public static Property property(String name, String methodOwner, String methodOwnerParam, Type type, boolean isStatic, boolean isInterface, boolean isSuper, Method getter, Method setter, int invokeOpcode) {
        return new Property(name, methodOwner, methodOwnerParam, getter, setter, isStatic, isInterface, isSuper, type, invokeOpcode);
    }

    public static StackValue expression(Type type, JetExpression expression, ExpressionCodegen generator) {
        return new Expression(type, expression, generator);
    }

    private static void box(final Type type, final Type toType, InstructionAdapter v) {
        // TODO handle toType correctly
        if (type == Type.INT_TYPE || (JetTypeMapper.isIntPrimitive(type) && toType.getInternalName().equals("java/lang/Integer"))) {
            v.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
        }
        else if (type == Type.BOOLEAN_TYPE) {
            v.invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        }
        else if (type == Type.CHAR_TYPE) {
            v.invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        }
        else if (type == Type.SHORT_TYPE) {
            v.invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
        }
        else if (type == Type.LONG_TYPE) {
            v.invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        }
        else if (type == Type.BYTE_TYPE) {
            v.invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
        }
    }

    private static void unbox(final Type type, InstructionAdapter v) {
        if (type == Type.INT_TYPE) {
            v.invokevirtual("java/lang/Number", "intValue", "()I");
        }
        else if (type == Type.BOOLEAN_TYPE) {
            v.invokevirtual("java/lang/Boolean", "booleanValue", "()Z");
        }
        else if (type == Type.CHAR_TYPE) {
            v.invokevirtual("java/lang/Character", "charValue", "()C");
        }
        else if (type == Type.SHORT_TYPE) {
            v.invokevirtual("java/lang/Number", "shortValue", "()S");
        }
        else if (type == Type.LONG_TYPE) {
            v.invokevirtual("java/lang/Number", "longValue", "()J");
        }
        else if (type == Type.BYTE_TYPE) {
            v.invokevirtual("java/lang/Number", "byteValue", "()B");
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokevirtual("java/lang/Number", "floatValue", "()F");
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokevirtual("java/lang/Number", "doubleValue", "()D");
        }
    }

    public void upcast(Type type, InstructionAdapter v) {
        if (type.equals(this.type)) return;

        if (type.getSort() == Type.OBJECT && this.type.getSort() == Type.OBJECT) {
            v.checkcast(type);
        }
        else {
            coerce(type, v);
        }
    }

    protected void coerce(Type type, InstructionAdapter v) {
        if (type.equals(this.type)) return;

        if (type.getSort() == Type.VOID && this.type.getSort() != Type.VOID) {
            if(this.type.getSize() == 1)
                v.pop();
            else
                v.pop2();
        }
        else if (type.getSort() != Type.VOID && this.type.getSort() == Type.VOID) {
            if(type.getSort() == Type.OBJECT)
                v.visitFieldInsn(Opcodes.GETSTATIC, "jet/Tuple0", "INSTANCE", "Ljet/Tuple0;");
            else if(type == Type.LONG_TYPE)
                v.lconst(0);
            else if(type == Type.FLOAT_TYPE)
                v.fconst(0);
            else if(type == Type.DOUBLE_TYPE)
                v.dconst(0);
            else
                v.iconst(0);
        }
        else if (type.getSort() == Type.OBJECT && this.type.equals(JetTypeMapper.TYPE_OBJECT) || type.getSort() == Type.ARRAY) {
                v.checkcast(type);
        }
        else if (type.getSort() == Type.OBJECT) {
            if(this.type.getSort() == Type.OBJECT && !type.equals(JetTypeMapper.TYPE_OBJECT)) {
                v.checkcast(type);
            }
            else
                box(this.type, type, v);
        }
        else if (this.type.getSort() == Type.OBJECT && type.getSort() <= Type.DOUBLE) {
            if (this.type.equals(JetTypeMapper.TYPE_OBJECT)) {
                if (type.getSort() == Type.BOOLEAN) {
                    v.checkcast(JvmPrimitiveType.BOOLEAN.getWrapper().getAsmType());
                }
                else if (type.getSort() == Type.CHAR) {
                    v.checkcast(JvmPrimitiveType.CHAR.getWrapper().getAsmType());
                }
                else {
                    v.checkcast(JetTypeMapper.JL_NUMBER_TYPE);
                }
            }
            unbox(type, v);
        }
        else {
            v.cast(this.type, type);
        }
    }

    protected void putAsBoolean(InstructionAdapter v) {
        Label ifTrue = new Label();
        Label end = new Label();
        condJump(ifTrue, false, v);
        v.iconst(0);
        v.goTo(end);
        v.mark(ifTrue);
        v.iconst(1);
        v.mark(end);
    }

    public static StackValue none() {
        return None.INSTANCE;
    }

    public static StackValue fieldForSharedVar(Type type, String name, String fieldName) {
        return new FieldForSharedVar(type, name, fieldName);
    }

    public static StackValue composed(StackValue prefix, StackValue suffix) {
        return new Composed(prefix, suffix);
    }

    public static StackValue thisOrOuter(ExpressionCodegen codegen, ClassDescriptor descriptor) {
        return new ThisOuter(codegen, descriptor);
    }

    public static StackValue postIncrement(int index, int increment) {
        return new PostIncrement(index, increment);
    }

    public static StackValue preIncrement(int index, int increment) {
        return new PreIncrement(index, increment);
    }

    public static StackValue receiver(ResolvedCall<? extends CallableDescriptor> resolvedCall, StackValue receiver, ExpressionCodegen codegen, @Nullable CallableMethod callableMethod, GenerationState state) {
        if(resolvedCall.getThisObject().exists() || resolvedCall.getReceiverArgument().exists())
            return new CallReceiver(resolvedCall, receiver, codegen, state, callableMethod);
        return receiver;
    }

    private static class None extends StackValue {
        public static None INSTANCE = new None();
        private None() {
            super(Type.VOID_TYPE);
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            coerce(type, v);
        }
    }

    public static class Local extends StackValue {
        final int index;

        public Local(int index, Type type) {
            super(type);
            this.index = index;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.load(index, this.type);
            coerce(type, v);
            // TODO unbox
        }

        @Override
        public void store(InstructionAdapter v) {
            v.store(index, this.type);
        }
    }

    public static class OnStack extends StackValue {
        public OnStack(Type type) {
            super(type);
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (type == Type.VOID_TYPE && this.type != Type.VOID_TYPE) {
                if (this.type.getSize() == 2) {
                    v.pop2();
                }
                else {
                    v.pop();
                }
            }
            else {
                coerce(type, v);
            }
        }
    }

    public static class Constant extends StackValue {
        private final Object value;

        public Constant(Object value, Type type) {
            super(type);
            this.value = value;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if(value instanceof Integer)
                v.iconst((Integer) value);
            else
            if(value instanceof Long)
                v.lconst((Long) value);
            else
            if(value instanceof Float)
                v.fconst((Float) value);
            else
            if(value instanceof Double)
                v.dconst((Double) value);
            else
                v.aconst(value);
            coerce(type, v);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            if (value instanceof Boolean) {
                boolean boolValue = (Boolean) value;
                if (boolValue ^ jumpIfFalse) {
                    v.goTo(label);
                }
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
        }
    }

    private static class NumberCompare extends StackValue {
        protected final IElementType opToken;
        private final Type operandType;

        public NumberCompare(IElementType opToken, Type operandType) {
            super(Type.BOOLEAN_TYPE);
            this.opToken = opToken;
            this.operandType = operandType;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (type == Type.VOID_TYPE) {
                return;
            }
            if (type != Type.BOOLEAN_TYPE) {
                throw new UnsupportedOperationException("don't know how to put a compare as a non-boolean type " + type);
            }
            putAsBoolean(v);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            int opcode;
            if (opToken == JetTokens.EQEQ) {
                opcode = jumpIfFalse ? Opcodes.IFNE : Opcodes.IFEQ;
            }
            else if (opToken == JetTokens.EXCLEQ) {
                opcode = jumpIfFalse ? Opcodes.IFEQ : Opcodes.IFNE;
            }
            else if (opToken == JetTokens.GT) {
                opcode = jumpIfFalse ? Opcodes.IFLE : Opcodes.IFGT;
            }
            else if (opToken == JetTokens.GTEQ) {
                opcode = jumpIfFalse ? Opcodes.IFLT : Opcodes.IFGE;
            }
            else if (opToken == JetTokens.LT) {
                opcode = jumpIfFalse ? Opcodes.IFGE : Opcodes.IFLT;
            }
            else if (opToken == JetTokens.LTEQ) {
                opcode = jumpIfFalse ? Opcodes.IFGT : Opcodes.IFLE;
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
            if (operandType == Type.FLOAT_TYPE || operandType == Type.DOUBLE_TYPE) {
                if (opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
                    v.cmpg(operandType);
                }
                else {
                    v.cmpl(operandType);
                }
            }
            else if (operandType == Type.LONG_TYPE) {
                v.lcmp();
            }
            else {
                opcode += (Opcodes.IF_ICMPEQ - Opcodes.IFEQ);
            }
            v.visitJumpInsn(opcode, label);
        }
    }

    private static class ObjectCompare extends NumberCompare {
        public ObjectCompare(IElementType opToken, Type operandType) {
            super(opToken, operandType);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            int opcode;
            if (opToken == JetTokens.EQEQEQ) {
                opcode = jumpIfFalse ? Opcodes.IF_ACMPNE : Opcodes.IF_ACMPEQ;
            }
            else if (opToken == JetTokens.EXCLEQEQEQ) {
                opcode = jumpIfFalse ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE;
            }
            else {
                throw new UnsupportedOperationException("don't know how to generate this condjump");
            }
            v.visitJumpInsn(opcode, label);
        }
    }

    private static class Invert extends StackValue {
        private StackValue myOperand;

        private Invert(StackValue operand) {
            super(Type.BOOLEAN_TYPE);
            myOperand = operand;
            if(myOperand.type != Type.BOOLEAN_TYPE)
                throw new UnsupportedOperationException("operand of ! must be boolean");
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (type != Type.BOOLEAN_TYPE) {
                throw new UnsupportedOperationException("don't know how to put a compare as a non-boolean type");
            }
            putAsBoolean(v);
        }

        @Override
        public void condJump(Label label, boolean jumpIfFalse, InstructionAdapter v) {
            myOperand.condJump(label, !jumpIfFalse, v);
        }
    }

    private static class ArrayElement extends StackValue {
        private Type boxed;

        public ArrayElement(Type type, boolean unbox) {
            super(type);
            this.boxed = unbox ? JetTypeMapper.boxType(type) : type;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.aload(boxed);    // assumes array and index are on the stack
            onStack(boxed).coerce(type, v);
        }

        @Override
        public void store(InstructionAdapter v) {
            onStack(type).coerce(boxed, v);
            v.astore(boxed);
        }

        @Override
        public void dupReceiver(InstructionAdapter v) {
            v.dup2();   // array and index
        }

        public int receiverSize() {
            return 2;
        }
    }

    private static class CollectionElement extends StackValue {
        private final Callable getter;
        private final Callable setter;
        private final ExpressionCodegen codegen;
        private final GenerationState state;
        private final FrameMap frame;
        private final ResolvedCall<FunctionDescriptor> resolvedGetCall;
        private final ResolvedCall<FunctionDescriptor> resolvedSetCall;
        private final FunctionDescriptor setterDescriptor;
        private final FunctionDescriptor getterDescriptor;

        public CollectionElement(Type type, ResolvedCall<FunctionDescriptor> resolvedGetCall, ResolvedCall<FunctionDescriptor> resolvedSetCall, ExpressionCodegen codegen, GenerationState state) {
            super(type);
            this.resolvedGetCall = resolvedGetCall;
            this.resolvedSetCall = resolvedSetCall;
            this.state = state;
            this.setterDescriptor = resolvedSetCall == null ? null : resolvedSetCall.getResultingDescriptor();
            this.getterDescriptor = resolvedGetCall == null ? null : resolvedGetCall.getResultingDescriptor();
            this.setter = resolvedSetCall == null ? null : codegen.resolveToCallable(setterDescriptor, false);
            this.getter = resolvedGetCall == null ? null : codegen.resolveToCallable(getterDescriptor, false);
            this.codegen = codegen;
            this.frame = codegen.myFrameMap;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if (getter == null) {
                throw new UnsupportedOperationException("no getter specified");
            }
            if(getter instanceof CallableMethod)
                ((CallableMethod)getter).invoke(v);
            else
                ((IntrinsicMethod)getter).generate(codegen, v, null, null, null, null, state);
            coerce(type, v);
        }

        @Override
        public void store(InstructionAdapter v) {
            if (setter == null) {
                throw new UnsupportedOperationException("no setter specified");
            }
            if(setter instanceof CallableMethod) {
                CallableMethod method = (CallableMethod) setter;
                method.invoke(v);
                Type returnType = method.getSignature().getAsmMethod().getReturnType();
                if(returnType != Type.VOID_TYPE) {
                    if(returnType.getSize() == 2)
                        v.pop2();
                    else
                        v.pop();
                }
            }
            else
                ((IntrinsicMethod) setter).generate(codegen, v, null, null, null, null, state);
        }

        public int receiverSize() {
            if(isStandardStack(resolvedGetCall, 1) && isStandardStack(resolvedSetCall, 2)) {
                return 2;
            }
            else {
                return -1;
            }
        }

        @Override
        public void dupReceiver(InstructionAdapter v) {
            if(isStandardStack(resolvedGetCall, 1) && isStandardStack(resolvedSetCall, 2)) {
                v.dup2();   // collection and index
            }
            else {
                int size = 0;
                int lastIndex = frame.enterTemp();
                frame.leaveTemp();
                
                // indexes
                List<ValueParameterDescriptor> valueParameters = resolvedGetCall.getResultingDescriptor().getValueParameters();
                int firstParamIndex = -1;
                for(int i = valueParameters.size()-1; i >= 0; --i) {
                    Type type = codegen.typeMapper.mapType(valueParameters.get(i).getType(), MapTypeMode.VALUE);
                    int sz = type.getSize();
                    frame.enterTemp(sz);
                    lastIndex += sz;
                    size += sz;
                    v.store((firstParamIndex = lastIndex)-sz, type);
                }

                List<TypeParameterDescriptor> typeParameters = resolvedGetCall.getResultingDescriptor().getTypeParameters();
                int firstTypeParamIndex = -1;
                for(int i = typeParameters.size()-1; i >= 0; --i)  {
                    if(typeParameters.get(i).isReified()) {
                        frame.enterTemp();
                        lastIndex++;
                        size++;
                        v.store(firstTypeParamIndex = lastIndex-1, JetTypeMapper.TYPE_OBJECT);
                    }
                }

                ReceiverDescriptor receiverParameter = resolvedGetCall.getReceiverArgument();
                int receiverIndex = -1;
                if(receiverParameter.exists()) {
                    Type type = codegen.typeMapper.mapType(receiverParameter.getType(), MapTypeMode.VALUE);
                    int sz = type.getSize();
                    frame.enterTemp(sz);
                    lastIndex += sz;
                    size += sz;
                    v.store((receiverIndex = lastIndex)-sz, type);
                }

                ReceiverDescriptor thisObject = resolvedGetCall.getThisObject();
                int thisIndex = -1;
                if(thisObject.exists()) {
                    frame.enterTemp();
                    lastIndex++;
                    size++;
                    v.store((thisIndex = lastIndex)-1, JetTypeMapper.TYPE_OBJECT);
                }
                
                // for setter

                int  realReceiverIndex;
                Type realReceiverType;
                if(thisIndex != -1) {
                    if(receiverIndex != -1) {
                        realReceiverIndex = receiverIndex;
                        realReceiverType =  codegen.typeMapper.mapType(receiverParameter.getType(), MapTypeMode.VALUE);
                    }
                    else {
                        realReceiverIndex = thisIndex;
                        realReceiverType = JetTypeMapper.TYPE_OBJECT;
                    }
                }
                else {
                    if(receiverIndex != -1) {
                        realReceiverType =  codegen.typeMapper.mapType(receiverParameter.getType(), MapTypeMode.VALUE);
                        realReceiverIndex = receiverIndex;
                    }
                    else {
                        throw new UnsupportedOperationException();
                    }
                }

                if(resolvedSetCall.getThisObject().exists()) {
                    if(resolvedSetCall.getReceiverArgument().exists()) {
                        codegen.generateFromResolvedCall(resolvedSetCall.getThisObject(), JetTypeMapper.TYPE_OBJECT);
                    }
                    v.load(realReceiverIndex - realReceiverType.getSize(), realReceiverType);
                }
                else {
                    if(resolvedSetCall.getReceiverArgument().exists()) {
                        v.load(realReceiverIndex - realReceiverType.getSize(), realReceiverType);
                    }
                    else {
                        throw new UnsupportedOperationException();
                    }
                }

                int index = firstParamIndex;
                for(int i = 0; i != valueParameters.size(); ++i) {
                    Type type = codegen.typeMapper.mapType(valueParameters.get(i).getType(), MapTypeMode.VALUE);
                    int sz = type.getSize();
                    v.load(index-sz, type);
                    index -= sz;
                }

                // restoring original
                if(thisIndex != -1) {
                    v.load(thisIndex-1, JetTypeMapper.TYPE_OBJECT);
                }
                
                if(receiverIndex != -1) {
                    Type type = codegen.typeMapper.mapType(receiverParameter.getType(), MapTypeMode.VALUE);
                    v.load(receiverIndex-type.getSize(), type);
                }

                if(firstTypeParamIndex != -1) {
                    index = firstTypeParamIndex;
                    for(int i = 0; i != typeParameters.size(); ++i)  {
                        if(typeParameters.get(i).isReified()) {
                            v.load(index-1, JetTypeMapper.TYPE_OBJECT);
                            index--;
                        }
                    }
                }
                
                index = firstParamIndex;
                for(int i = 0; i != valueParameters.size(); ++i) {
                    Type type = codegen.typeMapper.mapType(valueParameters.get(i).getType(), MapTypeMode.VALUE);
                    int sz = type.getSize();
                    v.load(index-sz, type);
                    index -= sz;
                }

                frame.leaveTemp(size);
            }
        }

        private boolean isStandardStack(ResolvedCall call, int valueParamsSize) {
            if(call == null)
                return true;

            for (TypeParameterDescriptor typeParameterDescriptor : call.getResultingDescriptor().getTypeParameters()) {
                if(typeParameterDescriptor.isReified())
                    return false;
            }

            List<ValueParameterDescriptor> valueParameters = call.getResultingDescriptor().getValueParameters();
            if(valueParameters.size() != valueParamsSize)
                return false;

            for (ValueParameterDescriptor valueParameter : valueParameters) {
                if (codegen.typeMapper.mapType(valueParameter.getType(), MapTypeMode.VALUE).getSize() != 1)
                    return false;
            }

            if(call.getThisObject().exists()) {
                if(call.getReceiverArgument().exists())
                    return false;
            }
            else {
                if(codegen.typeMapper.mapType(call.getResultingDescriptor().getReceiverParameter().getType(), MapTypeMode.VALUE).getSize() != 1)
                    return false;
            }

            return true;
        }
    }


    static class Field extends StackValue {
        final String owner;
        final String name;
        private final boolean isStatic;

        public Field(Type type, String owner, String name, boolean isStatic) {
            super(type);
            this.owner = owner;
            this.name = name;
            this.isStatic = isStatic;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner, name, this.type.getDescriptor());
            coerce(type, v);
        }

        @Override
        public void dupReceiver(InstructionAdapter v) {
            if (!isStatic) {
                v.dup();
            }
        }

        public int receiverSize() {
            return isStatic ? 0 : 1;
        }

        @Override
        public void store(InstructionAdapter v) {
            v.visitFieldInsn(isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, owner, name, this.type.getDescriptor());
        }
    }

    static class Property extends StackValue {
        @NotNull
        private final String name;
        @Nullable
        private final Method getter;
        @Nullable
        private final Method setter;
        @NotNull
        public final String methodOwner;
        @NotNull
        private final String methodOwnerParam;
        private final boolean isStatic;
        private final boolean isInterface;
        private final boolean isSuper;
        private final int invokeOpcode;

        public Property(@NotNull String name, @NotNull String methodOwner, @NotNull String methodOwnerParam, Method getter, Method setter, boolean aStatic, boolean isInterface, boolean isSuper, Type type, int invokeOpcode) {
            super(type);
            this.name = name;
            this.methodOwner = methodOwner;
            this.methodOwnerParam = methodOwnerParam;
            this.getter = getter;
            this.setter = setter;
            isStatic = aStatic;
            this.isInterface = isInterface;
            this.isSuper = isSuper;
            this.invokeOpcode = invokeOpcode;
            if (invokeOpcode == 0) {
                if (setter != null || getter != null) {
                    throw new IllegalArgumentException();
                }
            }
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if(isSuper && isInterface) {
                v.visitMethodInsn(Opcodes.INVOKESTATIC, methodOwner, getter.getName(), getter.getDescriptor().replace("(", "(L" + methodOwnerParam + ";"));
            }
            else {
            if (getter == null) {
                v.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, methodOwner, name, this.type.getDescriptor());
            }
            else {
                v.visitMethodInsn(invokeOpcode, methodOwner, getter.getName(), getter.getDescriptor());
            }
            }
            coerce(type, v);
        }

        @Override
        public void store(InstructionAdapter v) {
            if(isSuper && isInterface) {
                v.visitMethodInsn(Opcodes.INVOKESTATIC, methodOwner, setter.getName(), setter.getDescriptor().replace("(", "(L" + methodOwnerParam + ";"));
            }
            else {
            if (setter == null) {
                v.visitFieldInsn(isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, methodOwner, name, this.type.getDescriptor());
            }
            else {
                v.visitMethodInsn(invokeOpcode, methodOwner, setter.getName(), setter.getDescriptor());
                }
            }
        }

        @Override
        public void dupReceiver(InstructionAdapter v) {
            if (!isStatic) {
                v.dup();
            }
        }

        public int receiverSize() {
            return isStatic ? 0 : 1;
        }
    }

    private static class Expression extends StackValue {
        private final JetExpression expression;
        private final ExpressionCodegen generator;

        public Expression(Type type, JetExpression expression, ExpressionCodegen generator) {
            super(type);
            this.expression = expression;
            this.generator = generator;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            generator.gen(expression, type);
        }
    }

    public static class Shared extends StackValue {
        private final int index;

        public Shared(int index, Type type) {
            super(type);
            this.index = index;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.load(index, JetTypeMapper.TYPE_OBJECT);
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(Opcodes.GETFIELD, sharedType.getInternalName(), "ref", refType.getDescriptor());
            StackValue.onStack(refType).coerce(this.type, v);
            StackValue.onStack(this.type).coerce(type, v);
        }

        @Override
        public void store(InstructionAdapter v) {
            v.load(index, JetTypeMapper.TYPE_OBJECT);
            v.swap();
            Type refType = refType(this.type);
            Type sharedType = sharedTypeForType(this.type);
            v.visitFieldInsn(Opcodes.PUTFIELD, sharedType.getInternalName(), "ref", refType.getDescriptor());
        }
    }

    public static Type sharedTypeForType(Type type) {
        switch(type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                return JetTypeMapper.TYPE_SHARED_VAR;

            case Type.BYTE:
                return JetTypeMapper.TYPE_SHARED_BYTE;

            case Type.SHORT:
                return JetTypeMapper.TYPE_SHARED_SHORT;

            case Type.CHAR:
                return JetTypeMapper.TYPE_SHARED_CHAR;

            case Type.INT:
                return JetTypeMapper.TYPE_SHARED_INT;

            case Type.LONG:
                return JetTypeMapper.TYPE_SHARED_LONG;

            case Type.BOOLEAN:
                return JetTypeMapper.TYPE_SHARED_BOOLEAN;

            case Type.FLOAT:
                return JetTypeMapper.TYPE_SHARED_FLOAT;

            case Type.DOUBLE:
                return JetTypeMapper.TYPE_SHARED_DOUBLE;

            default:
                throw new UnsupportedOperationException();
        }
    }

    public static Type refType(Type type) {
        if(type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)
            return JetTypeMapper.TYPE_OBJECT;

        return type;
    }

    static class FieldForSharedVar extends StackValue {
        final String owner;
        final String name;

        public FieldForSharedVar(Type type, String owner, String name) {
            super(type);
            this.owner = owner;
            this.name = name;
        }

        @Override
        public void dupReceiver(InstructionAdapter v) {
            v.dup();
        }

        public int receiverSize() {
            return 1;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            Type sharedType = sharedTypeForType(this.type);
            Type refType = refType(this.type);
            v.visitFieldInsn(Opcodes.GETFIELD, sharedType.getInternalName(), "ref", refType.getDescriptor());
            StackValue.onStack(refType).coerce(this.type, v);
            StackValue.onStack(this.type).coerce(type, v);
        }

        @Override
        public void store(InstructionAdapter v) {
            v.visitFieldInsn(Opcodes.PUTFIELD, sharedTypeForType(type).getInternalName(), "ref", refType(type).getDescriptor());
        }
    }

    public static class Composed extends StackValue {
        public final StackValue prefix;
        public final StackValue suffix;

        public Composed(StackValue prefix, StackValue suffix) {
            super(suffix.type);
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            prefix.put(prefix.type, v);
            suffix.put(type, v);
        }

        @Override
        public void store(InstructionAdapter v) {
            prefix.put(JetTypeMapper.TYPE_OBJECT, v);
            suffix.store(v);
        }
    }

    private static class ThisOuter extends StackValue {
        private ExpressionCodegen codegen;
        private ClassDescriptor descriptor;

        public ThisOuter(ExpressionCodegen codegen, ClassDescriptor descriptor) {
            super(JetTypeMapper.TYPE_OBJECT);
            this.codegen = codegen;
            this.descriptor = descriptor;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            codegen.generateThisOrOuter(descriptor);
        }
    }

    private static class PostIncrement extends StackValue {
        private int index;
        private int increment;

        public PostIncrement(int index, int increment) {
            super(Type.INT_TYPE);
            this.index = index;
            this.increment = increment;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            if(!type.equals(Type.VOID_TYPE)) {
                v.load(index, Type.INT_TYPE);
                coerce(type, v);
            }
            v.iinc(index, increment);
        }
    }

    private static class PreIncrement extends StackValue {
        private int index;
        private int increment;

        public PreIncrement(int index, int increment) {
            super(Type.INT_TYPE);
            this.index = index;
            this.increment = increment;
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            v.iinc(index, increment);
            if(!type.equals(Type.VOID_TYPE)) {
                v.load(index, Type.INT_TYPE);
                coerce(type, v);
            }
        }
    }

    public static class CallReceiver extends StackValue {
        private ResolvedCall<? extends CallableDescriptor> resolvedCall;
        StackValue receiver;
        private ExpressionCodegen codegen;
        @NotNull
        private final GenerationState state;
        private CallableMethod callableMethod;

        public CallReceiver(ResolvedCall<? extends CallableDescriptor> resolvedCall, StackValue receiver, ExpressionCodegen codegen, @NotNull GenerationState state, CallableMethod callableMethod) {
            super(calcType(resolvedCall, codegen, callableMethod));
            this.resolvedCall = resolvedCall;
            this.receiver = receiver;
            this.codegen = codegen;
            this.state = state;
            this.callableMethod = callableMethod;
        }

        private static Type calcType(ResolvedCall<? extends CallableDescriptor> resolvedCall, ExpressionCodegen codegen, CallableMethod callableMethod) {
            ReceiverDescriptor thisObject = resolvedCall.getThisObject();
            ReceiverDescriptor receiverArgument = resolvedCall.getReceiverArgument();

            CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

            if (thisObject.exists()) {
                if(callableMethod != null) {
                    if(receiverArgument.exists()) {
                        return codegen.typeMapper.mapType(callableMethod.getReceiverClass(), MapTypeMode.VALUE);
                    }
                    else {
                        return codegen.typeMapper.mapType(callableMethod.getThisType(), MapTypeMode.VALUE);
                    }
                }
                else {
                    if(receiverArgument.exists()) {
                        return codegen.typeMapper.mapType(descriptor.getReceiverParameter().getType(), MapTypeMode.VALUE);
                    }
                    else {
                        return codegen.typeMapper.mapType(descriptor.getExpectedThisObject().getType(), MapTypeMode.VALUE);
                    }
                }
            }
            else {
                if (receiverArgument.exists()) {
                    if(callableMethod != null)
                        return codegen.typeMapper.mapType(callableMethod.getReceiverClass(), MapTypeMode.VALUE);
                    else
                        return codegen.typeMapper.mapType(descriptor.getReceiverParameter().getType(), MapTypeMode.VALUE);
                }
                else {
                    return Type.VOID_TYPE;
                }
            }
        }

        @Override
        public void put(Type type, InstructionAdapter v) {
            CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

            ReceiverDescriptor thisObject = resolvedCall.getThisObject();
            ReceiverDescriptor receiverArgument = resolvedCall.getReceiverArgument();
            if (thisObject.exists()) {
                if(receiverArgument.exists()) {
                    if (callableMethod != null) {
                        codegen.generateFromResolvedCall(thisObject, Type.getObjectType(callableMethod.getOwner()));
                    }
                    else {
                        codegen.generateFromResolvedCall(thisObject, codegen.typeMapper.mapType(descriptor.getExpectedThisObject().getType(), MapTypeMode.VALUE));
                    }
                    genReceiver(v, receiverArgument, type, descriptor.getReceiverParameter());
                }
                else {
                    genReceiver(v, thisObject, type, null);
                }
            }
            else {
                if (receiverArgument.exists()) {
                    genReceiver(v, receiverArgument, type, descriptor.getReceiverParameter());
                }
            }
        }

        private void genReceiver(InstructionAdapter v, ReceiverDescriptor receiverArgument, Type type, ReceiverDescriptor receiverParameter) {
            if(receiver == StackValue.none()) {
                if(receiverParameter != null) {
                    Type receiverType = codegen.typeMapper.mapType(receiverParameter.getType(), MapTypeMode.VALUE);
                    codegen.generateFromResolvedCall(receiverArgument, receiverType);
                    StackValue.onStack(receiverType).put(type, v);
                }
                else {
                    codegen.generateFromResolvedCall(receiverArgument, type);
                }
            }
            else {
                receiver.put(type, v);
            }
        }
    }
}
